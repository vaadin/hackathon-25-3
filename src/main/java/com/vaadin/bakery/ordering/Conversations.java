package com.vaadin.bakery.ordering;

import com.vaadin.flow.signals.shared.SharedValueSignal;
import tools.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * One shared signal per conversation, so a reply reaches the other side without
 * anybody reloading a page.
 *
 * The signal carries a count and nothing else. The messages themselves stay in
 * the database, because a conversation has attachments, ordering and read
 * marks, and putting all of that through a shared signal would be a second copy
 * of the truth for no gain. What the signal has to say is only "there is
 * something new", and every panel watching that reference goes and reads it.
 *
 * The map grows by one entry per order anybody has open. That is bounded by the
 * orders in play rather than by the orders that exist, and an entry is a long,
 * so nothing here needs eviction at this size. A bakery with a hundred thousand
 * live conversations would key this differently.
 */
@Component
public class Conversations {

    private final Map<String, SharedValueSignal<Long>> perOrder = new ConcurrentHashMap<>();

    /** The signal for one order, created the first time somebody looks. */
    public SharedValueSignal<Long> forOrder(String reference) {
        return perOrder.computeIfAbsent(reference, key -> new SharedValueSignal<Long>(0L, new TypeReference<Long>() {
            }));
    }

    /** Says a message landed. Every panel on that order is watching this. */
    public void posted(String reference) {
        var signal = forOrder(reference);
        signal.set(signal.peek() + 1);
    }
}
