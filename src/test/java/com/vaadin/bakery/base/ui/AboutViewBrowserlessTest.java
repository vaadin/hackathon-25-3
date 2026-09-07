package com.vaadin.bakery.base.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Table;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * POL-04. The about page is the first thing to open when a demo behaves
 * strangely, so it has to be right about what is running. It is also the one
 * Kotlin file in the repository, which this test incidentally proves compiles
 * and runs inside the same application.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class AboutViewBrowserlessTest extends SpringBrowserlessTest {

    @Test
    void itReportsThePlatformAndTheFlags() {
        navigate(AboutView.class);

        assertFalse(find(H2.class).all().isEmpty(), "it has a heading");
        var tables = find(Table.class).all();
        assertEquals(2, tables.size(), "one table for the platform, one for the flags");
    }

    @Test
    void theFlagTableCoversEveryFlagWeDependOn() {
        navigate(AboutView.class);

        var text = find(Table.class).all().get(1).getElement().getTextRecursively();
        assertTrue(text.contains("breadcrumbsComponent"), text);
        assertTrue(text.contains("switchComponent"), text);
        assertTrue(text.contains("aiComponents"), text);
    }

    @Test
    void itNamesTheAssistantThatIsActuallyRunning() {
        navigate(AboutView.class);

        // With no key there is no provider, and the page says off rather than
        // naming a stand in. That is the whole point of the row: somebody
        // opening this page during a demo needs to know which it is.
        var text = find(AboutView.class).single().getElement().getTextRecursively();
        assertTrue(text.contains("off") || text.contains("SpringAI"),
                "the page says which provider is answering, got: " + text);
    }
}
