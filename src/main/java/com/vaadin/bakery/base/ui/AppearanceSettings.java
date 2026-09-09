package com.vaadin.bakery.base.ui;

import com.vaadin.bakery.base.security.CurrentUser;
import com.vaadin.bakery.people.AppearancePreferences;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.WebStorage;
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

    /** What the browser remembers, for the days when nobody is signed in. */
    private static final String THEME_KEY = "bakery.theme";
    private static final String DARK_KEY = "bakery.dark";

    private final ValueSignal<Theme> theme = new ValueSignal<>(Theme.BAKERY_LUMO);
    private final ValueSignal<Boolean> dark = new ValueSignal<>(false);

    private final AppearancePreferences preferences;
    private final CurrentUser currentUser;

    /** The browser is asked once per session, and only when it is worth asking. */
    private boolean askedTheBrowser;

    public AppearanceSettings(AppearancePreferences preferences, CurrentUser currentUser) {
        this.preferences = preferences;
        this.currentUser = currentUser;
    }

    /**
     * Picks up whatever this person chose last time.
     *
     * Called by the shell rather than by the constructor: a session bean is
     * built before anybody has signed in, so at construction there is no person
     * to have a preference.
     */
    public void loadForCurrentUser() {
        currentUser.username()
                .flatMap(preferences::of)
                .ifPresent(choice -> {
                    if (choice.theme() != null) {
                        theme.set(Theme.valueOf(choice.theme()));
                    }
                    dark.set(Boolean.TRUE.equals(choice.dark()));
                });
    }

    /**
     * What this browser was last left looking at, for a visitor who is not
     * signed in.
     *
     * A signed in person's choice wins, because it follows them to another
     * machine, so this only runs when there is nobody to ask. Reading the
     * browser is a round trip: the page is already painted in whatever the
     * session defaulted to when the answer arrives, so the change is visible.
     * That is the price of the storage being where it is.
     */
    public void loadFromBrowser(UI ui) {
        if (askedTheBrowser || currentUser.username().isPresent()) {
            return;
        }
        askedTheBrowser = true;
        WebStorage.getItem(ui, WebStorage.Storage.LOCAL_STORAGE, THEME_KEY, stored -> {
            if (stored != null && !stored.isBlank()) {
                try {
                    theme.set(Theme.valueOf(stored));
                } catch (IllegalArgumentException renamed) {
                    // A theme this build no longer has. Keep the default and
                    // forget the name rather than failing a page load over it.
                    WebStorage.removeItem(ui, WebStorage.Storage.LOCAL_STORAGE, THEME_KEY);
                }
            }
        });
        WebStorage.getItem(ui, WebStorage.Storage.LOCAL_STORAGE, DARK_KEY,
                stored -> dark.set(Boolean.parseBoolean(stored)));
    }

    /** Choosing, as opposed to being set, which is what gets remembered. */
    public void chooseTheme(Theme chosen) {
        theme.set(chosen);
        remember();
    }

    /**
     * Both places, on purpose. The row on the user follows a person to another
     * machine and is the one that wins; the browser keeps this machine looking
     * the same after a logout, which destroys the session and everything in it,
     * and it is the only memory a visitor who never signs in has.
     */
    private void remember() {
        currentUser.username()
                .ifPresent(email -> preferences.remember(email, theme.peek().name(), isDark()));
        var ui = UI.getCurrent();
        if (ui != null) {
            WebStorage.setItem(ui, WebStorage.Storage.LOCAL_STORAGE, THEME_KEY, theme.peek().name());
            WebStorage.setItem(ui, WebStorage.Storage.LOCAL_STORAGE, DARK_KEY, String.valueOf(isDark()));
        }
    }

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
        remember();
    }
}
