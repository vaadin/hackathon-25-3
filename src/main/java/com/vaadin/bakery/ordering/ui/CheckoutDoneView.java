package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.ordering.CheckoutState;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.net.URI;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

/**
 * What the visitor sees the moment the order exists.
 *
 * The reference used to reach them only through the tracking link, which is a
 * link in a browser tab and nothing else: close the tab and the order is gone.
 * This page says the reference out loud, and hands over the tracking link as
 * something to copy rather than something to remember.
 *
 * It is deliberately not a checkout step. There is no breadcrumb and no way
 * back into a checkout that has already been cleared, and what it needs is the
 * one thing the session still holds after that clearing.
 */
@Route("checkout/done")
@PageTitle("Order received")
@AnonymousAllowed
public class CheckoutDoneView extends VerticalLayout implements BeforeEnterObserver {

    /**
     * Long enough that the toast is read, short enough that somebody still
     * looking at the page sees the bakery pick the order up.
     */
    private static final Duration SETTLE = Duration.ofSeconds(5);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final OrderService orders;
    private final CheckoutState state;
    private final Div stateBlock = new Div();

    public CheckoutDoneView(OrderService orders, CheckoutState state) {
        this.orders = orders;
        this.state = state;
        addClassNames("checkout-view", "checkout-done");
        stateBlock.addClassName("checkout-done__state");

        // Two things are true for a few seconds and then one of them stops
        // being true: the toast says the order has just arrived, and the state
        // block says what the bakery has done with it. One deferred callback
        // retires the first and re-reads the second, with no push connection
        // and no polling.
        whenAttached(ui -> {
            var placed = state.placed().peek();
            if (placed == null) {
                return () -> {
                };
            }
            var received = Notification.show(getTranslation("checkout.done.toast"), 0,
                    Notification.Position.BOTTOM_START);
            received.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            ui.triggerAfter(SETTLE, () -> {
                received.close();
                orders.byReferenceAndToken(placed.reference(), placed.token())
                        .ifPresent(this::renderState);
            });
            return received::close;
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        var placed = state.placed().peek();
        var order = placed == null ? null
                : orders.byReferenceAndToken(placed.reference(), placed.token()).orElse(null);
        if (order == null) {
            // Nothing was placed in this session, so there is nothing to
            // confirm. The basket is where a visitor who typed this URL meant
            // to be.
            event.forwardTo(CartView.class);
            return;
        }
        render(order, placed.token());
    }

    private void render(Order order, String token) {
        add(Translations.bindText(new H2(), "checkout.done.title"),
                Translations.bindText(new Paragraph(), "checkout.done.reference", order.getReference()));

        add(stateBlock);
        renderState(order);

        add(Translations.bindText(new Paragraph(), locale -> getTranslation(locale, "tracking.slot",
                order.getPickupLocation().getName(),
                order.getPickupDate().format(DATE.withLocale(locale)),
                order.getPickupTime().format(TIME))));

        var total = Translations.bindText(new Span(),
                locale -> getTranslation(locale, "cart.gross", order.gross().format(locale)));
        total.addClassName("checkout-view__total");
        add(total);

        add(link(order.getReference(), token));

        var shop = Translations.bindText(new Anchor("shop", ""), "catalogue.backToShop");
        add(shop);
    }

    /**
     * The block the deferred callback refreshes. It is rebuilt rather than
     * bound, because the whole point is a value read again from the database.
     */
    private void renderState(Order order) {
        stateBlock.removeAll();
        var badge = Translations.bindText(new Span(), order.getState().translationKey());
        badge.getElement().getThemeList().add("badge " + themeFor(order.getState()));
        stateBlock.add(badge);
    }

    private String themeFor(OrderState state) {
        return switch (state) {
            case READY, PICKED_UP -> "success";
            case PROBLEM, CANCELLED -> "error";
            default -> "contrast";
        };
    }

    private Div link(String reference, String token) {
        var address = new TextField();
        Translations.bind(address, address::setLabel, "checkout.done.link");
        address.setValue(trackingLink(reference, token));
        address.setReadOnly(true);
        address.setWidthFull();

        var copy = Translations.bindText(new Button(""), "checkout.done.copy");
        copy.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        // The 25.3 clipboard API, which reports back whether the browser let
        // the write happen. A copy button that lies is worse than no button.
        Clipboard.onClick(copy).writeText(address,
                copied -> Notification.show(getTranslation("checkout.done.copied")),
                error -> Notification.show(getTranslation("checkout.done.copyFailed")));

        var open = Translations.bindText(new Anchor(address.getValue(), ""), "checkout.done.open");

        var block = new Div(Translations.bindText(new Paragraph(), "checkout.done.keepIt"),
                new Div(address, copy), open);
        block.addClassName("checkout-done__link");
        return block;
    }

    /**
     * Absolute, because this one exists to be pasted somewhere that is not this
     * browser. Flow has no server side way to build a route's absolute URL:
     * {@code Page.fetchCurrentURL} is a round trip to the browser, so this
     * reads the request the call arrived on and falls back to the relative
     * path when there is not one.
     */
    private String trackingLink(String reference, String token) {
        var path = "track/" + reference + "?t=" + token;
        if (VaadinRequest.getCurrent() instanceof VaadinServletRequest servlet) {
            var url = URI.create(servlet.getRequestURL().toString());
            var context = servlet.getContextPath();
            return url.getScheme() + "://" + url.getAuthority()
                    + (context == null || context.isBlank() ? "" : context) + "/" + path;
        }
        return path;
    }
}
