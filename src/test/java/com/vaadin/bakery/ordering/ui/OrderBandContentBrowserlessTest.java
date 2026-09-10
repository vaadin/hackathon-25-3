package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.Channel;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * BOARD-05 and BOARD-09. What an expanded row says, and what selecting one does
 * not do.
 *
 * The band is what a barista reads before they answer a question on the
 * telephone, so it carries the two things that are not in the table: what the
 * customer cannot eat, and what has happened to the order lately. And expanding
 * a row must not change the selection, because the selection is what the bulk
 * actions will act on: reading one order while three others are ticked is
 * exactly the case that goes wrong quietly.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class OrderBandContentBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private OrderRepository orders;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    @SuppressWarnings("unchecked")
    private Grid<Order> board() {
        return (Grid<Order>) find(Grid.class).single();
    }

    private Order anOrderWithAllergens() {
        return orders.findAll().stream()
                .filter(order -> !orderService.detailLines(order.getReference()).stream()
                        .flatMap(line -> line.allergenKeys().stream())
                        .toList().isEmpty())
                .findFirst()
                .orElseThrow();
    }

    @Test
    void anExpandedRowCarriesAllergensAndTheLastTwoThingsThatHappened() {
        navigate(OrderBoardView.class);
        var order = anOrderWithAllergens();

        board().setDetailsVisible(order, true);

        var allergens = find(Div.class).withClassName("order-board__band-allergens").all();
        assertFalse(allergens.isEmpty(), "the band lists what is in the order");
        assertFalse(allergens.getFirst().getChildren().toList().isEmpty(),
                "and it lists at least one allergen");

        // The platform's own component, not a Span wearing theme="badge". That
        // attribute is a Lumo convention: Aura has no rule for it, so the same
        // chips came out coloured under one theme and as bare words under the
        // other. A component both themes know is the whole fix.
        assertTrue(allergens.getFirst().getChildren()
                        .allMatch(com.vaadin.flow.component.badge.Badge.class::isInstance),
                "every chip is a Badge: " + allergens.getFirst().getChildren()
                        .map(child -> child.getClass().getSimpleName()).toList());

        var history = find(Div.class).withClassName("order-board__band-history").all();
        assertFalse(history.isEmpty(), "the band carries the recent history");
        long entries = history.getFirst().getChildren().count();
        assertTrue(entries >= 1 && entries <= 2,
                "the last two entries and no more, found " + entries);
    }

    /**
     * Details are independent of selection in 25.3, which is the whole reason
     * the band can be opened while a bulk action is being prepared.
     */
    @Test
    void expandingARowLeavesTheSelectionAlone() {
        navigate(OrderBoardView.class);
        var order = anOrderWithAllergens();

        board().setDetailsVisible(order, true);

        assertTrue(board().getSelectedItems().isEmpty(),
                "reading an order did not tick it: " + board().getSelectedItems());

        // And the other way round: ticking one does not open it.
        board().select(order);
        var another = orders.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(order.getId()))
                .findFirst()
                .orElseThrow();
        board().select(another);
        assertFalse(board().isDetailsVisible(another), "and ticking one did not open it either");
    }

    /** BOARD-22. The channel a customer's own order is recorded under. */
    @Test
    void anOrderPlacedByACustomerIsRecordedAsOnline() {
        var online = orders.findAll().stream()
                .filter(order -> order.getChannel() == Channel.ONLINE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the dataset has no online order"));

        assertTrue(online.getCreatedBy() == null,
                "an online order has no member of staff behind it: " + online.getCreatedBy());
    }
}
