package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.menubar.MenuBar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

/**
 * FIX-08. The board's toolbar and its selection column.
 *
 * The report was four separate complaints and they turned out to be one: the
 * grid pages its rows from the database, so select all cannot work, and the
 * grid says so in a header that took 199 pixels of the table to say it. What is
 * left is a selection column the width of a checkbox, two actions that say what
 * they act on by being dark until it exists, and a column chooser in the
 * table's own header rather than floating above it.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "barista@bakery.test", roles = { "BARISTA" })
class BoardToolbarBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private OrderRepository orders;

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    @SuppressWarnings("unchecked")
    private Grid<Order> grid() {
        return (Grid<Order>) find(Grid.class).single();
    }

    private Button confirm() {
        return find(Button.class).withAriaLabel("Confirm selected").single();
    }

    /**
     * Nothing to explain, so nothing to say. The grid only writes the sentence
     * when this is left at its default, and it writes it into the table.
     */
    @Test
    void theSelectionHeaderCarriesNoUnexplainedSentence() {
        navigate(OrderBoardView.class);

        var i18n = grid().getI18n();

        assertEquals("", i18n.getSelectAllUnavailable(),
                "the header says nothing, because there is no select all to explain");
        assertFalse(((GridMultiSelectionModel<Order>) grid().getSelectionModel()).isSelectAllCheckboxVisible(),
                "and the checkbox that cannot work is not offered");
    }

    /** A verb nobody can read is not a label, so the label is the accessible name. */
    @Test
    void theSelectionActionsAreNamedEvenThoughTheyAreIcons() {
        navigate(OrderBoardView.class);

        var button = confirm();

        assertTrue(button.getText().isBlank(), "an icon, not a word");
        assertEquals("Confirm selected", button.getAriaLabel().orElse(""));
        assertEquals("Confirm selected", button.getTooltip().getText());
    }

    /** Dark until there is something to act on. */
    @Test
    void theSelectionActionsWakeUpWithTheSelection() {
        navigate(OrderBoardView.class);
        assertFalse(confirm().isEnabled(), "nothing is selected to begin with");

        var order = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();
        grid().select(order);
        assertTrue(confirm().isEnabled(), "and now there is");

        grid().deselectAll();
        assertFalse(confirm().isEnabled(), "and now there is not again");
    }

    /**
     * The chooser is inside the table. It is reached through the grid rather
     * than through the view, which is the whole point of moving it.
     */
    @Test
    void theColumnChooserBelongsToTheGrid() {
        navigate(OrderBoardView.class);

        var chooser = grid().getColumns().stream()
                .filter(column -> "chooser".equals(column.getKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the grid has a column for the chooser"));

        assertTrue(chooser.getHeaderComponent() instanceof MenuBar,
                "and its header is the menu, not a button above the table");
        assertTrue(chooser.isFrozenToEnd(), "kept at the end where settings are looked for");
    }
}
