package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.people.UserRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * BOARD-07. Two people editing one order, and the second one told rather than
 * quietly winning.
 *
 * The counter and the kitchen both open orders, and the same order can be open
 * in two places for minutes. Last write wins is the wrong answer here: the lines
 * of an order are what somebody is going to bake, and silently replacing one
 * person's edit with another's is how a cake comes out wrong.
 *
 * What defends it is the version column on every entity. This test holds a copy
 * of an order, lets somebody else change it, and then tries to save the copy.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConcurrentEditBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private OrderRepository orders;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository users;

    @Autowired
    private com.vaadin.bakery.catalogue.ProductRepository products;

    @Test
    void theSecondSaveOfTheSameOrderIsRefused() {
        var reference = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow()
                .getReference();
        var actor = users.findByEmailIgnoreCase("barista@bakery.test").orElseThrow();
        var product = products.findByAvailableTrueOrderBySortOrderAsc().getFirst();

        // Two screens, each holding the order as it was when they opened it.
        var firstScreen = orders.findByReference(reference).orElseThrow();
        var secondScreen = orders.findByReference(reference).orElseThrow();

        orderService.updateLines(firstScreen, List.of(new CartLine(product.getId(), 3, null)), actor);

        // The second screen still holds the version it opened with.
        assertThrows(OptimisticLockingFailureException.class,
                () -> orderService.updateLines(secondScreen,
                        List.of(new CartLine(product.getId(), 7, null)), actor),
                "the stale save is refused rather than overwriting the other one");
    }

    /**
     * And what the barista is told. The message names the way out, which is a
     * reload, because there is nothing they can do about a conflict except look
     * at what the order says now.
     */
    @Test
    void theRefusalTellsThemToReload() {
        var message = getTranslation("board.editor.saveFailed");

        assertTrue(message.toLowerCase().contains("reload"),
                "the message offers the only thing that helps: " + message);
    }

    /** The winner's edit is the one in the database, whole. */
    @Test
    void theFirstSaveIsTheOneThatSurvives() {
        var reference = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.NEW)
                .skip(1)
                .findFirst()
                .orElseThrow()
                .getReference();
        var actor = users.findByEmailIgnoreCase("barista@bakery.test").orElseThrow();
        var product = products.findByAvailableTrueOrderBySortOrderAsc().getFirst();
        var firstScreen = orders.findByReference(reference).orElseThrow();
        var secondScreen = orders.findByReference(reference).orElseThrow();

        orderService.updateLines(firstScreen, List.of(new CartLine(product.getId(), 3, null)), actor);
        try {
            orderService.updateLines(secondScreen, List.of(new CartLine(product.getId(), 7, null)), actor);
        } catch (OptimisticLockingFailureException expected) {
            // The point of the test.
        }

        var stored = orderService.detailLines(reference);
        assertEquals(1, stored.size(), "one line, from the save that won");
        assertEquals(3, stored.getFirst().quantity(), "and it is the first one's quantity");
    }

    private String getTranslation(String key) {
        return com.vaadin.flow.component.UI.getCurrent().getTranslation(key);
    }
}
