package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.KitchenBoard;
import com.vaadin.bakery.ordering.KitchenTicket;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.people.UserService;
import com.vaadin.browserless.SpringBrowserlessTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * POL2-07 and POL2-08. Who a ticket belongs to, and where it may go.
 *
 * "Claim this" only ever meant one person, which is right for a baker at the
 * oven and wrong for an administrator, who was assigning kitchen work to an
 * administrator every time they pressed it. And a ticket dragged onto a column
 * has to obey exactly the rules the buttons obey, or the board grows a second
 * set of rules that nobody wrote down.
 *
 * The drag itself is not here. A drop is a gesture and this tier cannot make
 * one; what it can do, and what this does, is call the decision the drop
 * listener calls, which is why that decision is a method rather than a lambda
 * inside the listener.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
// The board is a shared signal, so a class that writes to it must not hand the
// next class a dead signal environment. See FEEDBACK-25.3.md.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class KitchenAssignmentBrowserlessTest extends SpringBrowserlessTest {

    @BeforeEach
    void signIn() {
        TestLogin.asBaker();
    }

    @Autowired
    private KitchenBoard board;

    @Autowired
    private UserService people;

    private KitchenTicket ticketIn(OrderState state) {
        return board.snapshot().stream()
                .filter(ticket -> ticket.state() == state)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no ticket is " + state + " on the seeded board"));
    }

    private KitchenTicket reloaded(KitchenTicket ticket) {
        return board.snapshot().stream()
                .filter(candidate -> candidate.orderId().equals(ticket.orderId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the ticket left the board"));
    }

    @Test
    void thePickerOffersEveryBakerAndNobodyElse() {
        var offered = people.bakers();

        assertFalse(offered.isEmpty(), "there are bakers to assign to");
        assertTrue(offered.stream().allMatch(user -> user.getRole() == Role.BAKER),
                "and only bakers: a ticket assigned to an administrator is a ticket nobody is making");
        assertTrue(offered.stream().noneMatch(user -> user.isLocked()),
                "and nobody who cannot sign in");
    }

    @Test
    void aTicketIsAssignedToTheBakerThatWasChosen() {
        var view = navigate(KitchenBoardView.class);
        var ticket = ticketIn(OrderState.CONFIRMED);
        var chosen = people.bakers().stream()
                .filter(baker -> !baker.getId().equals(ticket.assignedBakerId()))
                .findFirst()
                .orElseThrow();

        view.assign(ticket, chosen);

        var after = reloaded(ticket);
        assertEquals(chosen.getId(), after.assignedBakerId(), "the ticket is that person's");
        assertEquals(chosen.getFullName(), after.assignedBakerName(), "and the board says whose");
    }

    @Test
    void aDropOnAColumnTheTicketCannotReachIsRefused() {
        var view = navigate(KitchenBoardView.class);
        var ticket = ticketIn(OrderState.CONFIRMED);

        assertFalse(view.dropOnto(ticket, OrderState.READY),
                "confirmed does not jump the oven, however it is dropped");
        assertEquals(OrderState.CONFIRMED, reloaded(ticket).state(), "and the ticket did not move");
    }

    @Test
    void aDropOnTheNextColumnMovesTheTicket() {
        var view = navigate(KitchenBoardView.class);
        var ticket = ticketIn(OrderState.CONFIRMED);

        assertTrue(view.dropOnto(ticket, OrderState.IN_PREPARATION));

        assertEquals(OrderState.IN_PREPARATION, reloaded(ticket).state(),
                "the same move the button makes, written to the same signal");
    }

    @Test
    void aDropOnTheColumnItIsAlreadyInChangesNothing() {
        var view = navigate(KitchenBoardView.class);
        var ticket = ticketIn(OrderState.CONFIRMED);

        assertTrue(view.dropOnto(ticket, OrderState.CONFIRMED), "not a refusal, because nothing was asked");
        assertEquals(OrderState.CONFIRMED, reloaded(ticket).state());
    }
}
