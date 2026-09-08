package com.example;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;
import java.util.stream.IntStream;

/**
 * One grid, alone on the page: lazy, multi select, and the select all
 * visibility left at its default, which means the framework has decided this
 * grid does not support select all.
 *
 * The checkbox is rendered in the header anyway. Click it: it ticks, and the
 * selection stays empty.
 */
@Route("only-default")
@AnonymousAllowed
public class OnlyDefaultView extends VerticalLayout {

    private static final List<String> NAMES = IntStream.rangeClosed(1, 500)
            .mapToObj(i -> "Row " + i).toList();

    public OnlyDefaultView() {
        var grid = new Grid<String>();
        grid.addColumn(name -> name).setHeader("Name");
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.setItems(query -> NAMES.stream().skip(query.getOffset()).limit(query.getLimit()),
                query -> NAMES.size());
        grid.setHeight("200px");

        var count = new Span("selected: 0");
        grid.addSelectionListener(event -> count.setText("selected: " + event.getAllSelectedItems().size()));

        add(new H3("Lazy, multi select, default select all visibility"),
                new Paragraph("Nothing else is on this page."), grid, count);
    }
}
