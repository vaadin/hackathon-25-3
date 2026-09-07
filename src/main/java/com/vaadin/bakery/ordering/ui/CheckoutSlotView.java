package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.ordering.CartSignals;
import com.vaadin.bakery.ordering.CheckoutState;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.bakery.ordering.SlotService;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;

/** Step two: when and where. */
@Route("checkout/slot")
@RouteParent(CheckoutContactView.class)
@PageTitle("Pickup")
@AnonymousAllowed
public class CheckoutSlotView extends VerticalLayout {

    public CheckoutSlotView(CheckoutState state, SlotService slots, CartSignals cart,
            PickupLocationRepository locations) {
        addClassName("checkout-view");
        add(CheckoutSteps.breadcrumbs(), Translations.bindText(new H2(), "checkout.slot.title"));

        var picker = new SlotPicker(slots, cart::maxLeadTimeDays, locations.findByActiveTrueOrderByNameAsc());
        picker.locationSelect().addValueChangeListener(event -> state.location().set(event.getValue()));
        picker.datePicker().addValueChangeListener(event -> state.date().set(event.getValue()));
        picker.timeSelect().addValueChangeListener(event -> state.time().set(event.getValue()));

        state.location().set(picker.getLocation());
        state.date().set(picker.getDate());
        state.time().set(picker.getTime());

        var back = Translations.bindText(new Button("",
                event -> getUI().ifPresent(ui -> ui.navigate("checkout/contact"))), "checkout.back");

        var next = Translations.bindText(new Button("",
                event -> getUI().ifPresent(ui -> ui.navigate("checkout/review"))), "checkout.next");
        next.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        next.bindEnabled(state.slotValid());

        add(picker, new com.vaadin.flow.component.html.Div(back, next));
    }
}
