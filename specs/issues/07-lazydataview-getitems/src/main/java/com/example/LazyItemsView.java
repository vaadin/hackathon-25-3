package com.example;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The documented way to fill a lazy grid, and the documented way to read one.
 *
 * Open http://localhost:8097 and press "Read the items". It throws:
 *
 *   java.lang.ArithmeticException: / by zero
 *
 * `AbstractLazyDataView.getItems` builds a `Query` with no page size,
 * `VaadinSpringDataHelpers.toSpringPageRequest` calls `query.getPage()`, and
 * `Query.getPage()` divides by a page size of zero.
 *
 * These are the two most common things to do with a grid in a Spring
 * application, and they do not work together.
 */
@Route("")
public class LazyItemsView extends VerticalLayout {

    public record Person(int id, String name) {
    }

    public LazyItemsView() {
        var grid = new Grid<Person>();
        grid.addColumn(Person::id).setHeader("Id");
        grid.addColumn(Person::name).setHeader("Name");
        grid.setItemsPageable(pageable -> page(pageable.getPageNumber(), pageable.getPageSize()),
                pageable -> 500);

        var result = new Paragraph();
        add(grid, new Button("Read the items", event ->
                result.setText("counted " + grid.getLazyDataView().getItems().count())), result);
    }

    private static List<Person> page(int number, int size) {
        return IntStream.range(number * size, number * size + size)
                .mapToObj(i -> new Person(i, "Person " + i))
                .toList();
    }
}
