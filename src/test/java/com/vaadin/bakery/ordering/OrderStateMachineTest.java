package com.vaadin.bakery.ordering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.error.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** DOM-04. Transitions are declared once, so they can be tested once. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Transactional
class OrderStateMachineTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orders;

    @Test
    void theDeclaredTransitionsAreTheOnlyOnes() {
        assertTrue(OrderState.NEW.canMoveTo(OrderState.CONFIRMED));
        assertFalse(OrderState.NEW.canMoveTo(OrderState.READY), "an order cannot skip preparation");
        assertTrue(OrderState.PROBLEM.canMoveTo(OrderState.CONFIRMED), "a problem can be resolved");
        assertTrue(OrderState.PICKED_UP.allowedTargets().isEmpty(), "a picked up order is finished");
        // A cancelled order is not finished, it is refused, and a customer who
        // cancelled by mistake and rang back is a real morning at a bakery.
        // Confirmed and nothing else: the order goes back to the queue rather
        // than to whatever state it happened to be cancelled from.
        assertEquals(java.util.Set.of(OrderState.CONFIRMED), OrderState.CANCELLED.allowedTargets(),
                "a cancelled order can be reopened, and only into the queue");
    }

    @Test
    void everyTransitionWritesHistory() {
        var order = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();
        int before = order.getHistory().size();

        var confirmed = orderService.changeState(order, OrderState.CONFIRMED, "ordering.history.confirmed", null);

        assertEquals(OrderState.CONFIRMED, confirmed.getState());
        assertEquals(before + 1, confirmed.getHistory().size());
        assertEquals(OrderState.CONFIRMED, confirmed.getHistory().getLast().getNewState());
    }

    @Test
    void anIllegalTransitionIsRefusedByName() {
        var order = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();

        var failure = assertThrows(DomainException.RuleViolation.class,
                () -> orderService.changeState(order, OrderState.PICKED_UP, "nope", null));
        assertEquals("ordering.state.illegalTransition", failure.translationKey());
    }

    @Test
    void aCustomerMayOnlyCancelWhileTheOrderIsNew() {
        var confirmed = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.CONFIRMED)
                .findFirst()
                .orElseThrow();

        var failure = assertThrows(DomainException.RuleViolation.class,
                () -> orderService.cancelAsCustomer(confirmed));
        assertEquals("ordering.cancel.tooLate", failure.translationKey());
    }
}
