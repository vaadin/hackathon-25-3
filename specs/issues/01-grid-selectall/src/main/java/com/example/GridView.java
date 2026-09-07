package com.example;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.stream.IntStream;

/**
 * One grid, multi select, lazily populated. That is the whole reproduction.
 *
 * The selection column's header gets `<span class="sr-only">Select All
 * unavailable</span>`, nothing inside the grid's shadow root styles `.sr-only`,
 * and the sentence is both painted and used to size the column.
 *
 * Open http://localhost:8080 and look at the first column, or read the width:
 *
 *   document.querySelector('vaadin-grid').shadowRoot
 *       .querySelector('thead th').getBoundingClientRect().width
 */
@Route("")
public class GridView extends VerticalLayout {

    public record Person(int id, String name) {
    }

    public GridView() {
        setSizeFull();

        var grid = new Grid<Person>();
        grid.addColumn(Person::id).setHeader("Id");
        grid.addColumn(Person::name).setHeader("Name");
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        // Lazy, which is the condition. An in memory grid offers a working
        // select all and never renders the sentence.
        grid.setItemsPageable(pageable -> page(pageable.getPageNumber(), pageable.getPageSize()),
                pageable -> 500);

        add(grid);
    }

    private static List<Person> page(int number, int size) {
        return IntStream.range(number * size, number * size + size)
                .mapToObj(i -> new Person(i, "Person " + i))
                .toList();
    }
}
