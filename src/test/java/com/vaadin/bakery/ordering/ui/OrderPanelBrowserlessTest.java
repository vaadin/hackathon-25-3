package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * BOARD-14, BOARD-15 and BOARD-16. Opening an order must not cost the list.
 * The Escape/backdrop-close tests here fire the server-side DOM events the
 * browser produces and check the server-side wiring reacts correctly; they
 * cannot press a real key or click a real backdrop, so they do not by
 * themselves prove those gestures still reach the layout in a live browser.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "barista@bakery.test", roles = { "BARISTA" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderPanelBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private OrderRepository orders;

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    private String anyReference() {
        return orders.findAll().getFirst().getReference();
    }

    @Test
    void followingAnOrderAddressArrivesWithTheListStillThere() {
        UI.getCurrent().navigate("orders/" + anyReference());

        assertFalse(find(Grid.class).all().isEmpty(), "the list is still on screen");
        assertFalse(find(OrderDetailView.class).all().isEmpty(), "and the order is open beside it");
    }

    @Test
    void theBoardIsTheLayoutOfTheOrderAddress() {
        UI.getCurrent().navigate("orders/" + anyReference());

        assertEquals(1, find(OrderBoardView.class).all().size(),
                "one board, hosting the order rather than being replaced by it");
    }

    @Test
    void closingTheOrderReturnsToThePlainList() {
        UI.getCurrent().navigate("orders/" + anyReference());
        assertFalse(find(OrderDetailView.class).all().isEmpty());

        find(OrderDetailView.class).single().close();

        assertTrue(find(OrderDetailView.class).all().isEmpty(), "the panel is gone");
        assertFalse(find(Grid.class).all().isEmpty(), "the list remains");
    }

    /**
     * Exercises the registrations at {@code OrderBoardView.closePanel()} directly,
     * the same way the browser's Escape key would trigger them: no browserless
     * test can press a physical key, so this fires the DOM event Escape produces
     * client-side and checks the server reacts to it. It proves the listener is
     * wired to the right outcome; it is not proof that a real browser still
     * delivers the keydown to the layout, which is BOARD-16's real subject and
     * belongs to a browser-driven check instead.
     */
    @Test
    void detailEscapePressClosesThePanel() {
        UI.getCurrent().navigate("orders/" + anyReference());
        var board = find(OrderBoardView.class).single();

        ComponentUtil.fireEvent(board, new MasterDetailLayout.DetailEscapePressEvent(board, true));

        assertTrue(find(OrderDetailView.class).all().isEmpty(), "Escape closes the panel");
        assertFalse(find(Grid.class).all().isEmpty(), "the list remains");
    }

    /** Same reasoning as above, for the backdrop click registration. */
    @Test
    void backdropClickClosesThePanel() {
        UI.getCurrent().navigate("orders/" + anyReference());
        var board = find(OrderBoardView.class).single();

        ComponentUtil.fireEvent(board, new MasterDetailLayout.BackdropClickEvent(board, true));

        assertTrue(find(OrderDetailView.class).all().isEmpty(), "the backdrop click closes the panel");
        assertFalse(find(Grid.class).all().isEmpty(), "the list remains");
    }
}
