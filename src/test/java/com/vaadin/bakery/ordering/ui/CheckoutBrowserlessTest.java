package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.bakery.ordering.CartSignals;
import com.vaadin.bakery.ordering.CheckoutState;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.ordering.PickupLocation;
import com.vaadin.bakery.ordering.SlotService;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.QueryParameters;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/** CHK-01, CHK-02, CHK-03, CHK-06, CHK-12 and the step view contract. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
// This one places real orders through the UI, which cannot be rolled back with
// a test transaction, so the context and its in memory database are rebuilt
// afterwards and the dataset checks keep seeing the dataset.
@org.springframework.test.annotation.DirtiesContext(
        classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class CheckoutBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ProductRepository products;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private SlotService slots;

    @Autowired
    private PickupLocationRepository locations;

    @Autowired
    private Clock clock;

    private CartSignals cart() {
        return context.getBean(CartSignals.class);
    }

    private CheckoutState state() {
        return context.getBean(CheckoutState.class);
    }

    private void fillContact(String email) {
        navigate(CheckoutContactView.class);
        test(find(TextField.class).withLabel("First name").single()).setValue("Ana");
        test(find(TextField.class).withLabel("Last name").single()).setValue("Ruiz");
        test(find(EmailField.class).single()).setValue(email);
        test(find(TextField.class).withLabel("Phone").single()).setValue("+34 600 111 222");
    }

    private record Slot(PickupLocation location, LocalDate date, LocalTime time) {
    }

    private Slot chooseSlot() {
        var location = locations.findByActiveTrueOrderByNameAsc().getFirst();
        var date = slots.load(location, LocalDate.now(clock).plusDays(3), LocalDate.now(clock).plusDays(20), 0)
                .stream()
                .filter(day -> day.isSelectable())
                .findFirst()
                .orElseThrow()
                .date();
        var time = slots.nextFreeTime(location, date).orElseThrow();
        state().location().set(location);
        state().date().set(date);
        state().time().set(time);
        return new Slot(location, date, time);
    }

    /** How many orders that slot is carrying right now. */
    private int booked(Slot slot) {
        return slots.options(slot.location(), slot.date()).stream()
                .filter(option -> option.time().equals(slot.time()))
                .findFirst()
                .orElseThrow()
                .booked();
    }

    @Test
    void aHalfFilledContactStepIsAllowedToLeave() {
        navigate(CheckoutContactView.class);
        test(find(TextField.class).withLabel("First name").single()).setValue("Ana");

        // The draft rules let this pass, the submit rules do not.
        assertFalse(state().isContactValid(), "an order still cannot be placed");
        assertTrue(find(TextField.class).withLabel("First name").single().isInvalid() == false,
                "and nothing is flagged as an error while typing");
    }

    private boolean showing(Class<? extends com.vaadin.flow.component.Component> view) {
        return !find(view).all().isEmpty();
    }

    /**
     * CHK-12. Forward asks whether what is there is well formed. It does not ask
     * whether it is finished, which is the review step's job, and it does not ask
     * anything at all on the way back.
     */
    @Test
    void everyStepOffersBackAndContinue() {
        navigate(CheckoutContactView.class);
        assertFalse(find(Button.class).withText("Back").all().isEmpty(), "the first step can return to the cart");
        assertFalse(find(Button.class).withText("Continue").all().isEmpty(), "and go on");

        navigate(CheckoutSlotView.class);
        assertFalse(find(Button.class).withText("Back").all().isEmpty(), "so can the slot step");
        assertFalse(find(Button.class).withText("Continue").all().isEmpty());

        navigate(CheckoutReviewView.class);
        assertFalse(find(Button.class).withText("Back").all().isEmpty(), "and the review step");
    }

    @Test
    void continueRefusesAMalformedFieldOnTheStepItLeaves() {
        navigate(CheckoutContactView.class);
        test(find(TextField.class).withLabel("First name").single()).setValue("Ana");
        test(find(EmailField.class).single()).setValue("ana@@example");

        test(find(Button.class).withText("Continue").single()).click();

        assertTrue(showing(CheckoutContactView.class), "a malformed email does not get to leave the step");
        assertFalse(showing(CheckoutSlotView.class));
        // A red box with nothing written in it is not a refusal, it is a puzzle.
        assertFalse(find(EmailField.class).single().getErrorMessage().isBlank(),
                "and it says what is wrong with it");
    }

    @Test
    void continueLetsAnUnfinishedStepThrough() {
        navigate(CheckoutContactView.class);
        test(find(TextField.class).withLabel("First name").single()).setValue("Ana");
        // No surname, no email, no phone: incomplete is not the same as wrong,
        // and the review step is where completeness is decided.

        test(find(Button.class).withText("Continue").single()).click();

        assertTrue(showing(CheckoutSlotView.class), "an unfinished step is allowed to move on");
    }

    @Test
    void backValidatesNothing() {
        navigate(CheckoutContactView.class);
        test(find(EmailField.class).single()).setValue("ana@@example");

        test(find(Button.class).withText("Back").single()).click();

        assertTrue(showing(CartView.class), "the way back is never blocked");
    }

    /**
     * A refresh is a new view instance over the same session, which is what
     * navigating to the same route again is here. If the values came back, they
     * were never in the view.
     */
    @Test
    void aRefreshResumesTheSameStepWithItsValues() {
        fillContact("ana.refresh@example.com");

        navigate(CheckoutContactView.class);

        assertEquals("Ana", find(TextField.class).withLabel("First name").single().getValue());
        assertEquals("ana.refresh@example.com", find(EmailField.class).single().getValue());
    }

    @Test
    void valuesSurviveGoingBackAndForth() {
        fillContact("ana.checkout@example.com");
        navigate(CheckoutSlotView.class);
        navigate(CheckoutContactView.class);

        assertEquals("Ana", find(TextField.class).withLabel("First name").single().getValue());
        assertEquals("ana.checkout@example.com", find(EmailField.class).single().getValue());
    }

    @Test
    void placingIsRefusedUntilEveryStepIsSubmitValid() {
        navigate(CheckoutReviewView.class);
        var place = find(Button.class).withText("Place the order").single();
        assertFalse(place.isEnabled(), "nothing has been entered yet");
    }

    @Test
    void aCompleteCheckoutCreatesTheOrder() {
        var croissant = products.findBySlug("butter-croissant").orElseThrow();
        navigate(CheckoutContactView.class);
        cart().add(croissant.getId(), 2, "Sliced");
        fillContact("ana.complete@example.com");
        var slot = chooseSlot();
        int bookedBefore = booked(slot);

        navigate(CheckoutReviewView.class);
        var place = find(Button.class).withText("Place the order").single();
        assertTrue(place.isEnabled(), "everything is filled in");
        long before = orders.count();
        test(place).click();

        assertEquals(before + 1, orders.count());
        var reference = orders.findAll().stream()
                .filter(candidate -> candidate.getCustomer().getEmail().equals("ana.complete@example.com"))
                .findFirst()
                .orElseThrow()
                .getReference();
        // Items are lazy, so read them back through the graph that fetches them.
        var order = orders.findByReference(reference).orElseThrow();
        assertEquals(2, order.getItems().getFirst().getQuantity());
        assertNotNull(order.getTrackingToken());
        assertEquals(0, cart().count(), "the basket is emptied once the order exists");

        // The slot is booked, not merely remembered on the order.
        assertEquals(slot.location(), order.getPickupLocation());
        assertEquals(slot.date(), order.getPickupDate());
        assertEquals(slot.time(), order.getPickupTime());
        assertEquals(bookedBefore + 1, booked(slot), "the slot carries one order more than it did");

        // An order starts with its own arrival written down.
        assertFalse(order.getHistory().isEmpty(), "there is a first history entry");
        assertEquals(OrderState.NEW, order.getHistory().getFirst().getNewState());
        assertEquals(OrderState.NEW, order.getState());

        // The totals are the lines, at the price the lines were snapshotted at,
        // and not a number that travelled from the browser.
        var line = order.getItems().getFirst();
        assertEquals(croissant.price().times(2), line.net(), "the price was snapshotted");
        assertEquals(line.net(), order.net());
        assertEquals(line.gross(), order.gross());
        assertEquals(order.net().plus(order.vat()), order.gross());
    }

    /** CHK-13. The confirmation, and that the link it prints actually opens the order. */
    @Test
    void theConfirmationShowsTheReferenceAndAWorkingTrackingLink() {
        var bun = products.findBySlug("cinnamon-bun").orElseThrow();
        navigate(CheckoutContactView.class);
        cart().add(bun.getId(), 1, null);
        fillContact("nuria.confirm@example.com");
        chooseSlot();

        navigate(CheckoutReviewView.class);
        test(find(Button.class).withText("Place the order").single()).click();

        assertTrue(showing(CheckoutDoneView.class), "placing lands on the confirmation, not on tracking");
        var placed = state().placed().peek();
        assertNotNull(placed, "the session remembers what was just placed");

        var text = find(CheckoutDoneView.class).single().getElement().getTextRecursively();
        assertTrue(text.contains(placed.reference()), "the reference is on the page: " + text);

        var link = find(TextField.class).withLabel("Your tracking link").single().getValue();
        assertTrue(link.contains(placed.reference()), link);
        assertTrue(link.contains(placed.token()), link);

        // And the link is not decoration: it opens the order.
        UI.getCurrent().navigate("track/" + placed.reference(),
                QueryParameters.of("t", placed.token()));
        assertTrue(find(TrackingView.class).single().getElement().getTextRecursively()
                .contains(placed.reference()), "the link reaches the order");
    }

    @Test
    void theConfirmationHasNothingToSayWhenNothingWasPlaced() {
        state().clear();

        // Not navigate(Class): that one asserts which view it landed on, and the
        // whole point here is that it lands somewhere else.
        UI.getCurrent().navigate("checkout/done");

        assertFalse(showing(CheckoutDoneView.class), "there is nothing to confirm");
        assertTrue(showing(CartView.class), "so the basket is where the visitor lands");
    }

    @Test
    void theSameEmailTwiceReusesTheCustomer() {
        var product = products.findBySlug("cinnamon-bun").orElseThrow();
        for (int i = 0; i < 2; i++) {
            navigate(CheckoutContactView.class);
            cart().add(product.getId(), 1, null);
            fillContact("repeat.customer@example.com");
            chooseSlot();
            navigate(CheckoutReviewView.class);
            test(find(Button.class).withText("Place the order").single()).click();
        }

        var placed = orders.findAll().stream()
                .filter(order -> order.getCustomer().getEmail().equals("repeat.customer@example.com"))
                .toList();
        assertEquals(2, placed.size(), "two orders");
        assertEquals(1, placed.stream().map(order -> order.getCustomer().getId()).distinct().count(),
                "one customer");
    }
}
