package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.QueryParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** CHK-07, CHK-09 and CHK-10. The one anonymous window onto private data. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class TrackingBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private OrderRepository orders;

    @Autowired
    private org.springframework.context.ApplicationContext context;

    private void open(String reference, String token) {
        UI.getCurrent().navigate("track/" + reference, QueryParameters.of("t", token));
    }

    @Test
    void theRightTokenShowsTheOrder() {
        var order = orders.findAll().getFirst();

        open(order.getReference(), order.getTrackingToken());

        assertTrue(find(H2.class).all().stream()
                .anyMatch(heading -> heading.getText().contains(order.getReference())));
    }

    @Test
    void aWrongTokenLooksExactlyLikeAWrongReference() {
        var order = orders.findAll().getFirst();

        open(order.getReference(), "not-the-token");
        var withWrongToken = find(H2.class).first().getText();

        open("ORD-2026-999999", "anything");
        var withWrongReference = find(H2.class).first().getText();

        assertEquals(withWrongReference, withWrongToken, "no oracle: both say the same thing");
        assertTrue(withWrongToken.contains("cannot find"));
    }

    @Test
    void cancellingIsOfferedOnlyWhileTheOrderIsNew() {
        var newOrder = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();
        open(newOrder.getReference(), newOrder.getTrackingToken());
        assertTrue(find(Button.class).withText("Cancel this order").all().size() == 1);

        var confirmed = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.CONFIRMED)
                .findFirst()
                .orElseThrow();
        open(confirmed.getReference(), confirmed.getTrackingToken());
        assertTrue(find(Button.class).withText("Cancel this order").all().isEmpty(),
                "once it is confirmed the customer has to call us");
    }

    @Test
    void reorderRefillsTheBasket() {
        var order = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.PICKED_UP)
                .findFirst()
                .orElseThrow();
        int lines = orders.findByReference(order.getReference()).orElseThrow().getItems().size();
        assertFalse(lines == 0, "the order had items to copy");

        open(order.getReference(), order.getTrackingToken());
        test(find(Button.class).withText("Order this again").single()).click();

        var cart = context.getBean(com.vaadin.bakery.ordering.CartSignals.class);
        assertEquals(lines, cart.snapshot().size(), "every line that is still sold comes back");
    }
}
