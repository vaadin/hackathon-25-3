package com.vaadin.bakery;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.testbench.BrowserTest;

/**
 * The whole application in one pass: sign in, browse, order, serve.
 *
 * This is the test that fails when a route moves. Every other test in this
 * suite knows the route it is testing, so renaming one breaks that one test and
 * looks local; this one walks the path a person walks, so it breaks when the
 * path breaks. The product page moved from {@code /products/{slug}} to
 * {@code /shop/product/{slug}} during the repair pass and nothing noticed,
 * which is why this exists.
 *
 * It asserts shallowly on purpose. Depth belongs to the tests that own each
 * screen; what this one owns is that the screens are still reachable from one
 * another in the order somebody uses them.
 */
@SuppressWarnings("unchecked")
class SmokeIT extends BrowserIT {

    @BrowserTest
    void aCustomerCanReachAProductAndABaristaCanReachTheBoard() {
        // The shop, anonymously, which is where a customer starts.
        open("/shop");
        assertTrue(count(".product-card") > 0, "the catalogue has cards at " + whereAmI());

        // A product, over the catalogue rather than instead of it.
        var href = (String) script("var a = document.querySelector('.product-card a');"
                + "return a ? a.getAttribute('href') : null;");
        assertTrue(href != null && href.startsWith("shop/product/"),
                "a card links to a product page under the shop, got " + href);
        open("/" + href);
        assertTrue(count(".product-view") == 1, "the product panel opened at " + whereAmI());
        assertTrue(count(".product-card") > 0, "and the catalogue is still behind it");

        // The basket.
        open("/cart");
        assertTrue(bodyText().length() > 0, "the cart renders at " + whereAmI());

        // The counter.
        signIn("barista@bakery.test", "barista");
        open("/orders");
        assertTrue(navigationOffers("orders"), "the board is in the counter's menu");
        assertTrue(!navigationOffers("admin/users"), "and the staff list is not: " + navigation());
        assertTrue(count("vaadin-grid") == 1, "the board is a grid at " + whereAmI());
        assertTrue(rows() > 0, "with orders in it, found " + rows());

        // The kitchen, which belongs to a baker and not to the counter. That
        // the barista is refused here is itself part of the walk.
        open("/kitchen");
        assertTrue(bodyText().contains("Access is denied"),
                "the counter cannot open the kitchen at " + whereAmI());
        signIn("baker@bakery.test", "baker");
        open("/kitchen");
        assertTrue(navigationOffers("kitchen"), "the kitchen is in a baker's menu");
        assertTrue(!navigationOffers("admin/invoices"), "and the invoices are not: " + navigation());
        assertTrue(count(".kitchen-board__column") >= 3,
                "three columns on the wall at " + whereAmI());

        // And the numbers.
        signIn("admin@bakery.test", "admin");
        open("/admin/dashboard");
        assertTrue(navigationOffers("admin/users"), "an administrator is offered everything: " + navigation());
        assertTrue(navigationOffers("admin/invoices"), "including the invoices");
        assertTrue(count("vaadin-dashboard-widget") >= 4, "four widgets at " + whereAmI());
        assertTrue(count("vaadin-chart") >= 3, "and the charts drew");
    }

    /**
     * FND-03 belongs here rather than in the browserless tier. The drawer is
     * built from `MenuConfiguration.getMenuEntries`, which filters by the
     * current user, and it is built on the thread that holds the session lock:
     * a browserless test that sets the security context from the test thread
     * sees only the public entries however it signs in. In a browser there is
     * one authentication and the menu is the one a person sees.
     */
    private java.util.List<String> navigation() {
        return (java.util.List<String>) script(
                "return [...document.querySelectorAll('vaadin-side-nav-item')]"
                        + ".map(function (i) { return i.getAttribute('path'); })"
                        + ".filter(Boolean);");
    }

    private boolean navigationOffers(String path) {
        return navigation().contains(path);
    }

    private String bodyText() {
        return (String) script("return document.body.innerText;");
    }

    private long rows() {
        return (Long) script("var g = document.querySelector('vaadin-grid');"
                + "return g && g.shadowRoot ? g.shadowRoot.querySelectorAll('tbody tr').length : 0;");
    }
}
