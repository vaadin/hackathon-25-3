package com.example;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A pageable grid, and the five argument Query every example passes.
 *
 * Open http://localhost:8096 and press "Fetch with a null sort". It throws:
 *
 *   java.lang.NullPointerException: Cannot invoke "java.util.List.stream()"
 *   because the return value of "com.vaadin.flow.data.provider.Query.getSortOrders()" is null
 *       at com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringDataSort(...)
 *       at com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest(...)
 *
 * `Query` accepts the null happily. Nothing fails until the helper streams it,
 * and the exception names a helper the caller never mentioned.
 *
 * The second button passes `List.of()` and works, which is the whole difference.
 */
@Route("")
public class QueryView extends VerticalLayout {

    public record Person(int id, String name) {
    }

    public QueryView() {
        var grid = new Grid<Person>();
        grid.addColumn(Person::id).setHeader("Id");
        grid.addColumn(Person::name).setHeader("Name");
        grid.setItemsPageable(pageable -> page(pageable.getPageNumber(), pageable.getPageSize()),
                pageable -> 500);

        var result = new Paragraph();

        add(grid,
                new Button("Fetch with a null sort", event -> result.setText(
                        "fetched " + grid.getDataProvider()
                                .fetch(new Query<>(0, 50, null, null, null)).count())),
                new Button("Fetch with an empty sort list", event -> result.setText(
                        "fetched " + grid.getDataProvider()
                                .fetch(new Query<>(0, 50, List.of(), null, null)).count())),
                result);
    }

    private static List<Person> page(int number, int size) {
        return IntStream.range(number * size, number * size + size)
                .mapToObj(i -> new Person(i, "Person " + i))
                .toList();
    }
}
