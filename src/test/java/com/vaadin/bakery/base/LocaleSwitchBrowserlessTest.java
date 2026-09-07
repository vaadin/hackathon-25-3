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
}
