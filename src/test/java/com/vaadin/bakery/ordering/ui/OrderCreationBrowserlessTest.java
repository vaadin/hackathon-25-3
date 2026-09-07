package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.bakery.ordering.Channel;
import com.vaadin.bakery.ordering.DaySlotLoad;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.PickupLocation;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.bakery.ordering.SlotService;
import com.vaadin.bakery.people.CustomerRepository;
import com.vaadin.bakery.people.CustomerService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/** BOARD-20, BOARD-21 and BOARD-22. The bakery takes orders three ways and records all three. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderCreationBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private OrderService orders;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CatalogueService catalogue;
    @Autowired
    private CustomerService customers;
    @Autowired
    private PickupLocationRepository locations;
    @Autowired
    private SlotService slots;
    @Autowired
    private CustomerRepository customerRepository;

    private com.vaadin.bakery.ordering.Order take(Channel channel) {
        var product = catalogue.availableProducts().getFirst();
        var location = locations.findByActiveTrueOrderByNameAsc().getFirst();
        var date = slots.today().plusDays(7);
        var time = slots.nextFreeTime(location, date).orElseThrow();
        var customer = customers.findOrCreate("Ada", "Nord", "ada@example.test", "+34600000000");
        return orders.place(List.of(new CartLine(product.getId(), 3, null)), customer, location, date, time,
                channel, null, null);
    }

    /**
     * The first selectable day at or after the given lead time, for a
     * deterministic slot. Never today: {@code SlotService.nextFreeTime} only
     * filters by the current time of day when the chosen date equals
     * {@code today()}, so a test that could land on today would go red once
     * every slot for today has passed, depending purely on when it happens to
     * run. Starting the search at least a day out makes the chosen date, and
     * therefore the time this test picks, independent of the clock.
     */
    private java.time.LocalDate pickableDate(PickupLocation location, int leadTimeDays) {
        var from = slots.today().plusDays(Math.max(leadTimeDays, 1));
        return slots.load(location, from, from.plusDays(30), leadTimeDays).stream()
                .filter(DaySlotLoad::isSelectable)
                .findFirst()
                .orElseThrow()
                .date();
    }

    @Test
    void anOrderTakenAtTheCounterIsRecordedAsOne() {
        var order = take(Channel.COUNTER);
        assertEquals(Channel.COUNTER, orders.byReference(order.getReference()).orElseThrow().getChannel());
    }

    @Test
    void anOrderTakenOnTheTelephoneIsRecordedAsOne() {
        var order = take(Channel.PHONE);
        assertEquals(Channel.PHONE, orders.byReference(order.getReference()).orElseThrow().getChannel());
    }

    @Test
    void anOrderWithNoLinesIsRefused() {
        var location = locations.findByActiveTrueOrderByNameAsc().getFirst();
        var date = slots.today().plusDays(7);
        var time = slots.nextFreeTime(location, date).orElseThrow();
        var customer = customers.findOrCreate("Ada", "Nord", "ada@example.test", "+34600000000");

        var refusal = assertThrows(DomainException.class, () -> orders.place(List.of(), customer, location,
                date, time, Channel.COUNTER, null, null));

        assertTrue(refusal.translationKey().contains("empty"), "it says why, got " + refusal.translationKey());
    }

    /**
     * BOARD-20/21/22 at the UI level. Nothing offers taking a counter order
     * until this task, so this is the red test: before {@code NewOrderView}
     * exists and the board's toolbar offers it, this fails with
     * {@code NoSuchElementException: No visible Button ... matching Button and
     * text='New order'}. Once the button and view exist, it still has to save
     * through {@code OrderService.place} and land a {@code Channel.COUNTER}
     * order for the assertions below to pass.
     */
    @Test
    void theBoardOffersTakingACounterOrderAndSavingRecordsIt() {
        var product = catalogue.availableProducts().stream()
                .filter(candidate -> candidate.getLeadTimeDays() == 0)
                .findFirst()
                .orElseThrow();
        var location = locations.findByActiveTrueOrderByNameAsc().getFirst();
        var date = pickableDate(location, 0);

        TestLogin.asBarista();
        navigate(OrderBoardView.class);
        test(find(Button.class).withText("New order").single()).click();

        var editor = find(OrderLineEditor.class).single();
        editor.setLines(List.of(new CartLine(product.getId(), 2, null)));

        var picker = find(SlotPicker.class).single();
        // The location is already selected: the picker defaults to the first
        // active one, which is exactly the one this test computed the date for.
        test(picker.datePicker()).setValue(date);

        test(find(TextField.class).withLabel("First name").single()).setValue("Rosa");
        test(find(TextField.class).withLabel("Last name").single()).setValue("Vidal");
        test(find(EmailField.class).single()).setValue("rosa.counter@example.test");
        test(find(TextField.class).withLabel("Phone").single()).setValue("+34611222333");

        var board = find(OrderBoardView.class).single();
        var providerBefore = board.grid().getDataProvider();

        long before = orderRepository.count();
        test(find(Button.class).withText("Save the order").single()).click();

        assertEquals(before + 1, orderRepository.count());
        var created = orderRepository.findAll().stream()
                .filter(candidate -> candidate.getCustomer().getEmail().equals("rosa.counter@example.test"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the counter order was never recorded"));
        assertEquals(Channel.COUNTER, created.getChannel());

        // BOARD-20's second half. Flow reuses the board instance behind the
        // panel, so writing the order is not enough: without a refresh the
        // list the order was taken from is never asked again. A stale
        // provider still answers with fresh rows, because applyFilter's
        // callback queries the database live on every fetch, so reading rows
        // out of it proves nothing. Only setItemsPageable being re-invoked,
        // which replaces the provider instance, proves the board was told to
        // refresh.
        assertNotSame(providerBefore, board.grid().getDataProvider(),
                "the board behind the panel was told to fetch again");
    }

    /**
     * The whole reason the picker sits on these staff screens: a product with
     * a lead time above zero has to push the earliest choosable day forward,
     * and that only happens because {@code editor.addLinesChangeListener}
     * calls {@code picker.refresh()}. The happy-path test above only ever uses
     * a zero lead time product, so it cannot exercise this coupling.
     */
    @Test
    void addingALeadTimeProductPushesTheEarliestPickupDateForward() {
        var product = catalogue.availableProducts().stream()
                .filter(candidate -> candidate.getLeadTimeDays() > 0)
                .findFirst()
                .orElseThrow();

        TestLogin.asBarista();
        navigate(OrderBoardView.class);
        test(find(Button.class).withText("New order").single()).click();

        var editor = find(OrderLineEditor.class).single();
        var picker = find(SlotPicker.class).single();

        editor.setLines(List.of(new CartLine(product.getId(), 1, null)));

        assertEquals(slots.today().plusDays(product.getLeadTimeDays()), picker.datePicker().getMin(),
                "the calendar's earliest day follows the lead time of what is actually in the editor");
    }

    /**
     * A save with nothing in the editor must not reach {@code place} at all,
     * and, just as importantly, must not have already called
     * {@code CustomerService.findOrCreate} with whatever was typed into the
     * customer fields: that call saves or renames a {@code Customer}
     * unconditionally, so a refused save that ran it anyway would leave data
     * corruption behind even though the order itself never exists.
     */
    @Test
    void savingWithNoLinesTouchesNoCustomerAndNoOrder() {
        TestLogin.asBarista();
        navigate(OrderBoardView.class);
        test(find(Button.class).withText("New order").single()).click();

        test(find(TextField.class).withLabel("First name").single()).setValue("Nobody");
        test(find(TextField.class).withLabel("Last name").single()).setValue("Yet");
        test(find(EmailField.class).single()).setValue("nobody.yet@example.test");

        long ordersBefore = orderRepository.count();
        long customersBefore = customerRepository.count();

        // Nothing was ever added to the editor: the trailing empty row does
        // not count, exactly like OrderLineEditor.getLines() already promises.
        test(find(Button.class).withText("Save the order").single()).click();

        assertEquals(ordersBefore, orderRepository.count(), "no order was placed");
        assertEquals(customersBefore, customerRepository.count(), "no customer was created either");
        assertTrue(customerRepository.findByEmailIgnoreCase("nobody.yet@example.test").isEmpty(),
                "the typed email was never persisted");
    }

    /**
     * BOARD-20's counterpart for the picker: before this fix, saving with
     * lines and a customer but no chosen day threw an uncaught
     * {@code NullPointerException} out of {@code OrderService.place}, which a
     * barista would have seen as Vaadin's internal error screen. Clicking Save
     * here must not throw, and must not place an order either.
     */
    @Test
    void savingWithNoPickupSlotChosenIsRefusedWithoutThrowing() {
        var product = catalogue.availableProducts().getFirst();

        TestLogin.asBarista();
        navigate(OrderBoardView.class);
        test(find(Button.class).withText("New order").single()).click();

        var editor = find(OrderLineEditor.class).single();
        editor.setLines(List.of(new CartLine(product.getId(), 1, null)));

        test(find(TextField.class).withLabel("First name").single()).setValue("Petra");
        test(find(TextField.class).withLabel("Last name").single()).setValue("Sola");
        test(find(EmailField.class).single()).setValue("petra.noslot@example.test");
        // The picker's date and time are left untouched: SlotPicker only ever
        // selects a location on construction, never a day or a time.

        long ordersBefore = orderRepository.count();

        test(find(Button.class).withText("Save the order").single()).click();

        assertEquals(ordersBefore, orderRepository.count(), "no order was placed without a chosen slot");
    }

    /**
     * The everyday case the review pointed to, proven at the service level.
     * {@code CustomerService.findOrCreate} used to commit in its own
     * transaction, separate from {@code OrderService.place}'s: a save refused
     * for a reason {@code place} only discovers itself, like a product not
     * baked on the chosen day, still renamed whatever existing customer that
     * email resolved to. Resolving the customer and placing the order now
     * happen inside the one transaction {@code place} runs, so a refusal
     * rolls both back together.
     *
     * A product restricted to particular weekdays is picked from the
     * catalogue, and the date is walked forward to a day that product is not
     * baked on: {@code SlotPicker} disables days for closures, closed
     * weekdays, lead time and capacity, never for a line's own weekday
     * restriction, so this is reachable from the UI exactly as an ordinary
     * mistake, not a crafted edge case.
     */
    @Test
    void aRefusedSaveDoesNotRenameAnExistingCustomer() {
        var product = catalogue.availableProducts().stream()
                .filter(candidate -> !candidate.getAvailableWeekdays().isEmpty()
                        && candidate.getAvailableWeekdays().size() < 7)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "the seeded catalogue is expected to restrict at least one product to some weekdays"));
        var excludedDay = EnumSet.complementOf(EnumSet.copyOf(product.getAvailableWeekdays())).iterator().next();
        var location = locations.findByActiveTrueOrderByNameAsc().getFirst();
        LocalDate date = slots.today().plusDays(7);
        while (date.getDayOfWeek() != excludedDay) {
            date = date.plusDays(1);
        }
        var time = LocalTime.of(10, 0);

        var existing = customers.findOrCreate("Original", "Owner", "existing.reused@example.test", "+34600000001");
        long ordersBefore = orderRepository.count();

        var attemptedDate = date;
        var refusal = assertThrows(DomainException.class, () -> orders.place(
                List.of(new CartLine(product.getId(), 1, null)), "Somebody", "Else",
                "existing.reused@example.test", "+34699999999", location, attemptedDate, time,
                Channel.COUNTER, null, null));

        assertTrue(refusal.translationKey().contains("notOnThatDay"), "it says why, got " + refusal.translationKey());
        assertEquals(ordersBefore, orderRepository.count(), "no order was placed");

        var reloaded = customerRepository.findById(existing.getId()).orElseThrow();
        assertEquals("Original", reloaded.getFirstName(), "the existing customer's name must survive a refused save");
        assertEquals("Owner", reloaded.getLastName());
        assertEquals("+34600000001", reloaded.getPhone());
    }
}
