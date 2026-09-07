package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.BrowserIT;
import com.vaadin.testbench.BrowserTest;

/**
 * FND-14. The tab and the trail say the route's own name.
 *
 * This is the blind spot that made the browser tier necessary. A
 * `PageTitleGenerator` annotated `@Component` becomes the whole application's
 * generator and overrides `@PageTitle` on every route: ours knew how to name a
 * product and returned a constant for everything else, so every page in the
 * bakery was called "Bakery". Two hundred and twenty four browserless tests
 * were green throughout, because the tier resolves the title through the router
 * and the router agreed with itself.
 */
class PageTitleIT extends BrowserIT {

    private String tab() {
        return getDriver().getTitle();
    }

    private String header() {
        return (String) script("var t = document.querySelector('.view-title');"
                + "return t ? t.innerText.trim() : '';");
    }

    @BrowserTest
    void eachRouteNamesItselfInTheTabAndInTheHeader() {
        signIn("admin@bakery.test", "admin");

        open("/hours");
        assertTrue(tab().contains("Opening hours") || tab().contains("Hours"),
                "the tab names this route, it said " + tab());
        assertTrue(header().contains("Hours") || header().contains("Opening"),
                "and so does the header, it said " + header());

        open("/admin/dashboard");
        assertTrue(!tab().equals("Bakery"), "not the application name, it said " + tab());
        assertTrue(header().toLowerCase().contains("dashboard"),
                "the header followed the route, it said " + header());

        open("/admin/invoices");
        assertTrue(header().toLowerCase().contains("invoice"),
                "and again on the next route, it said " + header());
    }

    /** A product page, which is where the dynamic generator actually earns its place. */
    @BrowserTest
    void aProductPageIsNamedAfterTheProduct() {
        open("/shop/product/almond-croissant");

        assertTrue(tab().contains("Almond croissant"),
                "the tab is the product's name, it said " + tab());
    }
}
