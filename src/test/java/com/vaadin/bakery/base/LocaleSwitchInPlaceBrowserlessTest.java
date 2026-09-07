package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.ui.OpeningHoursView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.sidenav.SideNavItem;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * FND-05, the half a navigating test cannot see: switching language retranslates
 * the view that is already on screen. Navigating after setLocale rebuilds the
 * view, so it passes even when nothing is bound to the locale, which is the trap
 * recorded in specs/FEEDBACK-PLATFORM.md. This test never navigates after the
 * switch.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class LocaleSwitchInPlaceBrowserlessTest extends SpringBrowserlessTest {

    @Test
    void switchingLocaleRetranslatesTheViewInPlace() {
        UI.getCurrent().setLocale(Locale.ENGLISH);
        navigate(OpeningHoursView.class);
        assertEquals("When we are open", find(H2.class).single().getText());

        UI.getCurrent().setLocale(Locale.of("es"));

        assertEquals("Cuando abrimos", find(H2.class).single().getText(),
                "the heading follows the locale without navigating");
        assertEquals("Pasate, o pide con antelacion y lo recoges.",
                find(Paragraph.class).single().getText(),
                "and so does the lead paragraph");
        assertTrue(navigationLabels().contains("Tienda"),
                "and so do the navigation entries, which were " + navigationLabels());
    }

    private List<String> navigationLabels() {
        return find(SideNavItem.class).all().stream().map(SideNavItem::getLabel).toList();
    }
}
