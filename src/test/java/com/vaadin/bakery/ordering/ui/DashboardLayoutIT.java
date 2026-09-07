package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.BrowserIT;
import com.vaadin.testbench.BrowserTest;
import java.util.List;
import org.openqa.selenium.Dimension;

/**
 * FIX-06 and FIX-07. The dashboard is a layout, and a layout is only true in a
 * browser.
 *
 * The defect this replaces was four equal cells in a CSS grid: a series over
 * ninety days got exactly as much width as the word "Ready" and the number
 * beside it, and on a phone the whole thing kept its columns and scrolled
 * sideways. Both of those are measurements, and the server has none: it knows
 * the colspan it asked for and nothing about the pixels that came back.
 */
class DashboardLayoutIT extends BrowserIT {

    @SuppressWarnings("unchecked")
    private List<Long> widgetWidths() {
        return (List<Long>) script("return [...document.querySelectorAll('vaadin-dashboard-widget')]"
                + ".map(w => Math.round(w.getBoundingClientRect().width));");
    }

    @BrowserTest
    void aChartWidgetIsWiderThanTheCountersWhenThereIsRoom() {
        signIn("admin@bakery.test", "admin");
        open("/admin/dashboard");

        var widths = widgetWidths();

        assertTrue(widths.size() >= 4, "four widgets, got " + widths.size() + " at " + whereAmI());
        long narrowest = widths.stream().mapToLong(Long::longValue).min().orElse(0);
        long widest = widths.stream().mapToLong(Long::longValue).max().orElse(0);
        assertTrue(widest > narrowest * 1.5,
                "a chart spans more columns than a counter: widths were " + widths);
    }

    /**
     * The order a baker asks in. Today is the first thing on the page, and the
     * revenue trend is below it, which is a vertical position and not a
     * property of any component.
     */
    @BrowserTest
    void todayComesBeforeTheTrends() {
        signIn("admin@bakery.test", "admin");
        open("/admin/dashboard");

        var tops = (List<Long>) script("return [...document.querySelectorAll('vaadin-dashboard-widget')]"
                + ".map(w => Math.round(w.getBoundingClientRect().top));");

        assertTrue(tops.get(0) <= tops.get(2),
                "the counters are above the revenue chart: tops were " + tops);
    }

    /**
     * Phone width. One column, and nothing wider than the window: a chart that
     * keeps a fixed width is invisible to every other assertion here and is
     * exactly what the report complained about.
     */
    @BrowserTest
    void atPhoneWidthEverythingIsOneColumnAndNothingScrollsSideways() {
        signIn("admin@bakery.test", "admin");
        open("/admin/dashboard");
        getDriver().manage().window().setSize(new Dimension(420, 900));
        // The dashboard reflows on a resize observer, not on a server round trip.
        open("/admin/dashboard");

        var columns = (Long) script("return new Set([...document.querySelectorAll("
                + "'vaadin-dashboard-widget')].map(w => Math.round("
                + "w.getBoundingClientRect().left))).size;");
        var overflow = (Long) script(
                "return document.documentElement.scrollWidth - document.documentElement.clientWidth;");

        assertTrue(columns == 1L, "every widget starts at the same edge, found " + columns
                + " columns at " + whereAmI());
        assertTrue(overflow <= 1L, "and the page does not scroll sideways, overflow was " + overflow);
    }
}
