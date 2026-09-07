package com.vaadin.bakery.base.i18n;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import java.util.Locale;

/**
 * Binds interface text to the current locale, so switching language updates what
 * is already on screen instead of forcing a reload. Views call these instead of
 * calling getTranslation once at construction time and freezing the result.
 *
 * Three primitives cover every case in this application:
 *
 * <ul>
 * <li>{@link #bindText} for a component that carries its own text.
 * <li>{@link #bind} for a string setter something else owns: a label, a
 * placeholder, an aria label, a grid column header.
 * <li>{@link #onLocale} for content that has to be rebuilt rather than
 * reassigned, such as a table whose header row is fixed at creation.
 * </ul>
 *
 * A string read once and thrown away needs none of them. A notification is
 * composed at the moment it is shown, so the locale in force then is already the
 * right one.
 */
public final class Translations {

    /**
     * Marks a component whose text is already bound.
     *
     * A signal binding cannot be released: `HasText.bindText` returns a
     * `SignalBinding` with no way to undo it, and binding a second time throws
     * `BindingActiveException`. Attaching twice is not exotic, it is what a
     * `Dialog` does every time it opens, so without this the second open of the
     * product editor threw from inside an attach listener. Bind once, and let
     * the binding outlive the detach the way the component does.
     */
    private static final String BOUND = Translations.class.getName() + ".bound";

    private Translations() {
    }

    private static boolean alreadyBound(Component component) {
        if (ComponentUtil.getData(component, BOUND) != null) {
            return true;
        }
        ComponentUtil.setData(component, BOUND, Boolean.TRUE);
        return false;
    }

    /** Binds a component's own text. */
    public static <C extends Component & HasText> C bindText(C component, String key, Object... params) {
        component.whenAttached(ui -> {
            if (!alreadyBound(component)) {
                component.bindText(ui.localeSignal()
                        .map(locale -> component.getTranslation(locale, key, params)));
            }
            return () -> {
            };
        });
        return component;
    }

    /**
     * Binds a component's own text when the text depends on more than the key:
     * a count, a formatted amount, anything else held in a signal. The computed
     * reads the locale signal and whatever the function reads, so it re-runs on
     * either.
     */
    public static <C extends Component & HasText> C bindText(C component,
            SerializableFunction<Locale, String> text) {
        component.whenAttached(ui -> {
            if (!alreadyBound(component)) {
                component.bindText(Signal.computed(() -> text.apply(ui.localeSignal().get())));
            }
            return () -> {
            };
        });
        return component;
    }

    /**
     * Binds a string setter that lives on something other than the component
     * itself. The owner is whatever sits in the component tree, and it is what
     * decides when the binding starts and stops: for a column header that is the
     * grid, not the column.
     */
    public static <C extends Component> C bind(C owner, SerializableConsumer<String> setter, String key,
            Object... params) {
        onLocale(owner, locale -> setter.accept(owner.getTranslation(locale, key, params)));
        return owner;
    }

    /**
     * Runs the action now and again on every locale change, for content that has
     * to be rebuilt rather than reassigned.
     */
    public static Registration onLocale(Component owner, SerializableConsumer<Locale> action) {
        return owner.whenAttached(ui -> Signal.effect(owner, () -> action.accept(ui.localeSignal().get())));
    }
}
