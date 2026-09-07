package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.SafeHtml;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.base.validation.OnSubmit;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.bakery.ordering.CartSignals;
import com.vaadin.bakery.ordering.Channel;
import com.vaadin.bakery.ordering.CheckoutState;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import java.time.format.DateTimeFormatter;

/**
 * Step three. Nothing new is entered here, so this is where the submit rules
 * finally apply: contact details complete, a slot chosen, a basket that still
 * makes sense.
 */
@Route("checkout/review")
@RouteParent(CheckoutSlotView.class)
@PageTitle("Review")
@AnonymousAllowed
public class CheckoutReviewView extends VerticalLayout {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public CheckoutReviewView(CheckoutState state, CartSignals cart, OrderService orders,
            ProductRepository products) {
        addClassName("checkout-view");
        add(CheckoutSteps.breadcrumbs(), Translations.bindText(new H2(), "checkout.review.title"));

        var summary = new Div();
        summary.addClassName("checkout-view__summary");
        cart.snapshot().forEach(line -> products.findById(line.productId()).ifPresent(product -> {
            var row = new Div(new Span(line.quantity() + " x " + product.getName()),
                    Translations.bindText(new Span(),
                            locale -> product.price().times(line.quantity()).format(locale)));
            row.addClassName("checkout-view__summary-row");
            summary.add(row);
        }));

        var total = Translations.bindText(new Span(),
                locale -> getTranslation(locale, "cart.gross", cart.grossAmount().format(locale)));
        total.addClassName("checkout-view__total");

        var who = Translations.bindText(new Paragraph(), "checkout.review.who",
                state.draft().getFirstName() == null ? "" : state.draft().getFullName(),
                state.draft().getEmail() == null ? "" : state.draft().getEmail());

        var when = Translations.bindText(new Paragraph(), locale -> state.isSlotValid()
                ? getTranslation(locale, "checkout.review.when", state.location().peek().getName(),
                        state.date().peek().format(DATE.withLocale(locale)),
                        state.time().peek().format(TIME))
                : getTranslation(locale, "checkout.review.noSlot"));

        var problems = new Paragraph();
        problems.addClassName("checkout-view__problems");
        problems.setVisible(false);

        var place = Translations.bindText(new Button("", event ->
                place(state, cart, orders, problems)), "checkout.place");
        place.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        // One computed signal over the two step validities and the basket.
        place.bindEnabled(Signal.computed(() -> state.contactValid().get()
                && state.slotValid().get()
                && cart.itemCount().get() > 0));

        var back = Translations.bindText(new Button("",
                event -> getUI().ifPresent(ui -> ui.navigate("checkout/slot"))), "checkout.back");

        add(summary, total, who, when, problems, new Div(back, place));
    }

    private void place(CheckoutState state, CartSignals cart, OrderService orders, Paragraph problems) {
        try {
            var draft = state.draft();
            // Resolving the customer inside place's transaction rather than
            // before it: a checkout refused for a full slot or a lead time
            // must not leave a customer created or renamed behind it.
            var order = orders.place(cart.snapshot(), draft.getFirstName(), draft.getLastName(), draft.getEmail(),
                    draft.getPhone(), state.location().peek(), state.date().peek(), state.time().peek(),
                    Channel.ONLINE, SafeHtml.text(state.note().peek()), null);
            var placed = new CheckoutState.Placed(order.getReference(), order.getTrackingToken());
            cart.clear();
            // clear() drops everything including the last placement, so the new
            // one is recorded after it and not before.
            state.clear();
            state.placed().set(placed);
            getUI().ifPresent(ui -> ui.navigate(CheckoutDoneView.class));
        } catch (DomainException failure) {
            // A domain refusal is a sentence, not a stack trace.
            problems.setText(getTranslation(failure.translationKey(), failure.arguments()));
            problems.setVisible(true);
            var notification = Notification.show(problems.getText());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
