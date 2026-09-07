package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.ui.HomeView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.page.ColorScheme;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * FND-07.
 *
 * The first version of this test asserted that a `dark` theme attribute
 * appeared on the UI element, which is what the Lumo examples do. It passed
 * while the toggle did nothing at all: Aura follows the CSS color scheme, and
 * so does every `light-dark()` value in our own stylesheets, so the attribute
 * changed and no pixel did. Asserting the colour scheme is asserting the thing
 * that actually decides what the user sees.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class ThemeToggleBrowserlessTest extends SpringBrowserlessTest {

    private ColorScheme.Value scheme() {
        return UI.getCurrent().getPage().getColorScheme();
    }

    @Test
    void togglingSwitchesTheColorScheme() {
        navigate(HomeView.class);
        var toggle = find(Button.class).withAriaLabel("Toggle dark mode").single();

        assertEquals(ColorScheme.Value.LIGHT, scheme(), "the application starts light");

        test(toggle).click();
        runPendingSignalsTasks();
        assertEquals(ColorScheme.Value.DARK, scheme(), "one press turns the dark scheme on");

        test(toggle).click();
        runPendingSignalsTasks();
        assertEquals(ColorScheme.Value.LIGHT, scheme(), "and another turns it back");
    }
}
