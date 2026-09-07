package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The board is a wall display, so its shape is part of what it does: one column
 * per state, each scrolling inside itself, and the day's production summary
 * beside the board rather than below the fold where nobody would find it.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class KitchenBoardLayoutBrowserlessTest extends SpringBrowserlessTest {

    @BeforeEach
    void signIn() {
        TestLogin.asBaker();
    }

    @Test
    void thereIsOneColumnPerKitchenState() {
        navigate(KitchenBoardView.class);

        var columns = find(Div.class).withClassName("kitchen-board__column").all();
        assertEquals(OrderState.kitchenColumns().size(), columns.size());
    }

    @Test
    void eachColumnCarriesItsOwnScroller() {
        navigate(KitchenBoardView.class);

        var scrollers = find(Div.class).withClassName("kitchen-board__scroller").all();
        assertEquals(OrderState.kitchenColumns().size(), scrollers.size(),
                "the scroller is per column, so a long column does not push the others off screen");
    }

    @Test
    void theSummaryStartsClosedAndOpensBesideTheBoard() {
        navigate(KitchenBoardView.class);
        var layout = find(MasterDetailLayout.class).single();

        assertNotNull(layout.getMaster(), "the board is the master");
        assertNull(layout.getDetail(), "and nothing is open beside it yet");

        test(find(Button.class).withText("What to bake today").single()).click();

        assertNotNull(layout.getDetail(), "the summary opens as the detail rather than below the fold");
        assertFalse(find(Table.class).all().isEmpty(), "and it carries the production table");
    }

    @Test
    void theSummaryCanBeClosedAgain() {
        navigate(KitchenBoardView.class);
        var layout = find(MasterDetailLayout.class).single();
        var toggle = find(Button.class).withText("What to bake today").single();

        test(toggle).click();
        assertNotNull(layout.getDetail());

        test(toggle).click();
        assertNull(layout.getDetail(), "the board gets its full width back");
    }

    @Test
    void everyColumnSaysHowManyTicketsItHolds() {
        navigate(KitchenBoardView.class);

        var headers = find(Div.class).withClassName("kitchen-board__column-header").all();
        assertEquals(OrderState.kitchenColumns().size(), headers.size());
        headers.forEach(header -> assertTrue(
                header.getElement().getTextRecursively().matches(".*\\d+.*"),
                "a full column should read as full before anybody counts the cards"));
    }
}
