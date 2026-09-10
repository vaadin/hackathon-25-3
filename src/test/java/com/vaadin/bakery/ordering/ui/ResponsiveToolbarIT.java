package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.BrowserIT;
import com.vaadin.testbench.BrowserTest;
import org.openqa.selenium.Dimension;

/**
 * POL2-14. The order board's toolbar at the width of a telephone.
 *
 * Six controls in one row do not fit 390 pixels, and what gave way was the
 * search field: it has a floor of 18rem and the row overlapped rather than
 * wrapping. This is a breakpoint and a breakpoint is a measurement, so the
 * server cannot answer it: it knows the two groups it built and nothing about
 * where the browser put them.
 *
 * The breakpoint is a container query on the board's own width rather than a
 * media query on the window's, because opening an order narrows the board while
 * the window never moves. That is also why this measures the two rows against
 * each other rather than against the viewport.
 */
class ResponsiveToolbarIT extends BrowserIT {

    private long top(String selector) {
        return (Long) script("return Math.round(document.querySelector('" + selector
                + "').getBoundingClientRect().top);");
    }

    private long bottom(String selector) {
        return (Long) script("return Math.round(document.querySelector('" + selector
                + "').getBoundingClientRect().bottom);");
    }

    @BrowserTest
    void atPhoneWidthTheFiltersAreOnARowUnderTheButtons() {
        signIn("barista@bakery.test", "barista");
        open("/orders");
        getDriver().manage().window().setSize(new Dimension(390, 844));
        // The toolbar reflows on a resize observer rather than a round trip, and
        // the drawer closes at this width, which changes the board's own width.
        open("/orders");
        waitUntil("return !!document.querySelector('.order-board__browse')"
                + " && !!document.querySelector('.order-board__commands');");

        var commandsBottom = bottom(".order-board__commands");
        var browseTop = top(".order-board__browse");

        assertTrue(browseTop >= commandsBottom,
                "what you are looking at is under what you can do: commands ended at " + commandsBottom
                        + " and the filters start at " + browseTop + ", at " + whereAmI());
    }

    /**
     * And the field is still usable rather than a sliver. A search box narrower
     * than a word is the defect this replaces, wearing different clothes.
     */
    @BrowserTest
    void theSearchFieldKeepsARealWidth() {
        signIn("barista@bakery.test", "barista");
        open("/orders");
        getDriver().manage().window().setSize(new Dimension(390, 844));
        open("/orders");
        waitUntil("return !!document.querySelector('.order-board__browse vaadin-text-field');");

        var field = (Long) script("return Math.round(document.querySelector("
                + "'.order-board__browse vaadin-text-field').getBoundingClientRect().width);");
        var overflow = (Long) script(
                "return document.documentElement.scrollWidth - document.documentElement.clientWidth;");

        assertTrue(field >= 120, "the search field is still a field, measured " + field);
        assertTrue(overflow <= 1, "and the page does not scroll sideways, overflow was " + overflow);
    }

    /**
     * Wide again, and the two groups share a line. Without this the test above
     * would pass on a toolbar that had simply been stacked for ever.
     */
    @BrowserTest
    void atDeskWidthTheToolbarIsOneRow() {
        signIn("barista@bakery.test", "barista");
        open("/orders");
        getDriver().manage().window().setSize(new Dimension(1400, 1000));
        open("/orders");
        waitUntil("return !!document.querySelector('.order-board__commands');");

        var commandsTop = top(".order-board__commands");
        var browseBottom = bottom(".order-board__browse");

        assertTrue(commandsTop < browseBottom,
                "the buttons are level with the filters: commands at " + commandsTop
                        + " and the filters end at " + browseBottom);
    }
}
