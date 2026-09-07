package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.billing.InvoiceRepository;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/** BOARD-09 and the state actions a role is allowed to take. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "barista@bakery.test", roles = { "BARISTA" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderDetailBrowserlessTest extends SpringBrowserlessTest {

    @org.junit.jupiter.api.BeforeEach
    void signIn() {
        // After the browserless environment is up, see TestLogin for why.
        TestLogin.asBarista();
    }

    @Autowired
    private OrderRepository orders;

    @Autowired
    private OrderService orderService;

    @Autowired
    private InvoiceRepository invoices;

    private void open(String reference) {
        UI.getCurrent().navigate("orders/" + reference);
    }

    @Test
    void theDetailShowsTheOrderAndItsHistory() {
        var order = orders.findAll().getFirst();
        open(order.getReference());

        assertTrue(find(H2.class).all().stream()
                .anyMatch(heading -> heading.getText().contains(order.getReference())));
    }

    @Test
    void onlyLegalTransitionsAreOffered() {
        var newOrder = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();
        open(newOrder.getReference());

        var labels = find(Button.class).all().stream().map(Button::getText).toList();
        assertTrue(labels.contains("Confirm"), labels.toString());
        assertFalse(labels.contains("Mark picked up"), "an order cannot skip straight to picked up");
    }

    @Test
    void confirmingWritesHistoryNamingTheBarista() {
        var order = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();
        int before = orderService.historyLines(order.getReference()).size();

        open(order.getReference());
        test(find(Button.class).withText("Confirm").single()).click();

        var history = orderService.historyLines(order.getReference());
        assertEquals(before + 1, history.size());
        assertEquals(OrderState.CONFIRMED, history.getLast().newState());
        assertEquals("Malin Castro", history.getLast().authorName());
    }

    /**
     * The spec's edge case, in its own words: a baker "reads everything,
     * changes only the states their role allows". The panel gained a line
     * editor that reprices from the live catalogue, so leaving it open to a
     * baker would let them change what a customer is charged.
     */
    @Test
    void aBakerReadsTheLinesButCannotChangeThem() {
        var order = orders.findAll().stream()
                .filter(candidate -> !orderService.detailLines(candidate.getReference()).isEmpty())
                .findFirst()
                .orElseThrow();

        TestLogin.asBaker();
        open(order.getReference());

        var editor = find(OrderLineEditor.class).single();
        assertFalse(editor.getLines().isEmpty(), "the baker still reads what was ordered");
        assertFalse(editor.isEnabled(), "but cannot type into it");
        assertTrue(find(Button.class).all().stream().noneMatch(button -> "Save the order".equals(button.getText())),
                "and is offered nothing to save with");
    }

    /** The other half of the same rule: the state actions stay. */
    @Test
    void aBakerKeepsTheStateActionsTheirRoleAllows() {
        var newOrder = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();

        TestLogin.asBaker();
        open(newOrder.getReference());

        var labels = find(Button.class).all().stream().map(Button::getText).toList();
        assertTrue(labels.contains("Confirm"), labels.toString());
    }

    /** Taking an order decides its lines and its price, so it is closed too. */
    @Test
    void aBakerIsNotOfferedTakingANewOrder() {
        TestLogin.asBaker();
        UI.getCurrent().navigate(OrderBoardView.ROUTE);

        assertTrue(find(Button.class).all().stream().noneMatch(button -> "New order".equals(button.getText())),
                "the board offers a baker no button that would only be refused");
    }

    /**
     * A state action used to call {@code Page.reload()}, which in a browser
     * throws away the list, its scroll position, the selection a bulk action
     * was being assembled from and every expanded row. The panel now re-renders
     * over a refreshed board instead, which is also the only version of this
     * that is observable without a browser: a reload does nothing here, so
     * before the change the panel still offered the action it had just taken.
     */
    @Test
    void aStateChangeRerendersThePanelAndKeepsTheList() {
        var newOrder = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();
        open(newOrder.getReference());
        var board = find(OrderBoardView.class).single();

        test(find(Button.class).withText("Confirm").single()).click();

        assertSame(board, find(OrderBoardView.class).single(), "the same list is still on screen");
        var labels = find(Button.class).all().stream().map(Button::getText).toList();
        assertTrue(labels.contains("Start baking"), "the panel shows the new state's actions: " + labels);
        assertFalse(labels.contains("Confirm"), "and stops offering the one just taken: " + labels);
    }

    @Test
    void markingAnOrderPickedUpIssuesItsInvoice() {
        var ready = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.READY)
                .filter(order -> invoices.findByOrder(order).isEmpty())
                .findFirst()
                .orElseThrow();

        open(ready.getReference());
        test(find(Button.class).withText("Mark picked up").single()).click();

        var updated = orders.findById(ready.getId()).orElseThrow();
        assertEquals(OrderState.PICKED_UP, updated.getState());
        assertTrue(invoices.findByOrder(updated).isPresent(), "picking up is what issues the invoice");
    }
}
