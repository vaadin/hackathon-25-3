package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.BrowserIT;
import com.vaadin.testbench.BrowserTest;
import java.util.List;

/**
 * KIT-08. Opening the summary costs the board nothing.
 *
 * The board is on a wall and a baker reading the summary is answering a second
 * question, not giving up the first. If the columns move, the cards reflow and
 * the person loses the place they were looking at. No component API can answer
 * whether that happened: the question is a measurement in pixels.
 */
class KitchenSummaryOverlayIT extends BrowserIT {

    @SuppressWarnings("unchecked")
    private List<Long> columnWidths() {
        return (List<Long>) script("return [...document.querySelectorAll('.kitchen-board__column')]"
                + ".map(function (c) { return Math.round(c.getBoundingClientRect().width); });");
    }

    private void toggleSummary() {
        script("var buttons = [...document.querySelectorAll('.kitchen-board__header vaadin-button')];"
                + "buttons[buttons.length - 1].click();");
        // The layout carries a `transition` attribute while it animates, and
        // while it does the master is briefly narrower and the panel briefly
        // beside it rather than over it. Measuring then measures the animation:
        // 451 became 365 and came back. Two consecutive equal readings are not
        // enough either, because a transition can hold a value across a frame.
        waitForTheLayoutToSettle();
    }

    private void waitForTheLayoutToSettle() {
        settled(() -> script("var m = document.querySelector('vaadin-master-detail-layout');"
                + "return m && !m.hasAttribute('transition');"));
    }

    /**
     * What the summary must not take is the board's share of its own container.
     *
     * The board's absolute pixels are the wrong yardstick, and it took several
     * runs to see why: the shell opens its drawer a moment after the first
     * render, and that moves the board by the drawer's width with nothing to do
     * with the summary. Waiting for the drawer did not settle it either, so the
     * measurement stopped depending on it: one script call returns the board's
     * width and the width its columns occupy together, in the same frame, and
     * what this compares is the share. A summary that stole width from the
     * board would move that share whatever the drawer was doing.
     */
    @BrowserTest
    void openingTheSummaryTakesNoWidthFromTheBoard() {
        signIn("baker@bakery.test", "baker");
        open("/kitchen");
        double before = shareOfTheBoardTheColumnsOccupy();
        assertTrue(before > 0.8, "the columns fill the board to begin with, they filled " + before);

        toggleSummary();

        double during = shareOfTheBoardTheColumnsOccupy();
        assertTrue(Math.abs(during - before) < 0.02,
                "the columns kept their share of the board: " + before + " then " + during);

        toggleSummary();
        double after = shareOfTheBoardTheColumnsOccupy();
        assertTrue(Math.abs(after - before) < 0.02,
                "and closing the summary changed nothing either: " + after);
    }

    /** Read in one call, so the two numbers describe the same instant. */
    private double shareOfTheBoardTheColumnsOccupy() {
        var measured = (Number) script(
                "var master = document.querySelector('.kitchen-board__master');"
                        + "var columns = [...document.querySelectorAll('.kitchen-board__column')];"
                        + "if (!master || columns.length === 0) { return 0; }"
                        + "var total = columns.reduce(function (sum, c) {"
                        + "  return sum + c.getBoundingClientRect().width; }, 0);"
                        + "return Math.round((total / master.getBoundingClientRect().width) * 1000) / 1000;");
        return measured.doubleValue();
    }

    /**
     * And the panel is as wide as its table and no wider, because it covers the
     * board and every millimetre it takes is board a baker cannot read.
     */
    @BrowserTest
    void thePanelIsNoWiderThanTheSummaryItContains() {
        signIn("baker@bakery.test", "baker");
        open("/kitchen");
        toggleSummary();

        var measured = (List<Long>) script(
                "var panel = document.querySelector('.kitchen-board__summary');"
                        + "var table = panel ? panel.querySelector('table') : null;"
                        + "if (!panel || !table) { return null; }"
                        + "return [Math.round(panel.getBoundingClientRect().width),"
                        + " Math.round(table.getBoundingClientRect().width)];");

        assertTrue(measured != null, "the summary has a table at " + whereAmI());
        long panel = measured.get(0);
        long table = measured.get(1);
        // The panel carries its own padding, so it is wider than the table by
        // that much and no more. Twice the table would mean it took a width
        // somebody chose rather than the width the content needs.
        assertTrue(panel < table * 2,
                "the panel follows its contents: panel " + panel + ", table " + table);
    }
}
