package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.KitchenBoard;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.people.UserRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.html.Div;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * KIT-09 and KIT-10. A board on a wall has to admit when it may be behind, and
 * has to show a baker that somebody else moved something.
 *
 * The board is read from across a kitchen and touched once an hour. The failure
 * it must not have is looking current while the connection behind it has gone,
 * because a stale board is worse than no board: somebody bakes from it.
 */
@SpringBootTest(classes = Application.class,
        properties = "bakery.kitchen.stale-after=PT2M")
@ActiveProfiles("test")
// A class that writes to a shared signal needs a context of its own, and it
// needs it BEFORE rather than AFTER. The signal is an application singleton and
// the browserless signal environment is per class: inheriting a context whose
// environment has been replaced leaves the signal writable in name only, its
// writes dropped and their operations never completing. See
// specs/FEEDBACK-25.3.md.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class StaleTicketBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private KitchenBoard board;

    @Autowired
    private UserRepository users;

    @BeforeEach
    void signIn() {
        TestLogin.asBaker();
    }

    /**
     * The arithmetic, driven directly rather than by waiting two minutes for
     * the scheduled check to come round. What is not asserted here is the
     * scheduling itself: {@code UI.triggerAfter} needs a live UI and a real
     * clock, and that half is the browser tier's.
     */
    @Test
    void silenceLongerThanTheThresholdIsStale() {
        var view = navigate(KitchenBoardView.class);
        var lastHeard = Instant.parse("2026-01-07T09:00:00Z");

        assertFalse(view.isStale(lastHeard, lastHeard.plus(Duration.ofSeconds(30))),
                "half a minute of quiet is a quiet kitchen, not a broken screen");
        assertTrue(view.isStale(lastHeard, lastHeard.plus(Duration.ofMinutes(2))),
                "the configured two minutes is the threshold, and reaching it counts");
        assertTrue(view.isStale(lastHeard, lastHeard.plus(Duration.ofHours(9))),
                "a board left open overnight is certainly behind");
    }

    @Test
    void theMarkerIsHiddenUntilItIsNeededAndAReloadClearsIt() {
        var view = navigate(KitchenBoardView.class);

        assertFalse(view.isShowingStale(), "a board that has just loaded is not stale");

        view.showStale(true);
        assertTrue(view.isShowingStale(), "and it says so when it is");

        view.refresh();
        assertFalse(view.isShowingStale(), "reloading from the service answers the marker");
        assertFalse(board.snapshot().isEmpty(), "and it really did reload the board");
    }

    /**
     * KIT-10. The flag on a ticket somebody else moved.
     *
     * The board redraws whole on every change, so the card carries the class
     * only when this screen did not make the change itself. A screen that
     * flashes its own work teaches a baker to ignore the flash.
     */
    @Test
    void aTicketMovedElsewhereIsFlaggedAndOneMovedHereIsNot() {
        var view = navigate(KitchenBoardView.class);
        var baker = users.findByEmailIgnoreCase("baker@bakery.test").orElseThrow();
        var ticket = board.snapshot().stream()
                .filter(candidate -> candidate.state() == OrderState.CONFIRMED)
                .findFirst()
                .orElseThrow();

        // Somebody else's tablet, which is what the shared signal makes of it.
        board.advance(ticket, OrderState.IN_PREPARATION, baker);

        assertTrue(flaggedCards(view) > 0, "the ticket that moved is flagged on this screen");

        // And a redraw with nothing new leaves the board calm again.
        view.refresh();
        assertTrue(flaggedCards(view) == 0, "nothing is still flashing once it has been seen");
    }

    private long flaggedCards(KitchenBoardView view) {
        return find(Div.class).withClassName("kitchen-board__ticket--moved").all().size();
    }
}
