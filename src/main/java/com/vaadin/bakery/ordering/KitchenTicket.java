package com.vaadin.bakery.ordering;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * A ticket on the kitchen board. This travels through a shared signal, so it is
 * a plain serialisable record of what a baker needs to read from across the
 * room: no entities, no lazy collections, nothing that needs a session.
 */
public record KitchenTicket(Long orderId, String reference, LocalDate pickupDate, LocalTime pickupTime,
        String customerFirstName, OrderState state, List<String> lines, List<String> allergenKeys,
        String assignedBakerName, Long assignedBakerId) implements Serializable {

    public boolean isClaimed() {
        return assignedBakerId != null;
    }

    public KitchenTicket withState(OrderState newState) {
        return new KitchenTicket(orderId, reference, pickupDate, pickupTime, customerFirstName, newState, lines,
                allergenKeys, assignedBakerName, assignedBakerId);
    }

    public KitchenTicket withBaker(Long bakerId, String bakerName) {
        return new KitchenTicket(orderId, reference, pickupDate, pickupTime, customerFirstName, state, lines,
                allergenKeys, bakerName, bakerId);
    }
}
