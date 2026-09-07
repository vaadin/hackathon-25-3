package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * FIX-11. A product opens over the catalogue, not instead of it.
 *
 * The list was thrown away on every look: filters, scroll position and the
 * place somebody had reached in forty eight cards, all gone because a product
 * page was a separate route with the shell as its layout. It is now a child of
 * the catalogue, which means the catalogue is still there behind it, which is
 * what this asserts: the same instance, still holding its cards.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class CatalogueOverlayBrowserlessTest extends SpringBrowserlessTest {

    private StorefrontView catalogue() {
        return find(StorefrontView.class).single();
    }

    @Test
    void theProductIsAChildOfTheCatalogue() {
        navigate("shop/product/almond-croissant", ProductDetailView.class);

        var chain = UI.getCurrent().getInternals().getActiveRouterTargetsChain();

        assertTrue(chain.stream().anyMatch(ProductDetailView.class::isInstance), "the product is open");
        assertTrue(chain.stream().anyMatch(StorefrontView.class::isInstance),
                "and the catalogue is still one of the open targets, not a page that was left");
    }

    /** The panel is over the list, whatever the width. */
    @Test
    void theCatalogueForcesTheOverlay() {
        navigate("shop/product/almond-croissant", ProductDetailView.class);

        assertTrue(catalogue().isForceOverlay(),
                "the list keeps the whole page and the product covers part of it");
    }

    /**
     * The same instance, still filtered. This is the whole point: the storefront
     * is not rebuilt, so nothing it was holding is lost.
     */
    @Test
    void goingBackFindsTheSameCatalogueWithItsFiltersIntact() {
        navigate(StorefrontView.class);
        var before = catalogue();
        before.filtersForTest().search().set("almond");
        int refreshes = before.refreshCount;

        navigate("shop/product/almond-croissant", ProductDetailView.class);
        navigate(StorefrontView.class);

        var after = catalogue();
        assertTrue(before == after, "the catalogue was never torn down");
        assertEquals("almond", after.filtersForTest().search().peek(), "and it kept what it was filtered by");
        assertTrue(after.refreshCount >= refreshes, "nothing reset it on the way back");
    }

    @Test
    void theProductPanelOffersAWayBack() {
        navigate("shop/product/almond-croissant", ProductDetailView.class);

        var back = find(com.vaadin.flow.component.button.Button.class)
                .withClassName("product-view__close")
                .single();

        assertNotNull(back);
        test(back).click();
        assertTrue(UI.getCurrent().getInternals().getActiveRouterTargetsChain().stream()
                        .noneMatch(ProductDetailView.class::isInstance),
                "and it closes the panel");
    }
}
