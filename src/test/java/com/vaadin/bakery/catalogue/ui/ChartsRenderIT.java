package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.BrowserIT;
import com.vaadin.testbench.BrowserTest;
import java.util.List;

/**
 * DASH-06. The charts actually draw.
 *
 * This is the question the browserless tier cannot answer. It knew the three
 * panels were {@code Chart} components with the right series, and it could not
 * know that every one of them was painting Highcharts' own palette on a white
 * plot area, because a chart is drawn by a library in the browser and nothing
 * on the server sees the result.
 */
class ChartsRenderIT extends BrowserIT {

    @BrowserTest
    void theDashboardDrawsItsThreeCharts() {
        signIn("admin@bakery.test", "admin");
        open("/admin/dashboard");

        var drawn = (Long) script("return [...document.querySelectorAll('vaadin-chart')]"
                + ".filter(c => c.shadowRoot && c.shadowRoot.querySelector('svg .highcharts-series')).length;");

        assertTrue(drawn >= 3, "revenue, orders by state and top products all drew, got " + drawn
                + " at " + whereAmI());
    }

    /**
     * And they follow the theme. Styled mode is the difference between a chart
     * that belongs to this application and one that carries its own colours,
     * and it is invisible from the server: the configuration says styled mode
     * either way, and only the rendered fill tells you whether it took.
     */
    @BrowserTest
    @SuppressWarnings("unchecked")
    void theChartsAreInStyledModeAndNotHighchartsOwnPalette() {
        signIn("admin@bakery.test", "admin");
        open("/admin/dashboard");

        var fills = (List<String>) script("return [...document.querySelectorAll('vaadin-chart')]"
                + ".map(c => c.shadowRoot && c.shadowRoot.querySelector("
                + "'.highcharts-series path, .highcharts-series rect'))"
                + ".filter(Boolean).map(e => e.getAttribute('fill') || 'none');");

        assertFalse(fills.isEmpty(), "at least one series was drawn, at " + whereAmI());
        // In styled mode Highcharts colours through CSS classes and writes no
        // fill attribute. #2caffe is its own first palette colour, and it is
        // exactly what appeared here before styled mode was turned on.
        assertTrue(fills.stream().noneMatch(fill -> fill.startsWith("#")),
                "no series carries a baked in colour: " + fills);
    }

    @BrowserTest
    void theQuestionBoxHasAChartOfItsOwn() {
        signIn("admin@bakery.test", "admin");
        open("/admin/dashboard");

        assertTrue(count(".dashboard__asked") == 1L, "one chart for a question, at " + whereAmI());
    }
}
