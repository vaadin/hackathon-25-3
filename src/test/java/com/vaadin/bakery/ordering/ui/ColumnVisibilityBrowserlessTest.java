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
import com.vaadin.flow.component.menubar.MenuBar;
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
     * The toggles are reached through the column that hosts the menu. A
     * component set as a grid column header is outside the tree the finder
     * walks, so {@code find(Checkbox.class)} returns nothing since the chooser
     * moved into the table. See {@code specs/FEEDBACK-25.3.md}.
     */
    private List<Checkbox> toggles() {
        var menu = (MenuBar) grid().getColumns().stream()
                .filter(column -> "chooser".equals(column.getKey()))
                .findFirst()
                .orElseThrow()
                .getHeaderComponent();
        return menu.getItems().getFirst().getSubMenu().getItems().stream()
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
        test(items).click();
        assertTrue(column.isVisible(), "and on after");
    }

    @Test
    void theLastVisibleColumnCannotBeTurnedOff() {
        navigate(OrderBoardView.class);
        var on = toggles().stream().filter(Checkbox::getValue).toList();
        // Turn them all off but the last one, which must refuse.
        on.subList(0, on.size() - 1).forEach(box -> test(box).click());
        var last = on.getLast();

        test(last).click();

        assertTrue(last.getValue(), "the toggle springs back");
        // The chooser lives in a column of its own at the end of the table, and
        // it is not one of the columns it lists, so it is not counted here.
        assertEquals(1, grid().getColumns().stream()
                        .filter(column -> !"chooser".equals(column.getKey()))
                        .filter(Grid.Column::isVisible).count(),
                "and the table still has a column");
    }
}
