package com.example;

import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;
import org.springframework.stereotype.Component;

/**
 * Two shared signals in an application singleton, which is where an application
 * would put state that every session sees.
 */
@Component
public class Tickets {

    private final SharedValueSignal<Integer> waiting = new SharedValueSignal<>(0);
    private final SharedListSignal<String> queue = new SharedListSignal<>(String.class);

    public SharedValueSignal<Integer> waiting() {
        return waiting;
    }

    public SharedListSignal<String> queue() {
        return queue;
    }
}
