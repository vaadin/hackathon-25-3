package com.vaadin.bakery.ordering;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Counts the work the order board asks the database for, and how often the
 * expensive column's value provider runs.
 *
 * This exists to prove a claim rather than to decorate a view: in 25.3 a hidden
 * Grid column runs no value provider and fetches no data, and the test that
 * says so needs a number it can compare.
 */
@Component
public class OrderQueryCounter {

    private final AtomicLong queries = new AtomicLong();
    private final AtomicLong expensiveColumnCalls = new AtomicLong();

    public void countQuery() {
        queries.incrementAndGet();
    }

    public void countExpensiveColumn() {
        expensiveColumnCalls.incrementAndGet();
    }

    public long queries() {
        return queries.get();
    }

    public long expensiveColumnCalls() {
        return expensiveColumnCalls.get();
    }

    public void reset() {
        queries.set(0);
        expensiveColumnCalls.set(0);
    }
}
