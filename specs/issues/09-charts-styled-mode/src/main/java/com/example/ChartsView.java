package com.example;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.router.Route;

/**
 * Two identical charts on a page in the dark colour scheme. One line of
 * difference: the right one turns styled mode on.
 *
 * Open http://localhost:8099. The left chart draws Highcharts' own palette on a
 * white plot area, on a dark page. The right one follows the theme.
 *
 * The `--vaadin-charts-*` style properties are inert without styled mode: the
 * component's own stylesheet guards every one of them behind
 * `:where([styled-mode])`, and Flow does not set it. So the properties a reader
 * finds documented do nothing until they find a different page that mentions
 * the switch.
 */
@Route("")
public class ChartsView extends VerticalLayout {

    public ChartsView() {
        setSizeFull();

        add(new H3("The same chart twice, on a dark page"));
        add(new Paragraph("Left: as Flow builds it. Right: with setStyledMode(true)."));

        add(sideBySide());
    }

    private HorizontalLayout sideBySide() {
        var row = new HorizontalLayout(labelled("Default", chart(false)),
                labelled("setStyledMode(true)", chart(true)));
        row.setSizeFull();
        return row;
    }

    private VerticalLayout labelled(String label, Chart chart) {
        var column = new VerticalLayout(new H3(label), chart);
        column.setSizeFull();
        return column;
    }

    private Chart chart(boolean styled) {
        var chart = new Chart(ChartType.COLUMN);
        var configuration = chart.getConfiguration();
        configuration.setTitle("Revenue");
        configuration.addSeries(new ListSeries("This period", 4, 7, 3, 8, 6));
        configuration.addSeries(new ListSeries("Previous period", 3, 5, 4, 6, 5));
        configuration.getChart().setStyledMode(styled);
        chart.setSizeFull();
        return chart;
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getUI().getPage().setColorScheme(ColorScheme.Value.DARK);
    }
}
