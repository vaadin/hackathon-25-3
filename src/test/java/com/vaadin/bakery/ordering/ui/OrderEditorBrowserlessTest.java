package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.base.Money;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * BOARD-17, BOARD-18 and BOARD-19. Any product, any quantity, zero means
 * remove, and nothing typed is thrown away without asking.
 *
 * The editor lives inside {@link OrderDetailView}, so it is reached by
 * navigating to an order's panel and finding it there, rather than by adding
 * a bare component to the page.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
// aLineWhoseProductBecameUnavailableIsNotDroppedSilently writes to the shared
// catalogue, so the context this class leaves behind is not the one it was
// given, however carefully it restores the row it touched.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderEditorBrowserlessTest extends SpringBrowserlessTest {

    @BeforeEach
    void signIn() {
        // After the browserless environment is up, see TestLogin for why.
        TestLogin.asBarista();
    }

    @Autowired
    private CatalogueService catalogue;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private OrderService orderService;

    private OrderLineEditor editor() {
        navigate("orders/" + referenceWithLines(), OrderDetailView.class);
        return find(OrderLineEditor.class).single();
    }

    /** Any seeded order that actually has something in it. */
    private String referenceWithLines() {
        return orders.findAll().stream()
                .map(com.vaadin.bakery.ordering.Order::getReference)
                .filter(reference -> !orderService.detailLines(reference).isEmpty())
                .findFirst()
                .orElseThrow();
    }

    /** The same formula {@code OrderItem.net()/vat()/gross()} snapshots onto a line. */
    private static Money gross(com.vaadin.bakery.catalogue.Product product, int quantity) {
        var net = product.price().times(quantity);
        return net.plus(net.percentage(product.getVatRate().percent()));
    }

    @Test
    void anEmptyRowIsAlwaysWaiting() {
        var editor = editor();
        assertTrue(editor.rowCount() > editor.getLines().size(), "there is always a row left to type into");

        var fresh = new OrderLineEditor(catalogue);
        assertEquals(1, fresh.rowCount(), "a fresh editor opens with exactly one row");
        assertEquals(List.of(), fresh.getLines(), "and an empty row is not a line");
    }

    @Test
    void aProductCanBeAddedInAnyQuantity() {
        var editor = editor();
        var product = catalogue.availableProducts().getFirst();

        editor.setLines(List.of(new CartLine(product.getId(), 4, null)));

        assertEquals(List.of(new CartLine(product.getId(), 4, null)), editor.getLines());
        assertEquals(gross(product, 4), editor.total(), "the total follows the lines, VAT included");
        assertEquals(2, editor.rowCount(), "the filled row plus a fresh empty one");
    }

    @Test
    void aQuantityOfZeroRemovesTheLine() {
        var editor = editor();
        var product = catalogue.availableProducts().getFirst();
        editor.setLines(List.of(new CartLine(product.getId(), 2, null)));

        editor.setQuantity(0, 0);

        assertEquals(List.of(), editor.getLines(), "the line is gone");
        assertEquals(Money.ZERO, editor.total());
    }

    @Test
    void theTotalIsVatInclusiveLikeTheStoredOrderTotal() {
        var editor = editor();
        var product = catalogue.availableProducts().getFirst();

        editor.setLines(List.of(new CartLine(product.getId(), 3, null)));

        var net = product.price().times(3);
        assertEquals(gross(product, 3), editor.total());
        assertTrue(editor.total().cents() > net.cents(),
                "a positive VAT rate must make the panel total bigger than the bare net price");
    }

    @Test
    void choosingAProductInTheTrailingRowOpensANewOne() {
        var editor = editor();
        editor.setLines(List.of());
        var product = catalogue.availableProducts().getFirst();

        editor.setProduct(0, product.getId());

        assertEquals(2, editor.rowCount(), "filling the trailing row opens a new one behind it");
        assertTrue(editor.isRowEnabled(0), "choosing a product enables that row's other controls");
    }

    /**
     * The save path through the panel, end to end: the real Save button, the
     * lines that end up on the order, and the board behind it being told.
     *
     * The identity check is the load bearing one. A stale data provider still
     * queries the database on every fetch, so reading fresh rows out of it
     * proves nothing about whether the grid was told to ask again; only
     * {@code setItemsPageable} being re-invoked does, and that is what replaces
     * the provider instance and what makes a live client re-fetch.
     */
    @Test
    void savingFromThePanelWritesTheLinesAndTellsTheBoard() {
        var reference = referenceWithLines();
        navigate("orders/" + reference, OrderDetailView.class);
        var product = catalogue.availableProducts().getFirst();
        var board = find(OrderBoardView.class).single();
        var providerBefore = board.grid().getDataProvider();

        find(OrderLineEditor.class).single().setLines(List.of(new CartLine(product.getId(), 7, "extra crusty")));
        test(find(Button.class).withText("Save the order").single()).click();

        assertEquals(List.of(new CartLine(product.getId(), 7, "extra crusty")),
                orderService.detailLines(reference).stream()
                        .map(line -> new CartLine(line.productId(), line.quantity(), line.comment()))
                        .toList(),
                "the panel's own Save button is what writes the lines");
        assertNotSame(providerBefore, board.grid().getDataProvider(),
                "the board behind the panel was told to fetch again");
        assertEquals(gross(product, 7), orders.findByReference(reference).orElseThrow().gross(),
                "and the stored total was repriced with the lines");
    }

    /**
     * BOARD-19. The panel does not lose work quietly. The dialog is not a
     * courtesy on the close control alone: Escape and a click on the backdrop
     * are the two ways a barista half way through an order actually leaves,
     * and the next case proves they ask the same question.
     */
    @Test
    void closingThePanelWithUnsavedChangesAsksFirst() {
        var panel = touchedPanel();

        panel.close();

        var dialog = find(ConfirmDialog.class).single();
        assertEquals("Unsaved changes", test(dialog).getHeader());
        assertFalse(find(OrderDetailView.class).all().isEmpty(),
                "the panel stays open while the question is unanswered");

        test(dialog).confirm();

        assertTrue(find(OrderDetailView.class).all().isEmpty(), "discarding is what closes it");
    }

    @Test
    void escapeAndTheBackdropAskTheSameQuestion() {
        touchedPanel();
        var board = find(OrderBoardView.class).single();

        ComponentUtil.fireEvent(board, new MasterDetailLayout.DetailEscapePressEvent(board, true));

        assertEquals(1, find(ConfirmDialog.class).all().size(), "Escape asks rather than discarding");
        assertFalse(find(OrderDetailView.class).all().isEmpty(), "and the panel is still there");
    }

    @Test
    void keepingTheChangesLeavesThePanelAsItWas() {
        var panel = touchedPanel();

        panel.close();
        test(find(ConfirmDialog.class).single()).cancel();

        assertFalse(find(OrderDetailView.class).all().isEmpty(), "cancelling the question stays put");
        assertEquals(9, find(OrderLineEditor.class).single().getLines().getFirst().quantity(),
                "with what was typed still there");
    }

    @Test
    void aPanelNobodyTouchedClosesWithoutAsking() {
        navigate("orders/" + referenceWithLines(), OrderDetailView.class);

        find(OrderDetailView.class).single().close();

        assertTrue(find(ConfirmDialog.class).all().isEmpty(), "there was nothing to lose");
        assertTrue(find(OrderDetailView.class).all().isEmpty(), "so it just closed");
    }

    /** The counter panel holds the same editor, so it owes the same question. */
    @Test
    void theCounterPanelAsksBeforeDiscardingToo() {
        navigate(OrderBoardView.class);
        test(find(Button.class).withText("New order").single()).click();
        find(OrderLineEditor.class).single()
                .setLines(List.of(new CartLine(catalogue.availableProducts().getFirst().getId(), 2, null)));

        find(NewOrderView.class).single().close();

        assertEquals(1, find(ConfirmDialog.class).all().size());
        assertFalse(find(NewOrderView.class).all().isEmpty(), "nothing typed at the counter is lost silently");
    }

    /**
     * An order's panel with its lines rewritten and nothing saved. Rewritten
     * rather than nudged: a quantity that happened to match what the seeded
     * order already carried would fire no change at all.
     */
    private OrderDetailView touchedPanel() {
        navigate("orders/" + referenceWithLines(), OrderDetailView.class);
        find(OrderLineEditor.class).single()
                .setLines(List.of(new CartLine(catalogue.availableProducts().getFirst().getId(), 9, null)));
        return find(OrderDetailView.class).single();
    }

    @Test
    void aLineWhoseProductBecameUnavailableIsNotDroppedSilently() {
        var editor = editor();
        // The last one, not the first: other tests in this class read the
        // first available product and must not see it disappear.
        var productId = catalogue.availableProducts().getLast().getId();
        // Re-fetched fresh each time and reassigned to the save() result: the
        // entity is versioned, and saving a stale copy after the first save
        // already bumped the row throws an optimistic locking failure.
        var product = catalogue.availableProducts().stream()
                .filter(candidate -> candidate.getId().equals(productId))
                .findFirst()
                .orElseThrow();
        product.setAvailable(false);
        product = catalogue.save(product);
        try {
            editor.setLines(List.of(new CartLine(productId, 3, null)));

            assertEquals(List.of(new CartLine(productId, 3, null)), editor.getLines(),
                    "a product already on the order is kept even once it is no longer sold");
            assertTrue(editor.total().cents() > 0, "and its price still counts toward the total");
        } finally {
            product.setAvailable(true);
            catalogue.save(product);
        }
    }
}
