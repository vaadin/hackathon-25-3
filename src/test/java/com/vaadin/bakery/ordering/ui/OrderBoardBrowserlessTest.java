package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

/** BOARD-01, BOARD-02 and BOARD-04. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "barista@bakery.test", roles = { "BARISTA" })
class OrderBoardBrowserlessTest extends SpringBrowserlessTest {

    @org.junit.jupiter.api.BeforeEach
    void signIn() {
        // After the browserless environment is up, see TestLogin for why.
        TestLogin.asBarista();
    }

    @Autowired
    private OrderRepository orders;

    @Autowired
    private OrderService orderService;

    @SuppressWarnings("unchecked")
    private Grid<Order> grid() {
        return (Grid<Order>) find(Grid.class).single();
    }

    /**
     * Not getLazyDataView().getItems(): with a pageable backed grid that path
     * throws / by zero in 25.3.0-beta1, see specs/FEEDBACK-25.3.md. Fetching
     * through the provider with an explicit query does the same job.
     */
    private List<Order> rows() {
        return grid().getDataProvider()
                .fetch(new com.vaadin.flow.data.provider.Query<>(0, 100, java.util.List.of(), null, null))
                .toList();
    }

    @Test
    void theBoardShowsUpcomingOrdersSortedBySlot() {
        navigate(OrderBoardView.class);
        var rows = rows();

        assertFalse(rows.isEmpty());
        for (int i = 1; i < rows.size(); i++) {
            var previous = rows.get(i - 1).pickupAt();
            var current = rows.get(i).pickupAt();
            assertTrue(!current.isBefore(previous), "rows are ordered by pickup time");
        }
    }

    @Test
    void searchingByCustomerNameNarrowsTheBoard() {
        var target = orders.findAll().getFirst();
        var surname = target.getCustomer().getLastName();
        navigate(OrderBoardView.class);

        test(find(TextField.class).single()).setValue(surname);

        var rows = rows();
        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().allMatch(order ->
                order.getCustomer().getLastName().equalsIgnoreCase(surname)
                        || order.getCustomer().getFirstName().toLowerCase().contains(surname.toLowerCase())
                        || order.getReference().toLowerCase().contains(surname.toLowerCase())),
                "everything left matches the term");
    }

    @Test
    void searchingByReferenceFindsExactlyOneOrder() {
        var target = orders.findAll().getFirst();
        navigate(OrderBoardView.class);

        test(find(TextField.class).single()).setValue(target.getReference());

        assertEquals(1, rows().size());
        assertEquals(target.getReference(), rows().getFirst().getReference());
    }

    @Test
    void expandingARowDoesNotChangeTheSelection() {
        navigate(OrderBoardView.class);
        var rows = rows();
        var selected = rows.get(0);
        var expanded = rows.get(1);

        grid().select(selected);
        grid().setDetailsVisible(expanded, true);

        assertTrue(grid().isDetailsVisible(expanded), "the second row is open");
        assertEquals(1, grid().getSelectedItems().size());
        assertTrue(grid().getSelectedItems().contains(selected), "and the first row is still the selected one");
        assertFalse(grid().isDetailsVisible(selected), "selecting did not open anything");
    }

    /** BOARD-12. The band leads with quantities and still carries allergens and history. */
    @Test
    void theExpandedRowShowsATilePerLine() {
        navigate(OrderBoardView.class);
        var order = orders.findAll().stream()
                .filter(candidate -> !orderService.detailLines(candidate.getReference()).isEmpty())
                .findFirst()
                .orElseThrow();

        grid().setDetailsVisible(order, true);

        var band = find(com.vaadin.flow.component.html.Div.class).all().stream()
                .filter(div -> div.getClassNames().contains("order-board__band"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the expanded row has no band"));
        var tiles = band.getChildren()
                .flatMap(child -> child.getChildren())
                .filter(child -> child.getElement().getClassList().contains("order-board__tile"))
                .toList();

        assertEquals(orderService.detailLines(order.getReference()).size(), tiles.size(),
                "one tile per line");
        var quantities = tiles.stream()
                .map(tile -> tile.getChildren().findFirst().orElseThrow().getElement().getText())
                .toList();
        assertTrue(quantities.stream().allMatch(text -> text.matches("\\d+")),
                "each tile leads with its quantity, got " + quantities);
    }
}
