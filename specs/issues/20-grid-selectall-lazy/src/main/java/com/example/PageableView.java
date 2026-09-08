package com.example;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridMultiSelectionModel.SelectAllCheckboxVisibility;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The same thing through setItemsPageable, which is the call a Spring Data
 * backed board makes. Included because the first version of this report was
 * measured on this path and not on setItems.
 */
@Route("pageable")
@AnonymousAllowed
public class PageableView extends VerticalLayout {

    private static final List<String> NAMES = IntStream.rangeClosed(1, 500)
            .mapToObj(i -> "Row " + i).toList();

    public PageableView() {
        var grid = new Grid<String>();
        grid.addColumn(name -> name).setHeader("Name");
        var selection = (GridMultiSelectionModel<String>) grid.setSelectionMode(Grid.SelectionMode.MULTI);
        selection.setSelectAllCheckboxVisibility(SelectAllCheckboxVisibility.VISIBLE);
        grid.setItemsPageable(
                pageable -> NAMES.stream()
                        .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                        .limit(pageable.getPageSize()).toList(),
                pageable -> NAMES.size());
        grid.setHeight("200px");

        var count = new Span("selected: 0");
        grid.addSelectionListener(event -> count.setText("selected: " + event.getAllSelectedItems().size()));

        add(new H3("setItemsPageable, multi select, VISIBLE"), grid, count);
    }
}
