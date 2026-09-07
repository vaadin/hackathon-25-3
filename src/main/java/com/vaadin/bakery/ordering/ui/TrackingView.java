package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.SafeHtml;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.ordering.CartSignals;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderActivity;
import com.vaadin.flow.signals.Signal;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * The only anonymous view of private data, so the rules are strict: the
 * reference alone is not enough, an opaque token has to match, and a wrong
 * token looks exactly like a wrong reference. No oracle.
 */
@Route("track/:reference")
@PageTitle("Your order")
@AnonymousAllowed
public class TrackingView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("d MMM HH:mm");

    private final OrderService orders;
    private final CartSignals cart;
    private final OrderActivity activity;

    public TrackingView(OrderService orders, CartSignals cart, OrderActivity activity) {
        this.orders = orders;
        this.cart = cart;
        this.activity = activity;
        addClassName("tracking-view");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        var reference = event.getRouteParameters().get("reference").orElse("");
        var token = event.getLocation().getQueryParameters().getParameters()
                .getOrDefault("t", java.util.List.of()).stream().findFirst().orElse(null);

        var order = orders.byReferenceAndToken(reference, token).orElse(null);
        if (order == null) {
            add(Translations.bindText(new H2(), "tracking.notFound.title"),
                    Translations.bindText(new Paragraph(), "tracking.notFound.body"),
                    Translations.bindText(new Anchor("shop", ""), "catalogue.backToShop"));
            return;
        }

        // The page follows the order rather than showing the state it had when
        // it loaded. A customer sits on this screen waiting to hear that their
        // cake is ready, and the one thing it must not do is need a refresh to
        // say so. The effect reads the order's shared signal, so anything the
        // counter or the kitchen does redraws it here.
        Signal.effect(this, () -> {
            activity.forOrder(reference).get();
            removeAll();
            orders.byReferenceAndToken(reference, token).ifPresent(fresh -> render(fresh, token));
        });
    }

    private void render(Order order, String token) {
        add(Translations.bindText(new H2(), "tracking.title", order.getReference()));

        var state = Translations.bindText(new Span(), order.getState().translationKey());
        state.getElement().getThemeList().add("badge " + themeFor(order.getState()));
        add(state);

        add(Translations.bindText(new Paragraph(), locale -> getTranslation(locale, "tracking.slot",
                order.getPickupLocation().getName(),
                order.getPickupDate().format(DATE.withLocale(locale)),
                order.getPickupTime().format(TIME))));

        var items = new Div();
        items.addClassName("tracking-view__items");
        order.getItems().forEach(item -> {
            var row = new Div(new Span(item.getQuantity() + " x " + item.getProduct().getName()),
                    Translations.bindText(new Span(), locale -> item.gross().format(locale)));
            if (item.getComment() != null && !item.getComment().isBlank()) {
                row.add(new Span(SafeHtml.text(item.getComment())));
            }
            row.addClassName("tracking-view__item");
            items.add(row);
        });
        add(items, Translations.bindText(new Span(),
                locale -> getTranslation(locale, "cart.gross", order.gross().format(locale))));

        var timeline = new Div();
        timeline.addClassName("tracking-view__timeline");
        order.getHistory().forEach(entry -> {
            var when = Translations.bindText(new Span(), locale ->
                    entry.getTimestamp().atZone(ZoneId.systemDefault()).format(STAMP.withLocale(locale)));
            timeline.add(new Div(when, Translations.bindText(new Span(), entry.getMessage())));
        });
        add(Translations.bindText(new H2(), "tracking.history"), timeline);

        var actions = new Div();
        actions.addClassName("tracking-view__actions");

        // Withdrawing is only fair while nothing has been baked.
        if (order.getState() == OrderState.NEW) {
            var cancel = Translations.bindText(new Button("", event -> confirmCancel(order, token)),
                    "tracking.cancel");
            cancel.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            actions.add(cancel);
        }

        var reorder = Translations.bindText(new Button("", event -> reorder(order)), "tracking.reorder");
        actions.add(reorder);
        add(actions);

        // The customer's side of the conversation, with the bakery.
        add(new ConversationPanel(orders, activity, order.getReference(), false, null,
                order.getCustomer().getFirstName(), !order.getState().isOpen()));
    }

    private String themeFor(OrderState state) {
        return switch (state) {
            case READY, PICKED_UP -> "success";
            case PROBLEM, CANCELLED -> "error";
            default -> "contrast";
        };
    }

    private void confirmCancel(Order order, String token) {
        var dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("tracking.cancel.confirm.title"));
        dialog.setText(getTranslation("tracking.cancel.confirm.body", order.getReference()));
        dialog.setCancelable(true);
        dialog.setConfirmText(getTranslation("tracking.cancel"));
        dialog.addConfirmListener(event -> {
            try {
                orders.cancelAsCustomer(order);
                getUI().ifPresent(ui -> ui.getPage()
                        .setLocation("track/" + order.getReference() + "?t=" + token));
            } catch (DomainException failure) {
                Notification.show(getTranslation(failure.translationKey(), failure.arguments()));
            }
        });
        dialog.open();
    }

    /** Put the same basket back, and say what could not come along. */
    private void reorder(Order order) {
        var skipped = new java.util.ArrayList<String>();
        order.getItems().forEach(item -> {
            if (item.getProduct().isAvailable()) {
                cart.add(item.getProduct().getId(), item.getQuantity(), item.getComment());
            } else {
                skipped.add(item.getProduct().getName());
            }
        });
        if (!skipped.isEmpty()) {
            Notification.show(getTranslation("tracking.reorder.skipped", String.join(", ", skipped)));
        }
        getUI().ifPresent(ui -> ui.navigate("cart"));
    }
}
