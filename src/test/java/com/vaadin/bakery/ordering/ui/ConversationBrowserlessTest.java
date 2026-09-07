package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.people.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/** MSG-01, MSG-03, MSG-06 and MSG-07. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConversationBrowserlessTest {

    @Autowired
    private OrderService orders;

    @Autowired
    private OrderRepository repository;

    @Autowired
    private UserRepository users;

    private String openOrder() {
        return repository.findAll().stream()
                .filter(order -> order.getState().isOpen())
                .findFirst()
                .orElseThrow()
                .getReference();
    }

    @Test
    void bothSidesOfTheConversationAreKept() {
        var reference = openOrder();
        var staff = users.findByEmailIgnoreCase("barista@bakery.test").orElseThrow();
        int before = orders.messages(reference).size();

        orders.post(reference, "Marta", false, null, "Can I add a candle?", List.of());
        orders.post(reference, staff.getFullName(), true, staff, "Of course, we will add one.", List.of());

        var messages = orders.messages(reference);
        assertEquals(before + 2, messages.size());
        assertFalse(messages.get(messages.size() - 2).fromStaff(), "the customer wrote first");
        assertTrue(messages.getLast().fromStaff(), "and the bakery answered");
    }

    @Test
    void aCustomerMessageCountsAsUnreadUntilStaffOpenIt() {
        var reference = openOrder();
        orders.markMessagesRead(reference);

        orders.post(reference, "Marta", false, null, "Is it ready?", List.of());
        assertTrue(orders.unreadMessages(reference) > 0, "the badge has something to show");

        orders.markMessagesRead(reference);
        assertEquals(0, orders.unreadMessages(reference), "and opening the conversation clears it");
    }

    @Test
    void whatTheCustomerWritesNeverBecomesMarkup() {
        var reference = openOrder();

        orders.post(reference, "Marta", false, null, "please write <b>Ana</b> on it", List.of());

        var last = orders.messages(reference).getLast().text();
        assertFalse(last.contains("<b>"), last);
        assertTrue(last.contains("&lt;b&gt;"), "the characters survive, escaped: " + last);
    }

    @Test
    void aFinishedOrderClosesTheConversation() {
        var reference = repository.findAll().stream()
                .filter(order -> order.getState() == OrderState.PICKED_UP)
                .findFirst()
                .orElseThrow()
                .getReference();

        var failure = assertThrows(DomainException.RuleViolation.class,
                () -> orders.post(reference, "Marta", false, null, "hello?", List.of()));
        assertEquals("ordering.message.closed", failure.translationKey());
    }
}
