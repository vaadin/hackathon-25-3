package com.example;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;

/**
 * Four components a browser shows and a browserless test cannot find. Each one
 * is handed to another component instead of being added to the view.
 */
@Route("")
@AnonymousAllowed
public class FindView extends VerticalLayout {

    public record Person(String name, boolean active) {
    }

    public FindView() {
        var grid = new Grid<Person>();
        grid.setItems(List.of(new Person("Ada", true), new Person("Grace", false)));

        // 1. a component rendered into a cell
        grid.addComponentColumn(person -> new Checkbox(person.active()));

        // 2. a component set as a column header
        var byName = grid.addColumn(Person::name);
        byName.setHeader(new Button("Sort"));

        // 3. an item in the grid's own context menu
        grid.addContextMenu().addItem(new Checkbox("Show inactive"));

        // 4. an ordinary child, for comparison: this one is found
        add(new Checkbox("A checkbox in the view"), grid);
    }
}
