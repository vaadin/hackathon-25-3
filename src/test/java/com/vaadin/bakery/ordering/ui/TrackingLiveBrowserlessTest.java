package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.ordering.KitchenBoard;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.people.UserRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.QueryParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * CHK-08 and KIT-02. The last hop, from the kitchen to the customer.
 *
 * Between bakers the shared board is proven. What was never proven is the claim
 * a customer would care about: the tracking page they are already looking at
 * follows a state change made by somebody else, with nothing reloaded. It is
 * the same signal, and that is exactly why it is worth asserting rather than
 * assuming: the tracking page is a different route, a different role and an
 * anonymous session.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
// A class that writes to a shared signal needs its own context before it runs.
// See specs/FEEDBACK-25.3.md.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class TrackingLiveBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private OrderRepository orders;

    @Autowired
    private OrderService orderService;

    @Autowired
    private KitchenBoard board;

    @Autowired
    private UserRepository users;

    private String pageText() {
        return find(Span.class).all().stream()
                .map(Span::getText)
                .filter(text -> text != null)
                .reduce("", (all, text) -> all + " " + text);
    }

    @Test
    void aStateChangeReachesAnOpenTrackingPage() {
        var order = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();
        var barista = users.findByEmailIgnoreCase("barista@bakery.test").orElseThrow();

        // The customer, watching. Nothing is navigated after this point.
        // navigate(String) rejects a query separator, so the token goes in as
        // query parameters. The row is in specs/FEEDBACK-PLATFORM.md.
        UI.getCurrent().navigate("track/" + order.getReference(),
                QueryParameters.of("t", order.getTrackingToken()));
        assertTrue(pageText().contains("New"), "the page starts on the state it was placed in");

        // The counter, elsewhere.
        orderService.changeState(orders.findById(order.getId()).orElseThrow(),
                OrderState.CONFIRMED, "ordering.history.confirmed", barista);
        board.reload();

        var afterwards = pageText();
        assertTrue(afterwards.contains("Confirmed"),
                "the open page shows the new state: " + afterwards.trim());
        assertFalse(afterwards.contains("New") && !afterwards.contains("Confirmed"),
                "and not the old one");
    }
}
