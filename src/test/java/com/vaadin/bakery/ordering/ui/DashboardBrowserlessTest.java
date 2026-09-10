package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.DashboardService;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.html.Span;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** DASH-01, DASH-02, DASH-03 and DASH-05. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class DashboardBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private DashboardService dashboard;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private Clock clock;

    @BeforeEach
    void signIn() {
        TestLogin.asAdmin();
    }

    @Test
    void everyPanelIsRendered() {
        navigate(DashboardView.class);

        // The panels are Dashboard widgets now, and a widget's title is a
        // property of the widget rather than a heading inside it. The first one
        // is the exception and says so: the banner carries its own heading
        // inside itself, beside the date, so its widget title is empty.
        var widgets = find(DashboardWidget.class).all();
        var titles = widgets.stream().map(DashboardWidget::getTitle).toList();
        assertTrue(titles.contains("This period"), titles.toString());
        assertTrue(titles.contains("Revenue by day"), titles.toString());
        assertTrue(titles.contains("Orders by state"), titles.toString());
        assertTrue(titles.contains("Busiest pickup hours"), titles.toString());
        assertTrue(titles.contains("Top products"), titles.toString());
    }

    /**
     * POL2-11. The first widget is the banner: the width of the page, its own
     * surface, and today's figures on it. It was one panel among five, the same
     * size as the pie chart beside it, and it is the block somebody opens this
     * page to read.
     */
    @Test
    void theFirstWidgetIsTheBanner() {
        navigate(DashboardView.class);

        var first = find(DashboardWidget.class).all().getFirst();
        assertTrue(first.getClassNames().contains("dashboard__banner-widget"),
                "the first widget is the banner");
        assertEquals(4, first.getColspan(), "and it takes the width of the page");
        assertTrue(first.getTitle() == null || first.getTitle().isBlank(),
                "its heading is inside it, beside the date");

        var banner = find(com.vaadin.flow.component.html.Div.class)
                .withClassName("dashboard__banner").single();
        assertTrue(banner.getElement().getTextRecursively().contains("Today"),
                "which is where the word is");
        assertTrue(banner.getElement().getTextRecursively().contains("Takings today"),
                "and today's takings are on it, which no panel showed before");
    }

    /**
     * POL2-11, the other half. The charts show the shape of a range and nothing
     * on the page said its size.
     */
    @Test
    void theRangeHasTotalsOfItsOwn() {
        var to = LocalDate.now(clock);
        var period = dashboard.period(to.minusDays(7), to);

        assertTrue(period.orders() > 0, "the seeded week has orders in it");
        assertEquals(period.gross().cents() / period.orders(), period.average().cents(),
                "the average is the takings over the count");
        assertTrue(period.byHour().stream().mapToLong(hour -> hour.orders()).sum() == period.orders(),
                "and every order is in exactly one hour of the day");
    }

    @Test
    void theThreeSeriesPanelsAreCharts() {
        navigate(DashboardView.class);

        // The fourth is the one a question draws into, which starts empty and
        // belongs to the assistant rather than to a panel.
        var charts = find(Chart.class).all().stream()
                .filter(chart -> !chart.getClassNames().contains("dashboard__asked"))
                .toList();
        assertEquals(4, charts.size(),
                "revenue, orders by state, the busiest hours and top products");

        var types = charts.stream()
                .map(chart -> chart.getConfiguration().getChart().getType())
                .toList();
        assertTrue(types.contains(ChartType.AREASPLINE), types.toString());
        assertTrue(types.contains(ChartType.PIE), types.toString());
        assertTrue(types.contains(ChartType.BAR), types.toString());
        assertTrue(types.contains(ChartType.COLUMN), types.toString());
    }

    @Test
    void theRevenueChartCarriesThePeriodBeforeItForComparison() {
        navigate(DashboardView.class);

        var revenue = find(Chart.class).all().stream()
                .filter(chart -> chart.getConfiguration().getChart().getType() == ChartType.AREASPLINE)
                .findFirst()
                .orElseThrow();
        var series = revenue.getConfiguration().getSeries();

        assertEquals(2, series.size(), "this period and the one before it");
        assertEquals("This period", series.get(0).getName());
        assertEquals("Previous period", series.get(1).getName());
    }

    @Test
    void aPanelWithNothingToPlotSaysSoRatherThanDrawingEmptyAxes() {
        navigate(DashboardView.class);
        var view = find(DashboardView.class).single();

        // Five years back there is nothing, which is the branch DASH-03 is
        // about: an empty range must produce a sentence, not a chart frame.
        var longAgo = LocalDate.now(clock).minusYears(5);
        var revenue = dashboard.revenue(longAgo, longAgo.plusDays(3));

        var revenuePanel = view.revenueChart(revenue, revenue, Locale.ENGLISH);
        var statesPanel = view.statesChart(dashboard.byState(longAgo, longAgo.plusDays(3)), Locale.ENGLISH);
        var productsPanel = view.topProductsChart(dashboard.topProducts(longAgo, longAgo.plusDays(3), 10),
                Locale.ENGLISH);

        for (var panel : java.util.List.of(revenuePanel, statesPanel, productsPanel)) {
            assertInstanceOf(Span.class, panel, "an empty range renders a sentence");
            assertEquals("Nothing in this range", ((Span) panel).getText());
        }
    }

    @Test
    void todaysCountersMatchADirectQuery() {
        var today = LocalDate.now(clock);
        long dueToday = orders.findByPickupDateBetweenAndStateInOrderByPickupDateAscPickupTimeAsc(today, today,
                java.util.List.of(OrderState.NEW, OrderState.CONFIRMED, OrderState.IN_PREPARATION,
                        OrderState.READY, OrderState.PICKED_UP, OrderState.PROBLEM)).size();

        assertEquals(dueToday, dashboard.today().due());
    }

    @Test
    void changingTheRangeChangesWhatIsCounted() {
        var to = LocalDate.now(clock);
        var week = dashboard.revenue(to.minusDays(7), to);
        var quarter = dashboard.revenue(to.minusDays(90), to);

        assertEquals(8, week.size(), "seven days plus today");
        assertEquals(91, quarter.size());
        assertTrue(quarter.stream().mapToInt(point -> point.gross().cents()).sum()
                >= week.stream().mapToInt(point -> point.gross().cents()).sum(),
                "a longer range cannot earn less");
    }

    @Test
    void aRangeWithNoOrdersRendersAnEmptyStateRatherThanFailing() {
        var longAgo = LocalDate.now(clock).minusYears(5);

        var revenue = dashboard.revenue(longAgo, longAgo.plusDays(3));

        assertEquals(4, revenue.size(), "the days exist");
        assertTrue(revenue.stream().allMatch(point -> point.gross().cents() == 0), "and they are all zero");
    }
}
