package com.vaadin.bakery.ordering;

import com.vaadin.flow.signals.shared.SharedValueSignal;
import tools.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * One shared signal per order, so anything that happens to it reaches every
 * screen showing it without anybody reloading a page.
 *
 * The signal carries a count and nothing else. What happened stays in the
 * database, because an order has attachments, a history, read marks and a
 * state machine, and putting all of that through a shared signal would be a
 * second copy of the truth for no gain. What the signal has to say is only
 * "something about this order changed", and every screen watching that
 * reference goes and reads it.
 *
 * Two kinds of thing bump it and both matter to the same reader: a message
 * posted in the conversation, and a state change. A customer watching their
 * tracking page wants to know when the bakery replies and when the cake is
 * ready, and neither of those should need a refresh.
 *
 * The map grows by one entry per order anybody has open. That is bounded by the
 * orders in play rather than by the orders that exist, and an entry is a long,
 * so nothing here needs eviction at this size. A bakery with a hundred thousand
 * live conversations would key this differently.
 */
@Component
public class OrderActivity {

    private final Map<String, SharedValueSignal<Long>> perOrder = new ConcurrentHashMap<>();

    /** The signal for one order, created the first time somebody looks. */
    public SharedValueSignal<Long> forOrder(String reference) {
        return perOrder.computeIfAbsent(reference, key -> new SharedValueSignal<Long>(0L, new TypeReference<Long>() {
            }));
    }

    /** Says something happened. Every screen on that order is watching this. */
    public void changed(String reference) {
        var signal = forOrder(reference);
        signal.set(signal.peek() + 1);
    }
}
