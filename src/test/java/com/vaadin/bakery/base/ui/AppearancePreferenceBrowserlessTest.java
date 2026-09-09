package com.vaadin.bakery.base.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.base.ui.AppearanceSettings.Theme;
import com.vaadin.bakery.people.AppearancePreferences;
import com.vaadin.browserless.SpringBrowserlessTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * FND-18. The theme belongs to the person, not to the session.
 *
 * It used to live in a session scoped bean, so it survived a navigation and not
 * a logout: everyone who preferred dark mode set it again every morning until
 * they stopped bothering. It is now on the user, which also means it survives a
 * restart, and null still means never chosen rather than chose the default.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AppearancePreferenceBrowserlessTest extends SpringBrowserlessTest {

    private static final String ADMIN = "admin@bakery.test";

    @Autowired
    private AppearancePreferences preferences;

    @Autowired
    private org.springframework.context.ApplicationContext context;

    @BeforeEach
    void signIn() {
        TestLogin.asAdmin();
    }

    private AppearanceSettings appearance() {
        return context.getBean(AppearanceSettings.class);
    }

    @Test
    void choosingAThemeWritesItAgainstThePerson() {
        navigate(OpeningHoursView.class);

        appearance().chooseTheme(Theme.DEFAULT_AURA);

        var stored = preferences.of(ADMIN).orElseThrow(() -> new AssertionError("nothing was remembered"));
        assertEquals(Theme.DEFAULT_AURA.name(), stored.theme());
    }

    @Test
    void theDarkToggleIsRememberedToo() {
        navigate(OpeningHoursView.class);
        boolean before = appearance().isDark();

        appearance().toggleDark();

        assertEquals(!before, preferences.of(ADMIN).orElseThrow().dark());
    }

    /**
     * The part that was broken: a fresh session, which is what a login is,
     * comes back to the choice rather than to the default.
     */
    @Test
    void aFreshSessionPicksTheChoiceUpAgain() {
        navigate(OpeningHoursView.class);
        appearance().chooseTheme(Theme.DEFAULT_LUMO);
        if (!appearance().isDark()) {
            appearance().toggleDark();
        }

        // A new session bean is exactly what the next login gets.
        var afterLoggingBackIn = appearance();
        afterLoggingBackIn.loadForCurrentUser();

        assertEquals(Theme.DEFAULT_LUMO, afterLoggingBackIn.currentTheme(), "the theme came back");
        assertTrue(afterLoggingBackIn.isDark(), "and so did the colour scheme");
    }

    /** Nobody has chosen for the baker, so the baker gets the default. */
    @Test
    void somebodyWhoNeverChoseHasNothingStored() {
        assertFalse(preferences.of("baker@bakery.test").isPresent(),
                "never chosen is not the same as chose the default");
    }

    /**
     * The browser is told as well, because the row on the person dies with the
     * session at logout and a visitor who never signs in has no row at all.
     *
     * What is asserted here is the call, not the storage: a browserless test
     * has no browser to store anything in, so the write shows up as the
     * pending JavaScript the UI would have sent. The round trip itself is the
     * browser tier's job.
     */
    @Test
    void choosingAlsoTellsTheBrowser() {
        navigate(OpeningHoursView.class);

        appearance().chooseTheme(Theme.DEFAULT_LUMO);

        var pending = com.vaadin.flow.component.UI.getCurrent().getInternals()
                .dumpPendingJavaScriptInvocations().stream()
                .map(invocation -> invocation.getInvocation().getExpression()
                        + " " + invocation.getInvocation().getParameters())
                .toList()
                .toString();
        assertTrue(pending.contains("localStorage"), "it writes to local storage: " + pending);
        assertTrue(pending.contains("bakery.theme"), pending);
        assertTrue(pending.contains(Theme.DEFAULT_LUMO.name()), pending);
    }
}
