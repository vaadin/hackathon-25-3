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

        // Whatever was already chosen comes back, because this view is built
        // again every time somebody steps back into it and the picker starts
        // from its own defaults. It used to be the other way round: the fresh
        // picker's values were written into the session, so stepping back and
        // forward emptied the day and the time that had just been chosen, and
        // the review step lost them with it.
        //
        // In this order, and the order is the whole trick: the location reloads
        // the calendar, the day reloads that day's times and prefills the next
        // free one, so the time has to be set last or the prefill wins.
        var chosenLocation = state.location().peek();
        if (chosenLocation != null) {
            picker.locationSelect().setValue(chosenLocation);
        }
        var chosenDate = state.date().peek();
        if (chosenDate != null) {
            picker.datePicker().setValue(chosenDate);
        }
        var chosenTime = state.time().peek();
        if (chosenTime != null && picker.timeSelect().getListDataView().getItems().anyMatch(chosenTime::equals)) {
            picker.timeSelect().setValue(chosenTime);
        }

        // Listened to after the seeding, so restoring a choice is not reported
        // as making one.
        picker.locationSelect().addValueChangeListener(event -> state.location().set(event.getValue()));
        picker.datePicker().addValueChangeListener(event -> state.date().set(event.getValue()));
        picker.timeSelect().addValueChangeListener(event -> state.time().set(event.getValue()));

        // The session ends up holding what the picker really holds, which is
        // not always what it was asked for: a slot that filled up while the
        // visitor was on another step is not offered any more, and a session
        // still claiming it would fail at the very end, which is the one place
        // a checkout must not fail.
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
