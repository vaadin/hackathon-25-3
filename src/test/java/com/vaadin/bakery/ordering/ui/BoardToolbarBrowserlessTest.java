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
     * The select all checkbox is offered, and it works.
     *
     * This test used to assert the opposite, that the checkbox was not offered
     * and the sentence explaining its absence was emptied. Both halves were
     * wrong: the component renders the checkbox whatever the visibility says,
     * so hiding it only made it inert, and the sentence is never written while
     * a checkbox is there. Reduced in
     * specs/issues/20-grid-select-all-lazy.md.
     */
    @Test
    void theSelectionHeaderOffersSelectAllAndSaysSoInTheReadersLanguage() {
        navigate(OrderBoardView.class);

        var i18n = grid().getI18n();
        var selection = (GridMultiSelectionModel<Order>) grid().getSelectionModel();

        assertTrue(selection.isSelectAllCheckboxVisible(), "the checkbox is offered");
        assertEquals("Select all is not available here", i18n.getSelectAllUnavailable(),
                "and the sentence that replaces it is translated, for the day it is used");
    }

    /** Select all means every order the filter matches, not the rows on screen. */
    @Test
    void selectAllReachesTheWholeFilterAndNotJustTheLoadedRows() {
        navigate(OrderBoardView.class);
        var selection = (GridMultiSelectionModel<Order>) grid().getSelectionModel();

        selection.selectAll();

        // Against the board's own count rather than a query written twice: the
        // filter is "not in the past" and duplicating it here would be a test
        // that agrees with itself.
        var listed = grid().getLazyDataView().getItems().count();
        assertTrue(selection.getSelectedItems().size() > 40,
                "more than one page, it selected " + selection.getSelectedItems().size());
        assertEquals(listed, (long) selection.getSelectedItems().size(),
                "every order the board lists");
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
     * The chooser is inside the table, and it is the grid's own context menu.
     *
     * It was a column of its own with a menu bar in its header, which is what
     * this test asserted. There is no such column now: the chooser has to be
     * reachable from the header, including the select all cell, and a context
     * menu is the only thing a grid offers that reaches it. What makes it "the
     * table's shape rather than a row's actions" is the dynamic content
     * handler, which answers only when the menu was opened on no row at all.
     */
    @Test
    void theColumnChooserBelongsToTheGrid() {
        navigate(OrderBoardView.class);
        var menu = find(OrderBoardView.class).single().chooserMenu();

        assertEquals(grid(), menu.getTarget(), "the menu belongs to the table itself");
        assertEquals(7, menu.getItems().size(), "one entry per column it can hide");
        assertTrue(grid().getColumns().stream().noneMatch(column -> "chooser".equals(column.getKey())),
                "and no column is spent on it");
    }
}
