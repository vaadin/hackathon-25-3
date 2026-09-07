package com.vaadin.bakery.ordering;

import com.vaadin.bakery.people.Customer;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

/**
 * What the visitor has entered so far. It lives in the session rather than in
 * the views, so going back a step, refreshing the browser or switching language
 * never loses a field.
 */
@Component
@VaadinSessionScope
public class CheckoutState {

    /**
     * The order the visitor has just placed. Everything else here is cleared
     * the moment an order exists, and this is what the confirmation page has
     * left to go on: the reference to show, and the token that proves this
     * session may look at it.
     */
    public record Placed(String reference, String token) implements Serializable {
    }

    private final Customer draft = new Customer();
    private final ValueSignal<Boolean> contactValid = new ValueSignal<>(false);
    private final ValueSignal<PickupLocation> location = new ValueSignal<>(null);
    private final ValueSignal<LocalDate> date = new ValueSignal<>(null);
    private final ValueSignal<LocalTime> time = new ValueSignal<>(null);
    private final ValueSignal<String> note = new ValueSignal<>("");
    private final ValueSignal<Placed> placed = new ValueSignal<>(null);
    private final Signal<Boolean> slotValid;

    public CheckoutState() {
        this.slotValid = Signal.computed(() ->
                location.get() != null && date.get() != null && time.get() != null);
    }

    public Customer draft() {
        return draft;
    }

    public ValueSignal<Boolean> contactValid() {
        return contactValid;
    }

    public Signal<Boolean> slotValid() {
        return slotValid;
    }

    public ValueSignal<PickupLocation> location() {
        return location;
    }

    public ValueSignal<LocalDate> date() {
        return date;
    }

    public ValueSignal<LocalTime> time() {
        return time;
    }

    public ValueSignal<String> note() {
        return note;
    }

    public ValueSignal<Placed> placed() {
        return placed;
    }

    public boolean isContactValid() {
        return Boolean.TRUE.equals(contactValid.peek());
    }

    public boolean isSlotValid() {
        return Signal.untracked(() -> slotValid.get());
    }

    public void clear() {
        draft.setFirstName(null);
        draft.setLastName(null);
        draft.setEmail(null);
        draft.setPhone(null);
        contactValid.set(false);
        location.set(null);
        date.set(null);
        time.set(null);
        note.set("");
        placed.set(null);
    }
}
