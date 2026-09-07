package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.component.html.Anchor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** SHOP-04. A filtered catalogue has to be a link somebody can send. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class CatalogueUrlStateBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private ProductRepository products;

    private List<String> visibleProductNames() {
        return find(Anchor.class).all().stream()
                .filter(anchor -> anchor.getHref().startsWith("products/"))
                .map(Anchor::getText)
                .toList();
    }

    @Test
    void openingAFilteredUrlShowsTheSameResultAsClicking() {
        // The browserless navigate helpers take route parameters, not query
        // parameters, so the query string goes through the UI directly.
        UI.getCurrent().navigate("shop", QueryParameters.of("q", "cake"));

        var names = visibleProductNames();
        assertFalse(names.isEmpty());
        assertTrue(names.stream().allMatch(name -> name.toLowerCase().contains("cake")), names.toString());
    }

    @Test
    void aCategorySlugInThePathSelectsThatCategory() {
        navigate("shop/drinks", StorefrontView.class);

        var drinks = products.findByAvailableTrueOrderBySortOrderAsc().stream()
                .filter(product -> product.getCategory().getSlug().equals("drinks"))
                .map(product -> product.getName())
                .sorted()
                .toList();
        assertEquals(drinks, visibleProductNames().stream().sorted().toList());
    }

    @Test
    void anAllergenExclusionSurvivesInTheUrl() {
        UI.getCurrent().navigate("shop", QueryParameters.of("without", "catalogue.allergen.NUTS"));

        assertTrue(visibleProductNames().stream().noneMatch(name -> name.contains("Almond croissant")),
                "the almond croissant carries nuts");
    }
}
