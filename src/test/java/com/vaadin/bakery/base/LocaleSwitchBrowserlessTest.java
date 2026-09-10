package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.ui.HomeView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Paragraph;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** FND-05: switching language changes the text, with no reload. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class LocaleSwitchBrowserlessTest extends SpringBrowserlessTest {

    @Test
    void switchingLocaleTranslatesTheView() {
        UI.getCurrent().setLocale(Locale.ENGLISH);
        navigate(HomeView.class);
        var english = find(Paragraph.class).single().getText();

        UI.getCurrent().setLocale(Locale.of("es"));
        navigate(HomeView.class);
        var spanish = find(Paragraph.class).single().getText();

        assertNotEquals(english, spanish, "The tagline should be translated");
        assertEquals("Pan, bolleria y tartas, hechos esta manana.", spanish);
    }

    /**
     * POL2-04. The language menu offered two languages and said nothing about
     * which one was in force, next to a theme menu that has always ticked its
     * own. One tick at a time, and it follows the locale rather than the click,
     * because the language can also be changed from somewhere else.
     */
    @Test
    void theLanguageMenuTicksTheLanguageInUse() {
        UI.getCurrent().setLocale(Locale.ENGLISH);
        navigate(HomeView.class);

        assertEquals("English", tickedLanguage(), "the one in force is the one ticked");

        UI.getCurrent().setLocale(Locale.of("es"));
        assertEquals("Espanol", tickedLanguage(), "and it moves with the language");
    }

    /**
     * The one ticked entry of the language menu, by name.
     *
     * The language menu is the one whose items are the two language names: the
     * shell has four menu bars in its header and none of them is reachable by a
     * class of ours.
     */
    private String tickedLanguage() {
        var names = java.util.List.of("English", "Espanol");
        var ticked = find(com.vaadin.flow.component.menubar.MenuBar.class).all().stream()
                .flatMap(menu -> menu.getItems().stream())
                .flatMap(item -> item.getSubMenu().getItems().stream())
                .filter(item -> names.contains(item.getText()))
                .filter(com.vaadin.flow.component.contextmenu.MenuItem::isChecked)
                .map(com.vaadin.flow.component.contextmenu.MenuItem::getText)
                .toList();
        assertEquals(1, ticked.size(), "exactly one language is ticked, found " + ticked);
        return ticked.getFirst();
    }
}
