package com.vaadin.bakery.ordering;

import com.vaadin.bakery.people.Role;

import java.util.List;
import java.util.Set;

/**
 * The order lifecycle. Transitions are declared here rather than scattered
 * across the views, so a new state cannot quietly become reachable.
 */
public enum OrderState {
    NEW, CONFIRMED, IN_PREPARATION, READY, PICKED_UP, PROBLEM, CANCELLED;

    public boolean canMoveTo(OrderState target) {
        return allowedTargets().contains(target);
    }

    public Set<OrderState> allowedTargets() {
        return switch (this) {
            case NEW -> Set.of(CONFIRMED, PROBLEM, CANCELLED);
            case CONFIRMED -> Set.of(IN_PREPARATION, PROBLEM, CANCELLED);
            case IN_PREPARATION -> Set.of(READY, PROBLEM, CANCELLED);
            case READY -> Set.of(PICKED_UP, PROBLEM, CANCELLED);
            case PICKED_UP -> Set.of();
            case PROBLEM -> Set.of(CONFIRMED, CANCELLED);
            // A cancellation is reversible. Most cancellations are somebody
            // pressing the wrong thing, and a state a screen can enter and
            // never leave is a trap: the reopening is recorded in the history
            // like every other change. Handing over is not reversible, and does
            // not need to be, because by then the order has an invoice.
            case CANCELLED -> Set.of(CONFIRMED);
        };
    }

    /**
     * Who may set this state. A baker bakes, so a baker sets the states an order
     * passes through while it is being baked, and keeps PROBLEM because a
     * problem with an order is usually something only the person at the oven can
     * see. Handing the order over is not baking: it is where the invoice is
     * issued. Cancelling is a commercial decision about a customer.
     */
    public boolean settableBy(Role role) {
        return role != Role.BAKER || isActiveInKitchen() || this == PROBLEM;
    }

    public boolean isOpen() {
        return this != PICKED_UP && this != CANCELLED;
    }

    public boolean isActiveInKitchen() {
        return this == CONFIRMED || this == IN_PREPARATION || this == READY;
    }

    public static List<OrderState> kitchenColumns() {
        return List.of(CONFIRMED, IN_PREPARATION, READY);
    }

    public String translationKey() {
        return "ordering.state." + name();
    }
}
