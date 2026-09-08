package com.example;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel.SelectAllCheckboxVisibility;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The same grid twice. Same selection mode, same request for a select all
 * checkbox, and the only difference is where the rows come from.
 *
 * The header of the in memory one has a checkbox. The header of the lazy one
 * has nothing, and asking for it is accepted without complaint.
 */
@Route("")
@AnonymousAllowed
public class TwoGridsView extends VerticalLayout {

    private static final List<String> NAMES = IntStream.rangeClosed(1, 500)
            .mapToObj(i -> "Row " + i).toList();

    public TwoGridsView() {
        add(new H3("In memory"), report(inMemory()),
                new H3("Lazy"), report(lazy()),
                new H3("Lazy, with the visibility left at its default"), report(lazyDefault()));
        setWidthFull();
    }

    /** The grid, and a line saying how many rows the server thinks are selected. */
    private VerticalLayout report(Grid<String> grid) {
        var count = new com.vaadin.flow.component.html.Span("selected: 0");
        grid.addSelectionListener(event -> count.setText("selected: " + event.getAllSelectedItems().size()
                + " of " + NAMES.size()));
        var block = new VerticalLayout(grid, count);
        block.setPadding(false);
        return block;
    }

    private Grid<String> inMemory() {
        var grid = multiSelect();
        grid.setItems(NAMES);
        return grid;
    }

    private Grid<String> lazy() {
        var grid = multiSelect();
        grid.setItems(query -> NAMES.stream().skip(query.getOffset()).limit(query.getLimit()),
                query -> NAMES.size());
        return grid;
    }

    private Grid<String> lazyDefault() {
        var grid = new Grid<String>();
        grid.addColumn(name -> name).setHeader("Name");
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.setItems(query -> NAMES.stream().skip(query.getOffset()).limit(query.getLimit()),
                query -> NAMES.size());
        grid.setHeight("200px");
        return grid;
    }

    private Grid<String> multiSelect() {
        var grid = new Grid<String>();
        grid.addColumn(name -> name).setHeader("Name");
        var selection = (com.vaadin.flow.component.grid.GridMultiSelectionModel<String>) grid
                .setSelectionMode(Grid.SelectionMode.MULTI);
        // Accepted in both cases, honoured in one.
        selection.setSelectAllCheckboxVisibility(SelectAllCheckboxVisibility.VISIBLE);
        grid.setAllRowsVisible(false);
        grid.setHeight("200px");
        return grid;
    }
}
