package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.assistant.AssistantConfiguration.AssistantStatus;
import com.vaadin.bakery.assistant.AssistantPolicy;
import com.vaadin.bakery.assistant.BakeryDatabase;
import com.vaadin.bakery.assistant.ui.AskPanel;
import com.vaadin.bakery.ordering.DashboardService;
import com.vaadin.bakery.ordering.DashboardService.ProductSales;
import com.vaadin.bakery.ordering.DashboardService.RevenuePoint;
import com.vaadin.bakery.ordering.DashboardService.StateCount;
import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.ai.chart.ChartAIController;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DataLabels;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.Marker;
import com.vaadin.flow.component.charts.model.PlotOptionsSpline;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;
import jakarta.annotation.security.RolesAllowed;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * The numbers, on one screen.
 *
 * The range is a signal and a single effect rebuilds every panel from it, so
 * there is one place where the dashboard decides what it is showing. The three
 * series panels are Charts, which this build is licensed for: there is no
 * second rendering beside them and nothing here asks what the machine is
 * allowed to run. See Licensing in specs/00-overview.md.
 */
@Route("admin/dashboard")
@PageTitle("Dashboard")
@Menu(order = 25, title = "Dashboard", icon = "vaadin:chart")
@RolesAllowed({ Role.ADMIN_NAME, Role.BARISTA_NAME })
public class DashboardView extends VerticalLayout {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM");

    public enum Range {
        WEEK(7), FORTNIGHT(14), MONTH(30), QUARTER(90);

        private final int days;

        Range(int days) {
            this.days = days;
        }

        public int days() {
            return days;
        }

        public String translationKey() {
            return "dashboard.range." + name();
        }
    }

    private final DashboardService dashboard;
    private final Clock clock;
    private final ValueSignal<Range> range = new ValueSignal<>(Range.WEEK);
    private final Div panels = new Div();
    private final AskPanel ask;

    public DashboardView(DashboardService dashboard, Clock clock, AssistantStatus assistant,
            AssistantPolicy policy, BakeryDatabase database) {
        this.dashboard = dashboard;
        this.clock = clock;
        addClassName("dashboard");

        var selector = new Select<Range>();
        Translations.bind(selector, selector::setLabel, "dashboard.range");
        selector.setItems(Range.values());
        Translations.onLocale(selector, locale ->
                selector.setItemLabelGenerator(value -> getTranslation(locale, value.translationKey())));
        selector.setValue(Range.WEEK);
        selector.addValueChangeListener(event -> range.set(event.getValue()));

        panels.addClassName("dashboard__panels");

        // A question, drawn. The chart is built here rather than in a panel
        // because the panels are rebuilt on every range and language change and
        // the controller holds on to the one it was given.
        var asked = chart(ChartType.COLUMN);
        asked.addClassName("dashboard__asked");
        ask = new AskPanel(assistant, policy, "dashboard.ask", "dashboard", asked,
                () -> new ChartAIController(asked, database));

        add(Translations.bindText(new H2(), "dashboard.title"), selector, panels, ask);

        // One effect over two signals: every panel is rebuilt from the same
        // range, and again when the language changes, because the labels, the
        // month names and the amounts all follow the locale.
        Translations.onLocale(this, locale -> render(range.get(), locale));
    }

    private void render(Range selected, Locale locale) {
        panels.removeAll();
        var to = LocalDate.now(clock);
        var from = to.minusDays(selected.days());
        // The period immediately before this one, same length, so the two
        // series line up index by index on the revenue chart.
        var comparisonTo = from.minusDays(1);
        var comparisonFrom = comparisonTo.minusDays(selected.days());

        var today = dashboard.today();
        panels.add(panel(getTranslation(locale, "dashboard.today"),
                counter(getTranslation(locale, "dashboard.today.due"), String.valueOf(today.due())),
                counter(getTranslation(locale, "dashboard.today.ready"), String.valueOf(today.ready())),
                counter(getTranslation(locale, "dashboard.today.problems"), String.valueOf(today.problems())),
                counter(getTranslation(locale, "dashboard.today.next"),
                        today.nextPickup().isBlank() ? "-" : today.nextPickup())));

        panels.add(panel(getTranslation(locale, "dashboard.revenue"),
                revenueChart(dashboard.revenue(from, to), dashboard.revenue(comparisonFrom, comparisonTo), locale)));

        panels.add(panel(getTranslation(locale, "dashboard.states"),
                statesChart(dashboard.byState(from, to), locale)));

        panels.add(panel(getTranslation(locale, "dashboard.topProducts"),
                topProductsChart(dashboard.topProducts(from, to, 10), locale)));
    }

