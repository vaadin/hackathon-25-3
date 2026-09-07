package com.vaadin.bakery.ordering;

import java.time.LocalDate;

/**
 * What the date picker needs to know about one day: whether it can be picked at
 * all, why not when it cannot, and how much room is left when it can.
 */
public record DaySlotLoad(LocalDate date, int capacity, int booked, boolean closed, String closedReason) {

    public int remaining() {
        return Math.max(0, capacity - booked);
    }

    public boolean isFull() {
        return !closed && remaining() == 0;
    }

    public boolean isSelectable() {
        return !closed && !isFull();
    }

    /** Drives the custom part name, so a nearly full day looks different. */
    public String partName() {
        if (closed) {
            return "closed";
        }
        if (isFull()) {
            return "full";
        }
        double ratio = capacity == 0 ? 1 : (double) booked / capacity;
        return ratio >= 0.75 ? "busy" : "free";
    }
}
