package com.vaadin.bakery.ordering;

import java.time.Instant;

/** One line of an order's history, with the author already resolved. */
public record OrderHistoryLine(Instant timestamp, String messageKey, OrderState newState, String authorName) {
}