    /** Gross per day, with the period before it drawn behind as a plain spline. */
    Component revenueChart(List<RevenuePoint> current, List<RevenuePoint> comparison, Locale locale) {
        if (current.stream().mapToInt(point -> point.gross().cents()).sum() == 0) {
            return emptyState(locale);
        }
        var chart = chart(ChartType.AREASPLINE);
        var configuration = chart.getConfiguration();

        var days = new XAxis();
        days.setCategories(current.stream()
                .map(point -> point.date().format(DAY.withLocale(locale)))
                .toArray(String[]::new));
        configuration.addxAxis(days);

        var amount = new YAxis();
        amount.setTitle(getTranslation(locale, "dashboard.axis.revenue"));
        amount.setMin(0);
        configuration.addyAxis(amount);

        configuration.addSeries(euros(getTranslation(locale, "dashboard.series.current"), current));

        var behind = euros(getTranslation(locale, "dashboard.series.previous"), comparison);
        var plain = new PlotOptionsSpline();
        plain.setMarker(new Marker(false));
        behind.setPlotOptions(plain);
        configuration.addSeries(behind);
        return chart;
    }

    /** Distribution over the range. States with no orders are left out entirely. */
    Component statesChart(List<StateCount> states, Locale locale) {
        var present = states.stream().filter(state -> state.count() > 0).toList();
        if (present.isEmpty()) {
            return emptyState(locale);
        }
        var chart = chart(ChartType.PIE);
        var series = new DataSeries(getTranslation(locale, "dashboard.series.orders"));
        present.forEach(state -> series.add(
                new DataSeriesItem(getTranslation(locale, state.state().translationKey()), state.count())));
        chart.getConfiguration().addSeries(series);
        return chart;
    }

    /**
     * Top ten by units. The bar is the units and the money rides on the data
     * label, so one panel answers both halves of the question the specification
     * asks without a second axis at a second scale.
     */
    Component topProductsChart(List<ProductSales> products, Locale locale) {
        if (products.isEmpty()) {
            return emptyState(locale);
        }
        var chart = chart(ChartType.BAR);
        var configuration = chart.getConfiguration();

        var names = new XAxis();
        names.setCategories(products.stream().map(ProductSales::product).toArray(String[]::new));
        configuration.addxAxis(names);

        var units = new YAxis();
        units.setTitle(getTranslation(locale, "dashboard.axis.units"));
        units.setMin(0);
        // Headroom past the longest bar, so the money label has somewhere to
        // sit instead of straddling the bar's end.
        units.setMax(Math.ceil(products.stream().mapToInt(ProductSales::units).max().orElse(1) * 1.4));
        configuration.addyAxis(units);

        var series = new DataSeries(getTranslation(locale, "dashboard.series.units"));
        products.forEach(product -> {
            var item = new DataSeriesItem(product.product(), product.units());
            var money = new DataLabels(true);
            money.setFormat(product.gross().format(locale));
            // Ten bars in one panel put the labels close enough that Charts
            // drops every other one to avoid a collision they do not have.
            money.setAllowOverlap(true);
            item.setDataLabels(money);
            series.add(item);
        });
        configuration.addSeries(series);
        configuration.getLegend().setEnabled(false);
        return chart;
    }

    private Chart chart(ChartType type) {
        var chart = new Chart(type);
        chart.addClassName("dashboard__chart");
        var configuration = chart.getConfiguration();
        // Flow defaults a chart to the Java styling API, which bakes Highcharts'
        // own palette into fill attributes and leaves the chart white on a dark
        // page. Styled mode is what makes it read the --vaadin-charts-*
        // properties instead, and those follow the colour scheme.
        configuration.getChart().setStyledMode(true);
        // The panel already carries the heading, and the attribution belongs on
        // the about page rather than under every panel.
        configuration.setTitle("");
        configuration.disableCredits();
        return chart;
    }

    private ListSeries euros(String name, List<RevenuePoint> points) {
        var series = new ListSeries(name);
        points.forEach(point -> series.addData(point.gross().cents() / 100.0));
        return series;
    }

    private Component emptyState(Locale locale) {
        var empty = new Span(getTranslation(locale, "dashboard.empty"));
        empty.addClassName("dashboard__empty");
        return empty;
    }

    private Div counter(String label, String value) {
        var counter = new Div(new Span(value), new Span(label));
        counter.addClassName("dashboard__counter");
        return counter;
    }

    /** The question box, for the tests that assert on what it is offering. */
    public AskPanel ask() {
        return ask;
    }

    private Div panel(String title, Component... content) {
        var panel = new Div();
        panel.addClassNames("panel", "dashboard__panel");
        panel.add(new H3(title));
        panel.add(content);
        return panel;
    }
}
