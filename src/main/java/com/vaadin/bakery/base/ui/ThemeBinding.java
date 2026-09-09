package com.vaadin.bakery.base.ui;

import com.vaadin.bakery.base.ui.AppearanceSettings.Theme;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;

/**
 * Applies the chosen theme and colour scheme to whatever page a component is on.
 *
 * This used to live inside {@code MainLayout}, which meant a route outside the
 * shell had no theme at all: the login page, which is the first thing anybody
 * sees, arrived in serif type with a black button on a cold load, and looked
 * right afterwards only because a session that had been inside the shell left
 * the stylesheet on the page.
 *
 * No theme is declared statically on the app shell, on purpose, so that plain
 * Lumo and plain Aura can be compared without one leaking into the other. That
 * decision is what makes this class necessary rather than a one line
 * annotation.
 */
public final class ThemeBinding {

    private Registration themeStylesheet;
    private Registration paletteStylesheet;
    private String loadedStylesheet;

    private ThemeBinding() {
    }

    /**
     * Binds a component's page to the appearance settings for as long as it is
     * attached. The returned registration is what
     * {@code Component.whenAttached} releases on detach.
     */
    public static Registration apply(Component owner, AppearanceSettings appearance) {
        var binding = new ThemeBinding();
        return owner.whenAttached(ui -> {
            // Asked here rather than in the shell, because this is the one call
            // both the shell and the login page make, and the login page is
            // exactly where a visitor with no account arrives.
            appearance.loadFromBrowser(ui);

            // Page.setColorScheme, not a theme attribute: Aura follows the CSS
            // color-scheme property, and so does every light-dark() value in our
            // own stylesheets. Setting theme="dark" on the body, which is what
            // the Lumo 24 examples do, changes nothing at all here.
            Signal.effect(owner, () -> ui.getPage().setColorScheme(
                    Boolean.TRUE.equals(appearance.dark().get())
                            ? ColorScheme.Value.DARK
                            : ColorScheme.Value.LIGHT));

            // Exactly one theme stylesheet at a time, plus the bakery palette
            // when the chosen variant asks for it. Loading both themes at once
            // would leave one of them half applied, and a plain theme has to
            // look exactly like itself for the comparison to mean anything.
            Signal.effect(owner, () -> binding.load(ui.getPage(), appearance.theme().get()));

            return () -> {
            };
        });
    }

    private void load(com.vaadin.flow.component.page.Page page, Theme wanted) {
        // Only touch what actually changes. Removing a stylesheet and adding
        // the same URL back in one round trip loses it: the two operations
        // reach the browser together and the add is treated as a duplicate of a
        // sheet that is still there. Switching between Lumo and "Lumo with the
        // bakery colours" left the page with no theme at all until this was
        // split in two.
        if (!wanted.stylesheet().equals(loadedStylesheet)) {
            if (themeStylesheet != null) {
                themeStylesheet.remove();
            }
            themeStylesheet = page.addStyleSheet(wanted.stylesheet());
            loadedStylesheet = wanted.stylesheet();
        }

        if (wanted.hasBakeryPalette() && paletteStylesheet == null) {
            paletteStylesheet = page.addStyleSheet(AppearanceSettings.BAKERY_PALETTE);
        } else if (!wanted.hasBakeryPalette() && paletteStylesheet != null) {
            paletteStylesheet.remove();
            paletteStylesheet = null;
        }
    }
}
