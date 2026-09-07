package com.vaadin.bakery.base.ui;

import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.aura.Aura;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.stereotype.Component;

/**
 * How this visitor wants the application to look.
 *
 * It lives in the session rather than in the layout, because the layout is
 * rebuilt on navigation and a preference that resets when you click a link is
 * not a preference. Both values are signals, so the shell reacts without
 * anybody wiring a listener.
 */
@Component
@VaadinSessionScope
public class AppearanceSettings {

    /**
     * Four choices: the palette first, the theme in brackets.
     *
     * The two default variants load nothing but the theme, so each looks exactly
     * like itself, which is the only way to compare them honestly. The bakery
     * variants add one stylesheet on top that repaints them in the bakery's own
     * colours. The order here is the order in the menu.
     */
    public enum Theme {
        BAKERY_AURA(Aura.STYLESHEET, true),
        BAKERY_LUMO(Lumo.STYLESHEET, true),
        DEFAULT_AURA(Aura.STYLESHEET, false),
        DEFAULT_LUMO(Lumo.STYLESHEET, false);

        private final String stylesheet;
        private final boolean bakeryPalette;

        Theme(String stylesheet, boolean bakeryPalette) {
            this.stylesheet = stylesheet;
            this.bakeryPalette = bakeryPalette;
        }

        public String stylesheet() {
            return stylesheet;
        }

        public boolean hasBakeryPalette() {
            return bakeryPalette;
        }

        public String translationKey() {
            return "app.theme." + name();
        }
    }

    /** Loaded on top of a theme by the two bakery variants. */
    public static final String BAKERY_PALETTE = "styles/themes/bakery.css";

    private final ValueSignal<Theme> theme = new ValueSignal<>(Theme.BAKERY_LUMO);
    private final ValueSignal<Boolean> dark = new ValueSignal<>(false);

    public ValueSignal<Theme> theme() {
        return theme;
    }

    public ValueSignal<Boolean> dark() {
        return dark;
    }

    public Theme currentTheme() {
        return theme.peek();
    }

    public boolean isDark() {
        return Boolean.TRUE.equals(dark.peek());
    }

    public void toggleDark() {
        dark.update(value -> !Boolean.TRUE.equals(value));
    }
}
