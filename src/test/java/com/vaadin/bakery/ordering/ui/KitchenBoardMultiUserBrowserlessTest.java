package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.ordering.KitchenBoard;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.people.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * KIT-01 and KIT-03. The claim is that one baker moving a ticket moves it for
 * everybody, so the test needs two bakers, and the shared signal is the thing
 * under test rather than the view.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
// A class that writes to a shared signal needs a context of its own, and it
// needs it BEFORE rather than AFTER. The signal is an application singleton and
// the browserless signal environment is per class: inheriting a context whose
// environment has been replaced leaves the signal writable in name only, its
// writes dropped and their operations never completing. See
// specs/FEEDBACK-25.3.md.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class KitchenBoardMultiUserBrowserlessTest {

    @Autowired
    private KitchenBoard board;

    @Autowired
    private UserRepository users;

    @Test
    void theBoardIsSharedRatherThanPerSession() {
        board.reload();
        var before = board.snapshot();
        assertTrue(before.size() > 0, "the reconciled dataset always leaves work in the kitchen");

        // The signal is one object for the whole application, which is exactly
        // why one baker's change is another baker's screen.
        assertEquals(board.tickets(), board.tickets());
        assertEquals(before.size(), board.tickets().peek().size());
    }

    @Test
    void movingATicketIsVisibleToEverybodyWatchingTheSignal() {
        board.reload();
        var ticket = board.snapshot().stream()
                .filter(candidate -> candidate.state() == OrderState.CONFIRMED)
                .findFirst()
                .orElseThrow();
        var baker = users.findByEmailIgnoreCase("baker@bakery.test").orElseThrow();

        board.advance(ticket, OrderState.IN_PREPARATION, baker);

        var after = board.snapshot().stream()
                .filter(candidate -> candidate.orderId().equals(ticket.orderId()))
                .findFirst()
                .orElseThrow();
        assertEquals(OrderState.IN_PREPARATION, after.state(), "the shared value moved");
        assertNotEquals(ticket.state(), after.state());
    }

    @Test
    void aTicketThatLeavesTheKitchenLeavesTheBoard() {
        board.reload();
        var baker = users.findByEmailIgnoreCase("baker@bakery.test").orElseThrow();
        // Walk one ticket the whole way rather than hoping the seeded day has a
        // ready one: the point is that leaving the kitchen removes it.
        var ticket = board.snapshot().stream()
                .filter(candidate -> candidate.state() == OrderState.CONFIRMED)
                .findFirst()
                .orElseThrow();
        board.advance(ticket, OrderState.IN_PREPARATION, baker);
        var inPreparation = board.snapshot().stream()
                .filter(candidate -> candidate.orderId().equals(ticket.orderId()))
                .findFirst()
                .orElseThrow();
        board.advance(inPreparation, OrderState.READY, baker);
        var ready = board.snapshot().stream()
                .filter(candidate -> candidate.orderId().equals(ticket.orderId()))
                .findFirst()
                .orElseThrow();

        // The hand over is the counter's, not the oven's: it is where the invoice
        // is issued, so a baker no longer has it. See specs/04-security.md.
        var barista = users.findByEmailIgnoreCase("barista@bakery.test").orElseThrow();
        board.advance(ready, OrderState.PICKED_UP, barista);

        assertTrue(board.snapshot().stream()
                .noneMatch(candidate -> candidate.orderId().equals(ticket.orderId())),
                "a picked up order is off the wall");
    }

    @Test
    void twoBakersClaimingTheSameTicketLeavesOneOwnerAndTellsTheOther() {
        board.reload();
        var ticket = board.snapshot().stream()
                .filter(candidate -> !candidate.isClaimed())
                .findFirst()
                .orElseThrow();
        var ana = users.findByEmailIgnoreCase("ana@bakery.test").orElseThrow();
        var luis = users.findByEmailIgnoreCase("luis@bakery.test").orElseThrow();

        var first = board.claim(ticket, ana);
        var second = board.claim(ticket, luis);

        assertTrue(first.isEmpty(), "the first baker just gets it");
        assertTrue(second.isPresent(), "and the second is told who has it");
        assertEquals(ana.getFullName(), second.get());

        var owner = board.snapshot().stream()
                .filter(candidate -> candidate.orderId().equals(ticket.orderId()))
                .findFirst()
                .orElseThrow();
        assertEquals(ana.getId(), owner.assignedBakerId());
    }
}
