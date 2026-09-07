package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.menu.MenuConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * FIX-04. A product page is headed by the product.
 *
 * The defect was not in the view. The tab title was always right, because the
 * router resolves it with the route parameters, and the header was always wrong,
 * because the shell asks {@code MenuConfiguration.getPageHeader}, which calls
 * the same generator with a context whose parameters are empty. One generator,
 * two answers, and only one of them visible in a test that looks at the view.
 *
 * So this asserts on the call the shell actually makes.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class ProductHeaderBrowserlessTest extends SpringBrowserlessTest {

    private String headerOf(Component view) {
        return MenuConfiguration.getPageHeader(view).orElse("");
    }

    @Test
    void theHeaderOfAProductPageIsTheProductName() {
        navigate("products/almond-croissant", ProductDetailView.class);

        assertEquals("Almond croissant", headerOf(find(ProductDetailView.class).single()));
    }

    @Test
    void adifferentProductGetsItsOwnName() {
        navigate("products/carrot-cake", ProductDetailView.class);

        assertEquals("Carrot cake", headerOf(find(ProductDetailView.class).single()));
    }

    /** A slug nobody sells still says so, which is the fallback doing its job. */
    @Test
    void aSlugThatIsNotSoldStillSaysSo() {
        navigate("products/there-is-no-such-thing", ProductDetailView.class);

        assertEquals("We cannot find that product", headerOf(find(ProductDetailView.class).single()));
    }

    /**
     * The storefront had no title of its own at all, so the same call answered
     * with the class name. Found while fixing the product header.
     */
    @Test
    void theStorefrontHasATitleRatherThanAClassName() {
        navigate(StorefrontView.class);

        var header = headerOf(find(StorefrontView.class).single());
        assertEquals("Shop", header);
        assertTrue(!header.contains("View"), "a page header is not a class name");
    }
}
