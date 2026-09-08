package com.vaadin.bakery.assistant.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.ordering.Channel;
import com.vaadin.bakery.ordering.DaySlotLoad;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.PickupLocation;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.bakery.ordering.SlotService;
import com.vaadin.bakery.ordering.ui.SlotPicker;
import com.vaadin.bakery.people.CustomerRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * AI-10 through the view: with no licence the counter still works, because the
 * form is still filled by hand, and nothing pretends otherwise.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
// One test below places a real order through the UI, which cannot be rolled
// back with a test transaction, so the context and its in memory database are
// rebuilt afterwards and the dataset checks keep seeing the dataset.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PhoneOrderBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private CatalogueService catalogue;
    @Autowired
    private OrderRepository orders;
    @Autowired
    private PickupLocationRepository locations;
    @Autowired
    private SlotService slots;
    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    /**
     * The first selectable day at or after the given lead time, for a
     * deterministic slot. Never today, because {@code SlotService.nextFreeTime}
     * only filters by the current time of day when the chosen date equals
     * {@code today()}: starting the search at least a day out keeps the chosen
     * date, and the time this test picks, independent of the clock.
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
    void theViewOpensWhicheverProviderIsRunning() {
        var view = navigate(PhoneOrderView.class);

        assertEquals(0, view.meter().turns(), "nothing has been asked yet");
    }

    @Test
    void theMeterCountsWhatEachAnswerCost() {
        var view = navigate(PhoneOrderView.class);

        view.meter().record(248, "stop");
        view.meter().record(310, "max_tokens");

        assertEquals(2, view.meter().turns());
        assertEquals(558, view.meter().tokens());
        assertTrue(view.meter().wasTruncated(), "and the last one was cut off, which the user is told");
    }

    /**
     * BOARD-21 through the view: put the lines in, fill the customer, pick a
     * slot, save, and confirm a {@code Channel.PHONE} order actually lands in
     * the database. The lines go in through the editor rather than through a
     * parser button, because there is no parser button: with no model, a
     * barista fills this form the way they fill any other.
     * {@code OrderCreationBrowserlessTest} proves the counter half of this
     * task the same way; nothing before this test clicked this view's save
     * button.
     */
    @Test
    void savingRecordsAPhoneOrderThroughTheEditor() {
        var product = catalogue.availableProducts().stream()
                .filter(candidate -> candidate.getLeadTimeDays() == 0)
                .findFirst()
                .orElseThrow();
        var location = locations.findByActiveTrueOrderByNameAsc().getFirst();
        var date = pickableDate(location, 0);

        navigate(PhoneOrderView.class);

        var view = find(PhoneOrderView.class).single();
        view.editor().setLines(List.of(new CartLine(product.getId(), 2, null)));

        test(find(TextField.class).withLabel("First name").single()).setValue("Marta");
        test(find(TextField.class).withLabel("Last name").single()).setValue("Somoza");
        test(find(EmailField.class).single()).setValue("marta.phone@example.test");

        var picker = find(SlotPicker.class).single();
        test(picker.datePicker()).setValue(date);

        long before = orders.count();
        test(find(Button.class).withText("Save").single()).click();

        assertEquals(before + 1, orders.count());
        var created = orders.findAll().stream()
                .filter(candidate -> candidate.getCustomer().getEmail().equals("marta.phone@example.test"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the phone order was never recorded"));
        assertEquals(Channel.PHONE, created.getChannel());
    }

    /**
     * Mirrors {@code OrderCreationBrowserlessTest.savingWithNoPickupSlotChosenIsRefusedWithoutThrowing}:
     * before the fix, {@code PhoneOrderView.java:107} passed a null date
     * straight into {@code OrderService.place}, which throws an uncaught
     * {@code NullPointerException} rather than a {@code DomainException} the
     * catch block can show as a message. Clicking Save here, with lines and a
     * customer but no chosen slot, must not throw, and must not create the
     * customer or place the order either.
     */
    @Test
    void savingWithNoPickupSlotChosenIsRefusedWithoutThrowing() {
        var product = catalogue.availableProducts().getFirst();

        navigate(PhoneOrderView.class);

        var view = find(PhoneOrderView.class).single();
        view.editor().setLines(List.of(new CartLine(product.getId(), 1, null)));

        test(find(TextField.class).withLabel("First name").single()).setValue("Petra");
        test(find(TextField.class).withLabel("Last name").single()).setValue("Sola");
        test(find(EmailField.class).single()).setValue("petra.phone.noslot@example.test");
        // The picker's date and time are left untouched on purpose.

        long ordersBefore = orders.count();
        long customersBefore = customerRepository.count();

        test(find(Button.class).withText("Save").single()).click();

        assertEquals(ordersBefore, orders.count(), "no order was placed without a chosen slot");
        assertEquals(customersBefore, customerRepository.count(), "no customer was created either");
    }

}
