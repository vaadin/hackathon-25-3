package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.ui.OpeningHoursView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * FND-12. The two titles follow the language, without navigating.
 *
 * There are two of them and they come from different places: the span in the
 * shell's navbar, which the layout binds, and the browser tab, which the router
 * sets. A locale switch has to move both, and the trap is that navigating
 * afterwards moves them anyway, so a test that navigates proves nothing.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class PageTitleLocaleBrowserlessTest extends SpringBrowserlessTest {

    private String shellTitle() {
        return find(Span.class).withClassName("view-title").single().getText();
    }

    private String tabTitle() {
        return UI.getCurrent().getInternals().getTitle();
    }

    @Test
    void theShellTitleAndTheTabTitleBothFollowTheLanguage() {
        UI.getCurrent().setLocale(Locale.ENGLISH);
        navigate(OpeningHoursView.class);
        var shellInEnglish = shellTitle();
        var tabInEnglish = tabTitle();

        UI.getCurrent().setLocale(Locale.of("es"));

        assertNotEquals(shellInEnglish, shellTitle(),
                "the title in the navbar changed, it was " + shellInEnglish);
        assertEquals("Horario", shellTitle(), "and it is the Spanish name of this route");
        assertNotEquals(tabInEnglish, tabTitle(),
                "the browser tab changed too, it was " + tabInEnglish);
    }
}
