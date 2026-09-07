package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/** BOARD-06. A bulk action has to say how many worked and name what did not. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "barista@bakery.test", roles = { "BARISTA" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BulkActionsBrowserlessTest extends SpringBrowserlessTest {

    @org.junit.jupiter.api.BeforeEach
    void signIn() {
        // After the browserless environment is up, see TestLogin for why.
        TestLogin.asBarista();
    }

    @Autowired
    private OrderRepository orders;

    @Autowired
    private com.vaadin.bakery.ordering.OrderService orderService;

    @SuppressWarnings("unchecked")
    private Grid<Order> grid() {
        return (Grid<Order>) find(Grid.class).single();
    }

    @Test
    void confirmingSeveralNewOrdersAtOnce() {
        navigate(OrderBoardView.class);
        var selectable = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.NEW)
                .limit(3)
                .toList();
        assertEquals(3, selectable.size(), "the dataset should offer three new orders");

        selectable.forEach(order -> grid().select(order));
        test(find(Button.class).withAriaLabel("Confirm selected").single()).click();

        selectable.forEach(order -> assertEquals(OrderState.CONFIRMED,
                orders.findById(order.getId()).orElseThrow().getState()));
    }

    @Test
    void anOrderThatCannotMoveIsNamedRatherThanSilentlySkipped() {
        navigate(OrderBoardView.class);
        var pickedUp = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.PICKED_UP)
                .findFirst()
                .orElseThrow();
        var newOrder = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();

        grid().select(pickedUp);
        grid().select(newOrder);
        test(find(Button.class).withAriaLabel("Confirm selected").single()).click();

        assertEquals(OrderState.CONFIRMED, orders.findById(newOrder.getId()).orElseThrow().getState(),
                "the one that could move, moved");
        assertEquals(OrderState.PICKED_UP, orders.findById(pickedUp.getId()).orElseThrow().getState(),
                "and the one that could not is untouched");
        var messages = find(com.vaadin.flow.component.notification.Notification.class).all().stream()
                .map(notification -> test(notification).getText())
                .toList();
        assertTrue(messages.stream().anyMatch(text -> text.contains(pickedUp.getReference())),
                "the refusal names the order, got " + messages);
    }

    @Test
    void everyChangeWritesHistoryNamingTheActor() {
        navigate(OrderBoardView.class);
        var order = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();
        int before = orderService.historyLines(order.getReference()).size();

        grid().select(order);
        test(find(Button.class).withAriaLabel("Confirm selected").single()).click();

        var history = orderService.historyLines(order.getReference());
        assertEquals(before + 1, history.size());
        assertEquals("Malin Castro", history.getLast().authorName(), "the barista who pressed the button");
    }
}
