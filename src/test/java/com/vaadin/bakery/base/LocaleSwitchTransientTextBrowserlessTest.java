package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.ordering.ui.CheckoutContactView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * FND-11. What must not follow the language, and what must not be lost.
 *
 * Two rules that pull the other way from every other locale test here. A
 * notification that is already on screen keeps the language it appeared in,
 * because it is a record of something that happened and retranslating it would
 * rewrite history in front of the reader. And a half filled form keeps every
 * value that was typed, because rebuilding a view to retranslate it is exactly
 * how a form gets emptied.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class LocaleSwitchTransientTextBrowserlessTest extends SpringBrowserlessTest {

    @Test
    void aNotificationKeepsTheLanguageItAppearedIn() {
        UI.getCurrent().setLocale(Locale.ENGLISH);
        navigate(com.vaadin.bakery.base.ui.OpeningHoursView.class);
        var shown = Notification.show("Saved");

        UI.getCurrent().setLocale(Locale.of("es"));

        // The text is a property of the notification rather than a child, so
        // this reads the property. `getTextRecursively` returns nothing for it.
        assertEquals("Saved", shown.getElement().getProperty("text", ""),
                "the notification is a record of something that already happened");
    }

    @Test
    void ahalfFilledFormKeepsWhatWasTyped() {
        UI.getCurrent().setLocale(Locale.ENGLISH);
        navigate(CheckoutContactView.class);
        var first = find(TextField.class).withLabel("First name").single();
        var last = find(TextField.class).withLabel("Last name").single();
        test(first).setValue("Marta");
        test(last).setValue("Somoza");

        UI.getCurrent().setLocale(Locale.of("es"));

        assertEquals("Marta", first.getValue(), "the value survived the switch");
        assertEquals("Somoza", last.getValue());
        assertTrue(find(TextField.class).withLabel("Nombre").all().size() > 0
                        || "Nombre".equals(first.getLabel()),
                "and the label around it is Spanish now, which is what makes this hard");
    }
}
