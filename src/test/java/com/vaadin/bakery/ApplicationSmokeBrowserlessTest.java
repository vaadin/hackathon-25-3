package com.vaadin.bakery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.base.ui.HomeView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.html.H1;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** FND-01: the application boots and the landing route resolves. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class ApplicationSmokeBrowserlessTest extends SpringBrowserlessTest {

    @Test
    void landingViewRenders() {
        navigate(HomeView.class);
        var heading = find(H1.class).single();
        assertEquals("Bakery", heading.getText());
        assertTrue(find(HomeView.class).all().size() == 1, "The landing view should be attached once");
    }
}
