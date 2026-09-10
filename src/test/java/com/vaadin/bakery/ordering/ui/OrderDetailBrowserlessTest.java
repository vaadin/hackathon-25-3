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

    /**
     * The state is a field now, not a row of buttons.
     *
     * It used to be one button per legal transition, which is what these tests
     * asserted. The panel offers a combo box of the states this order may move
     * to, and Save is what commits the choice: a barista who picked the wrong
     * one can pick again, which a button that acted on click never allowed.
     */
    @SuppressWarnings("unchecked")
    private com.vaadin.flow.component.combobox.ComboBox<OrderState> stateField() {
        return find(com.vaadin.flow.component.combobox.ComboBox.class).all().stream()
                .filter(combo -> combo.getClassNames().contains("order-detail__state"))
                .map(combo -> (com.vaadin.flow.component.combobox.ComboBox<OrderState>) combo)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the panel has a state field"));
    }

    /** Which states this order may move to, as the field offers them. */
    private java.util.List<OrderState> offered() {
        return stateField().getListDataView().getItems().toList();
    }

    /** Picking a state and committing it, which is one gesture in the panel. */
    private void moveTo(OrderState target) {
        stateField().setValue(target);
        test(find(Button.class).withText("Save").single()).click();
    }

    @Test
    void onlyLegalTransitionsAreOffered() {
        var newOrder = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();
        open(newOrder.getReference());

        // The order's own state is in the list as well, because a field has to
        // show what it holds, so this asserts the targets around it.
        assertTrue(offered().contains(OrderState.CONFIRMED), offered().toString());
        assertTrue(offered().contains(OrderState.NEW), "and the state it is in now");
        assertFalse(offered().contains(OrderState.PICKED_UP),
                "an order cannot skip straight to picked up");
    }

    @Test
    void confirmingWritesHistoryNamingTheBarista() {
        var order = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();
        int before = orderService.historyLines(order.getReference()).size();

        open(order.getReference());
        moveTo(OrderState.CONFIRMED);

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

        assertTrue(offered().contains(OrderState.CONFIRMED), offered().toString());
        assertFalse(stateField().isReadOnly(), "a baker may still move the state");
    }

    /** Taking an order decides its lines and its price, so it is closed too. */
    @Test
    void aBakerIsNotOfferedTakingANewOrder() {
        TestLogin.asBaker();
        UI.getCurrent().navigate(OrderBoardView.ROUTE);

        assertTrue(find(Button.class).all().stream()
                        .noneMatch(button -> "New order".equals(button.getAriaLabel().orElse(""))),
                "the board offers a baker no button that would only be refused");
    }

    /**
     * A state action used to call {@code Page.reload()}, which in a browser
     * throws away the list, its scroll position, the selection a bulk action
     * was being assembled from and every expanded row. Saving refreshes the
     * board behind the panel and closes the panel instead, which is the whole
     * of what one Save does: the barista is looking at the list again, with the
     * order they just moved in it.
     */
    @Test
    void savingKeepsTheListAndClosesThePanel() {
        var newOrder = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.NEW)
                .findFirst()
                .orElseThrow();
        open(newOrder.getReference());
        var board = find(OrderBoardView.class).single();

        moveTo(OrderState.CONFIRMED);

        assertSame(board, find(OrderBoardView.class).single(), "the same list is still on screen");
        assertTrue(find(OrderDetailView.class).all().isEmpty(), "and the panel is done with");
        assertEquals(OrderState.CONFIRMED, orders.findById(newOrder.getId()).orElseThrow().getState(),
                "with the order moved");
    }

    /**
     * POL2-03. The message box carried the component's own default, which is
     * the word "Message" in English whatever language the page is in. It names
     * what the box is about rather than what writing in it does, and it never
     * followed the reader.
     */
    @Test
    void theMessageBoxIsNamedForWhatItDoes() {
        open(orders.findAll().stream()
                .filter(order -> order.getState().isOpen())
                .findFirst().orElseThrow().getReference());

        var input = find(com.vaadin.flow.component.messages.MessageInput.class).single();
        assertEquals("New message", input.getI18n().getMessage(), "not the component's own \"Message\"");
        assertEquals("Send", input.getI18n().getSend());

        try {
            UI.getCurrent().setLocale(java.util.Locale.of("es"));
            assertEquals("Mensaje nuevo", input.getI18n().getMessage(), "and it follows the language");
        } finally {
            // The UI outlives the method, and a Spanish one would surprise the
            // next test in this class.
            UI.getCurrent().setLocale(java.util.Locale.ENGLISH);
        }
    }

    @Test
    void markingAnOrderPickedUpIssuesItsInvoice() {
        var ready = orders.findAll().stream()
                .filter(order -> order.getState() == OrderState.READY)
                .filter(order -> invoices.findByOrder(order).isEmpty())
                .findFirst()
                .orElseThrow();

        open(ready.getReference());
        moveTo(OrderState.PICKED_UP);

        var updated = orders.findById(ready.getId()).orElseThrow();
        assertEquals(OrderState.PICKED_UP, updated.getState());
        assertTrue(invoices.findByOrder(updated).isPresent(), "picking up is what issues the invoice");
    }
}
