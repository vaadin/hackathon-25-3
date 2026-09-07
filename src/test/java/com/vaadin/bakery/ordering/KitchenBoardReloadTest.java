package com.vaadin.bakery.ordering;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.ui.KitchenBoardView;
import com.vaadin.browserless.SpringBrowserlessTest;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * KIT-11. Reloading the board twice leaves the board it had once.
 *
 * The smallest form of the defect: no view, no session, no second user. Just
 * the service, reloaded twice.
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
class KitchenBoardReloadTest extends SpringBrowserlessTest {

    @Autowired
    private KitchenBoard board;

    /**
     * With a board open, which is the case that fails. The same two reloads
     * with nobody watching are fine, so the difference is an attached view
     * whose effect is subscribed to the shared list.
     */
    @Test
    void reloadingTwiceDoesNotDoubleTheBoardWithABoardOpen() {
        TestLogin.asBaker();
        navigate(KitchenBoardView.class);

        board.reload();
        int afterOne = board.snapshot().size();

        board.reload();

        assertEquals(afterOne, board.snapshot().size(),
                "the same kitchen, not the same kitchen twice");
    }

    @Test
    void noOrderAppearsOnTheBoardMoreThanOnce() {
        TestLogin.asBaker();
        navigate(KitchenBoardView.class);
        board.reload();
        board.reload();
        board.reload();

        var counts = board.snapshot().stream()
                .collect(Collectors.groupingBy(KitchenTicket::orderId, Collectors.counting()));
        var repeated = counts.entrySet().stream().filter(entry -> entry.getValue() > 1).toList();

        assertEquals(0, repeated.size(), "every order is on the wall once, repeated: " + repeated);
    }
}
