package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/** BOARD-10 and BOARD-11. The menu is the full inventory of columns, and it cannot empty the table. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "barista@bakery.test", roles = { "BARISTA" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ColumnVisibilityBrowserlessTest extends SpringBrowserlessTest {

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    @SuppressWarnings("unchecked")
    private Grid<Order> grid() {
        return (Grid<Order>) find(Grid.class).single();
    }

    /**
     * The toggles are reached through the grid's own context menu, which is
     * where the chooser lives: there is no column and no header component any
     * more, because a grid offers a menu over the whole header, the select all
     * cell included, and that is the only way to put one there. A menu's items
     * are outside the tree the finder walks, so {@code find(Checkbox.class)}
     * finds none of these, and they are not attached either until a client
     * opens the menu, which a browserless test has no way to do. So the toggles
     * are driven by their value rather than by {@code test(box).click()}: it is
     * the same listener and the same code path, and the alternative is not
     * covering the chooser at all. See {@code specs/FEEDBACK-25.3.md}.
     */
    private List<Checkbox> toggles() {
        return find(OrderBoardView.class).single().chooserMenu().getItems().stream()
                .flatMap(item -> item.getChildren())
                .filter(Checkbox.class::isInstance)
                .map(Checkbox.class::cast)
                .toList();
    }

    @Test
    void everyColumnIsListedWithItsCurrentState() {
        navigate(OrderBoardView.class);

        var labels = toggles().stream().map(Checkbox::getLabel).toList();
        assertEquals(List.of("Reference", "Customer", "Pickup", "State", "Items", "Channel", "Total"), labels,
                "the menu lists every column, in table order");

        var byLabel = toggles().stream()
                .collect(java.util.stream.Collectors.toMap(Checkbox::getLabel, box -> box));
        assertTrue(byLabel.get("Reference").getValue(), "reference is on to begin with");
        assertFalse(byLabel.get("Items").getValue(), "the expensive column is off to begin with");
    }

    @Test
    void turningAColumnOnChangesTheTable() {
        navigate(OrderBoardView.class);
        var items = toggles().stream().filter(box -> "Items".equals(box.getLabel())).findFirst().orElseThrow();
        var column = grid().getColumns().stream().filter(c -> "items".equals(c.getKey())).findFirst().orElseThrow();

        assertFalse(column.isVisible(), "off before");
        items.setValue(true);
        assertTrue(column.isVisible(), "and on after");
    }

    @Test
    void theLastVisibleColumnCannotBeTurnedOff() {
        navigate(OrderBoardView.class);
        var on = toggles().stream().filter(Checkbox::getValue).toList();
        // Turn them all off but the last one, which must refuse.
        on.subList(0, on.size() - 1).forEach(box -> box.setValue(false));
        var last = on.getLast();

        last.setValue(false);

        assertTrue(last.getValue(), "the toggle springs back");
        // The edit column is the grid's own furniture rather than one of the
        // columns the chooser lists, so it is not counted here.
        assertEquals(1, grid().getColumns().stream()
                        .filter(column -> !"edit".equals(column.getKey()))
                        .filter(Grid.Column::isVisible).count(),
                "and the table still has a column");
    }
}
