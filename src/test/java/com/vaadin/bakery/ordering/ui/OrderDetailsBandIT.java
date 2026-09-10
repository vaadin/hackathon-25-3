package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.BrowserIT;
import com.vaadin.testbench.BrowserTest;

/**
 * BOARD-13. The tiles in an expanded row answer to the table, not to the window.
 *
 * This is the container query in the application, and the only test that can
 * see it. Opening the order panel narrows the table while the browser window
 * never moves, so a media query would notice nothing and the tiles would keep a
 * layout meant for twice the width. Nothing on the server can tell the
 * difference; the numbers only exist in the browser.
 */
class OrderDetailsBandIT extends BrowserIT {

    /**
     * A row is expanded the way a person expands one: by clicking the cell they
     * can see. Clicking the {@code td} in the grid's shadow root does nothing,
     * because the item click event comes from the slotted cell content.
     */
    private void expandTheFirstRow() {
        script("var g = document.querySelector('vaadin-grid');"
                + "var cells = [...g.querySelectorAll('vaadin-grid-cell-content')];"
                + "var reference = cells.filter(function (c) {"
                + "  return /^ORD-/.test(c.innerText.trim()); })[0];"
                + "reference.click();");
    }

    /** Tiles per row, read from where they actually sit. */
    private long tilesPerRow() {
        return (Long) script("var tiles = [...document.querySelectorAll('.order-board__tile')];"
                + "if (tiles.length === 0) { return 0; }"
                + "var top = Math.round(tiles[0].getBoundingClientRect().top);"
                + "return tiles.filter(function (t) {"
                + "  return Math.round(t.getBoundingClientRect().top) === top; }).length;");
    }

    @BrowserTest
    void narrowingTheTableReflowsTheTilesWithTheWindowUnmoved() {
        signIn("barista@bakery.test", "barista");
        open("/orders");
        var windowWidth = script("return window.innerWidth;");

        expandTheFirstRow();
        waitUntil("return document.querySelectorAll('.order-board__tile').length > 0;");
        long wide = tilesPerRow();
        assertTrue(wide >= 2, "the band lays its tiles out in a row, found " + wide + " at " + whereAmI());

        // The panel takes half the table and the window does not move. It opens
        // from the edit column, which is the only way the board opens it: this
        // used to dispatch a double click on a reference cell, from the days
        // when the board listened for one, and it went on dispatching it into a
        // board that had stopped listening. Nothing failed for a while because
        // the wait that follows it was the assertion.
        script("var button = document.querySelector("
                + "'vaadin-grid-cell-content .order-board__edit');"
                + "button.click();");
        waitUntil("return document.querySelectorAll('.order-detail').length === 1;");

        // Opening the panel closes the band on purpose: the panel says
        // everything the band says and more, and two views of one order at once
        // is one of them wasting half the board. So the band is opened again,
        // now inside a table half the width, which is the whole measurement.
        expandTheFirstRow();
        waitUntil("return document.querySelectorAll('.order-board__tile').length > 0;");

        assertTrue(script("return window.innerWidth;").equals(windowWidth),
                "the browser window never moved");
        long narrow = tilesPerRow();
        assertTrue(narrow > 0, "the band is still there at " + whereAmI());
        assertTrue(narrow <= wide,
                "the tiles reflowed to the narrower table: " + wide + " per row became " + narrow);
    }
}
