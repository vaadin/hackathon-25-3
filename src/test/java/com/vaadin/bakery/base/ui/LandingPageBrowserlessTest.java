package com.vaadin.bakery.base.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** The front door: hero, opening hours and the featured row. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class LandingPageBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private CatalogueService catalogue;

    @Test
    void theHeroNamesTheBakery() {
        navigate(HomeView.class);
        assertEquals("Bakery", find(H1.class).single().getText());
    }

    @Test
    void openingHoursAreShownForEveryActiveLocation() {
        navigate(HomeView.class);
        var lines = find(Span.class).withClassName("home-view__hours-line").all();
        assertEquals(3, lines.size());
        assertTrue(lines.getFirst().getText().contains("to"), lines.getFirst().getText());
    }

    @Test
    void theFeaturedRowLinksIntoTheCatalogue() {
        navigate(HomeView.class);
        var productLinks = find(Anchor.class).all().stream()
                .filter(anchor -> anchor.getHref().startsWith("products/"))
                .toList();

        assertFalse(productLinks.isEmpty(), "the landing page should feature something");
        assertEquals(catalogue.featured().size(), productLinks.size());
    }
}
