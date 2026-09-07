package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.OrderActivity;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.messages.MessageList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * CONV-06. A reply reaches the other side without anybody reloading.
 *
 * The conversation used to refresh when a page opened, which reads as working
 * whenever the person testing it opens the page after replying. What it never
 * did was arrive: a customer sitting on the tracking page waiting to hear about
 * their cake saw nothing until they pressed reload.
 *
 * The panel now follows a shared signal per order. This test has one panel
 * open, posts from somewhere else, and asserts the open panel caught up.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConversationMultiUserBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private OrderActivity activity;

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    private String anOpenOrder() {
        return orders.findAll().stream()
                .filter(order -> order.getState() != OrderState.PICKED_UP
                        && order.getState() != OrderState.CANCELLED)
                .findFirst()
                .orElseThrow()
                .getReference();
    }

    private MessageList list() {
        return find(MessageList.class).first();
    }

    @Test
    void aReplyPostedElsewhereArrivesInAnOpenPanel() {
        var reference = anOpenOrder();
        navigate("orders/" + reference, OrderDetailView.class);
        int before = list().getItems().size();

        // Somebody else's screen, or the same conversation from the customer's
        // side. Either way this panel was not the one that typed it.
        orderService.post(reference, "The customer", false, null, "Is it ready yet?", List.of());

        var after = list().getItems();
        assertEquals(before + 1, after.size(), "the open panel caught the new message");
        assertTrue(after.stream().anyMatch(item -> "Is it ready yet?".equals(item.getText())),
                "and it is the message that was posted");
    }

    /** The signal is per order, so a message about one does not stir the other. */
    @Test
    void aMessageAboutAnotherOrderLeavesThisPanelAlone() {
        var reference = anOpenOrder();
        var somewhereElse = orders.findAll().stream()
                .filter(order -> order.getState() != OrderState.PICKED_UP
                        && order.getState() != OrderState.CANCELLED)
                .map(order -> order.getReference())
                .filter(candidate -> !candidate.equals(reference))
                .findFirst()
                .orElseThrow();
        navigate("orders/" + reference, OrderDetailView.class);
        int before = list().getItems().size();

        orderService.post(somewhereElse, "The customer", false, null, "Different order", List.of());

        assertEquals(before, list().getItems().size(), "this conversation did not move");
    }

    /** And the signal itself counts, which is what every panel is watching. */
    @Test
    void postingMovesTheOrdersOwnSignal() {
        var reference = anOpenOrder();
        long before = activity.forOrder(reference).peek();

        orderService.post(reference, "The bakery", true, null, "On it", List.of());

        assertTrue(activity.forOrder(reference).peek() > before,
                "the shared value moved, which is what reaches the other screens");
    }
}
