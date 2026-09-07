package com.vaadin.bakery.ordering;

import java.time.LocalTime;

public record SlotOption(LocalTime time, int capacity, int booked) {

    public int remaining() {
        return Math.max(0, capacity - booked);
    }

    public boolean isAvailable() {
        return remaining() > 0;
    }
}
