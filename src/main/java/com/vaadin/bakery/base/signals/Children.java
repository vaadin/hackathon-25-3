package com.vaadin.bakery.base.signals;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Renders a list signal as child components.
 *
 * The 25.3 documentation describes {@code container.bindChildren(listSignal,
 * factory)} for exactly this, but the method does not exist in 25.3.0-beta1: it
 * is absent from flow-server and from every layout in the release. Until it
 * ships, this adapter does the same job with an effect. It rebuilds the whole
 * child list on every change rather than patching it, which is fine for the
 * sizes this application deals with, and it keeps the call sites identical to
 * what the documented API will look like.
 *
 * When bindChildren lands, this class is the only thing to delete.
 */
public final class Children {

    private Children() {
    }

    public static <T> Registration bind(Component owner, HasComponents container, ListSignal<T> list,
            SerializableFunction<ValueSignal<T>, Component> factory) {
        return Signal.effect(owner, () -> {
            var items = list.get();
            container.removeAll();
            items.forEach(item -> container.add(factory.apply(item)));
        });
    }

    /**
     * The same thing for a plain list behind a signal, where the items are not
     * individually reactive.
     */
    public static <T> Registration bindValues(Component owner, HasComponents container, Signal<java.util.List<T>> list,
            SerializableFunction<T, Component> factory) {
        return Signal.effect(owner, () -> {
            var items = list.get();
            container.removeAll();
            items.forEach(item -> container.add(factory.apply(item)));
        });
    }
}
