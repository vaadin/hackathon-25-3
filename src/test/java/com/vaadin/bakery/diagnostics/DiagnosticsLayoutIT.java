package com.vaadin.bakery.diagnostics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.BrowserIT;
import com.vaadin.testbench.BrowserTest;
import java.util.List;
import org.openqa.selenium.Dimension;

/**
 * FIX-09. The diagnostics panels sit side by side, at a shared height.
 *
 * The rule was already written to reflow, and it never did: a CSS grid inside a
 * `VerticalLayout` is only as wide as its own contents, so `auto-fit` had one
 * column to fit into and four panels stacked at 270 pixels down the left of a
 * 1400 pixel page. Nothing on the server can see that, which is why this is a
 * browser test.
 */
class DiagnosticsLayoutIT extends BrowserIT {

    @SuppressWarnings("unchecked")
    private List<Long> tops() {
        return (List<Long>) script("return [...document.querySelectorAll('.diagnostics__panel')]"
                + ".map(p => Math.round(p.getBoundingClientRect().top));");
    }

    @SuppressWarnings("unchecked")
    private List<Long> heights() {
        return (List<Long>) script("return [...document.querySelectorAll('.diagnostics__panel')]"
                + ".map(p => Math.round(p.getBoundingClientRect().height));");
    }

    @BrowserTest
    void thePanelsShareARowAndAHeight() {
        signIn("admin@bakery.test", "admin");
        open("/admin/diagnostics");

        var tops = tops();
        var heights = heights();

        assertTrue(tops.size() >= 3, "there are panels to lay out, found " + tops.size()
                + " at " + whereAmI());
        assertTrue(tops.get(0).equals(tops.get(1)),
                "the first two are on the same row: tops were " + tops);
        assertTrue(heights.get(0).equals(heights.get(1)),
                "and they are the same height: heights were " + heights);
    }

    @BrowserTest
    void atPhoneWidthTheyStackWithoutScrollingSideways() {
        signIn("admin@bakery.test", "admin");
        open("/admin/diagnostics");
        getDriver().manage().window().setSize(new Dimension(420, 900));
        open("/admin/diagnostics");

        var columns = (Long) script("return new Set([...document.querySelectorAll("
                + "'.diagnostics__panel')].map(p => Math.round("
                + "p.getBoundingClientRect().left))).size;");
        var overflow = (Long) script(
                "return document.documentElement.scrollWidth - document.documentElement.clientWidth;");

        assertTrue(columns == 1L, "one column, found " + columns + " at " + whereAmI());
        assertTrue(overflow <= 1L, "and nothing sticks out sideways, overflow was " + overflow);
    }
}
