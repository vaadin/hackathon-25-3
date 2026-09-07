package com.vaadin.bakery.base.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.ui.AppearanceSettings.Theme;
import com.vaadin.bakery.catalogue.ui.StorefrontView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.page.ColorScheme;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Lumo or Aura, chosen at runtime, and remembered.
 *
 * The preference lives in the session rather than in the layout because the
 * layout is rebuilt on navigation: when it lived in the layout, choosing a
 * theme and clicking a link put you back where you started.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class ThemeSelectorBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private ApplicationContext context;

    private AppearanceSettings appearance() {
        return context.getBean(AppearanceSettings.class);
    }

    @Test
    void theBakeryColoursOnLumoAreTheDefault() {
        navigate(HomeView.class);

        assertEquals(Theme.BAKERY_LUMO, appearance().currentTheme(),
                "Lumo is what the old Bakery looked like, and the bakery's own colours on top of it "
                        + "are what a visitor should meet first");
    }

    @Test
    void aPlainThemeAddsNothingOfOurs() {
        assertEquals(false, Theme.DEFAULT_LUMO.hasBakeryPalette(), "plain Lumo has to look exactly like Lumo");
        assertEquals(false, Theme.DEFAULT_AURA.hasBakeryPalette(), "and plain Aura like Aura");
        assertEquals(true, Theme.BAKERY_LUMO.hasBakeryPalette());
        assertEquals(true, Theme.BAKERY_AURA.hasBakeryPalette());
    }

    @Test
    void thePlainAndPaintedVariantsShareTheirThemeStylesheet() {
        assertEquals(Theme.DEFAULT_LUMO.stylesheet(), Theme.BAKERY_LUMO.stylesheet());
        assertEquals(Theme.DEFAULT_AURA.stylesheet(), Theme.BAKERY_AURA.stylesheet());
        assertNotEquals(Theme.DEFAULT_LUMO.stylesheet(), Theme.DEFAULT_AURA.stylesheet());
    }

    @Test
    void thePreferenceSurvivesNavigation() {
        navigate(HomeView.class);
        appearance().theme().set(Theme.DEFAULT_AURA);
        appearance().dark().set(true);

        navigate(StorefrontView.class);

        assertEquals(Theme.DEFAULT_AURA, appearance().currentTheme(), "the theme is still the one that was chosen");
        assertTrue(appearance().isDark(), "and so is the colour scheme");
        assertEquals(ColorScheme.Value.DARK, UI.getCurrent().getPage().getColorScheme(),
                "the page was told about it again on the new view");
    }

    @Test
    void theDarkToggleGoesThroughTheSharedSettings() {
        navigate(HomeView.class);
        var toggle = find(Button.class).withAriaLabel("Toggle dark mode").single();

        test(toggle).click();
        runPendingSignalsTasks();

        assertTrue(appearance().isDark(), "the button writes to the session, not to the layout");
        assertEquals(ColorScheme.Value.DARK, UI.getCurrent().getPage().getColorScheme());
    }

    @Test
    void fourVariantsAreOffered() {
        assertEquals(4, Theme.values().length, "each theme, plain and in the bakery's colours");
        assertEquals("app.theme.DEFAULT_LUMO", Theme.DEFAULT_LUMO.translationKey());
        assertEquals("app.theme.BAKERY_AURA", Theme.BAKERY_AURA.translationKey());
    }
}
