package com.vaadin.bakery.assistant.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.bakery.ordering.Channel;
import com.vaadin.bakery.ordering.DaySlotLoad;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.PickupLocation;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.bakery.ordering.SlotService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.ai.provider.LLMProvider;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

/**
 * AI-04 and AI-05, and every rule of AC2.
 *
 * These assert against the tools the model is actually given, which is the only
 * honest place to check a guardrail: a rule that lives in a prompt is a request,
 * and a rule that lives in a tool is a rule.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
// Filling a slot to prove it can be full means placing real orders, which a
// test transaction cannot roll back, so the context is rebuilt afterwards.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FormFillingGuardrailsBrowserlessTest extends SpringBrowserlessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private SlotService slots;
    @Autowired
    private PickupLocationRepository locations;
    @Autowired
    private CatalogueService catalogue;
    @Autowired
    private OrderService orderService;
    @Autowired
    private Clock clock;

    private int filler;

    private record Slot(LocalDate date, LocalTime time) {
    }

    /** The first day this location will actually take an order. */
    private LocalDate firstSelectableDay(PickupLocation location) {
        return slots.load(location, LocalDate.now(clock).plusDays(2), LocalDate.now(clock).plusDays(30), 0)
                .stream()
                .filter(DaySlotLoad::isSelectable)
                .findFirst()
                .orElseThrow()
                .date();
    }

    private Slot freeSlot(PhoneOrderView view) {
        var location = view.picker().getLocation();
        var date = firstSelectableDay(location);
        return new Slot(date, slots.nextFreeTime(location, date).orElseThrow());
    }

    /**
     * A slot that really is full, filled here rather than hunted for in the
     * dataset: a guardrail test that passes only when the seeded data happens
     * to cooperate is not a test.
     */
    private Slot fullSlot(PhoneOrderView view) {
        // The smallest location, so this costs six orders rather than twelve.
        var smallest = locations.findByActiveTrueOrderByNameAsc().stream()
                .min(Comparator.comparingInt(PickupLocation::getSlotCapacity))
                .orElseThrow();
        view.picker().locationSelect().setValue(smallest);

        var date = firstSelectableDay(smallest);
        var time = slots.nextFreeTime(smallest, date).orElseThrow();
        var sameDay = catalogue.availableProducts().stream()
                .filter(product -> product.getLeadTimeDays() == 0)
                .findFirst()
                .orElseThrow();

        while (slots.hasCapacity(smallest, date, time)) {
            orderService.place(List.of(new CartLine(sameDay.getId(), 1, null)), "Full", "Slot",
                    "full.slot." + filler++ + "@example.test", "+34 600 000 000", smallest, date, time,
                    Channel.PHONE, null, null);
        }
        return new Slot(date, time);
    }

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    private PhoneOrderView open() {
        navigate(PhoneOrderView.class);
        return find(PhoneOrderView.class).single();
    }

    private LLMProvider.ToolSpec tool(PhoneOrderView view, String name) {
        return view.allTools().stream()
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No tool called " + name + ", only "
                        + view.allTools().stream().map(LLMProvider.ToolSpec::getName).toList()));
    }

    private String formState(PhoneOrderView view) {
        return tool(view, "get_form_state").execute(JSON.createObjectNode());
    }

    /** AC2: it cannot write into a hidden field, because it cannot see one. */
    @Test
    void theInternalNoteIsInvisibleToTheModel() {
        var view = open();

        var state = formState(view);

        // The controller names fields with generated ids, so the label in the
        // description is the only thing to look for.
        assertFalse(state.contains("Internal note"),
                "a staff only note is not something the model gets an opinion about: " + state);
        assertTrue(state.contains("First name"), "while the fields it may fill are there: " + state);
        assertTrue(state.contains("Phone"), state);
    }

    /** AI-05. A slot the bakery cannot serve comes back named, with the next one. */
    @Test
    void aFullSlotIsRejectedWithTheSlotNamed() {
        var view = open();
        var propose = tool(view, "propose_pickup_slot");
        var full = fullSlot(view);

        var failure = assertThrows(RuntimeException.class, () -> propose.execute(JSON.createObjectNode()
                .put("date", full.date().toString())
                .put("time", full.time().toString())));

        assertTrue(failure.getMessage().contains(full.date().toString()), failure.getMessage());
        assertTrue(failure.getMessage().contains(full.time().toString()), failure.getMessage());
        assertTrue(failure.getMessage().contains("fully booked"), failure.getMessage());
        assertNull(view.picker().getDate(), "and nothing was written");
    }

    /** A slot that is free is accepted, or the test above proves nothing. */
    @Test
    void aFreeSlotIsAccepted() {
        var view = open();
        var free = freeSlot(view);

        var answer = tool(view, "propose_pickup_slot").execute(JSON.createObjectNode()
                .put("date", free.date().toString())
                .put("time", free.time().toString()));

        assertTrue(answer.contains(free.date().toString()), answer);
        assertEquals(free.date(), view.picker().getDate());
        assertEquals(free.time(), view.picker().getTime());
    }

    /** AC2: it cannot create a customer, because the field only accepts rows that exist. */
    @Test
    void theCustomerFieldOnlyOffersPeopleTheBakeryAlreadyKnows() {
        var view = open();

        var options = tool(view, "query_field_options").execute(JSON.createObjectNode()
                .put("field", customerFieldId(view))
                .put("filter", "zzz-nobody-is-called-this")
                .put("limit", 10));

        assertFalse(options.toLowerCase().contains("zzz-nobody"),
                "a search that matches nobody returns nobody, it does not invent one: " + options);
    }

    /** AI-04. A product that does not exist is refused, and the refusal names what does. */
    @Test
    void aProductThatDoesNotExistIsRejectedAndReportedBack() {
        var view = open();
        var add = tool(view, "add_order_line");

        var failure = assertThrows(RuntimeException.class, () -> add.execute(JSON.createObjectNode()
                .put("product", "unicorn tart")
                .put("quantity", 2)));

        assertTrue(failure.getMessage().contains("unicorn tart"), failure.getMessage());
        assertTrue(failure.getMessage().contains("The catalogue holds"),
                "and it says what the model could have said instead: " + failure.getMessage());
        assertEquals(List.of(), view.editor().getLines(), "nothing was written");
    }

    /** AC2. A quantity outside the range the field allows is refused the same way. */
    @Test
    void aQuantityOutOfRangeIsRejected() {
        var view = open();
        var add = tool(view, "add_order_line");
        var sold = view.anyProductName();

        assertThrows(RuntimeException.class, () -> add.execute(JSON.createObjectNode()
                .put("product", sold).put("quantity", 0)));
        assertThrows(RuntimeException.class, () -> add.execute(JSON.createObjectNode()
                .put("product", sold).put("quantity", 100)));

        assertEquals(List.of(), view.editor().getLines(), "neither was written");
    }

    /** And the same tool accepts what it should, or the two above prove nothing. */
    @Test
    void aRealProductInRangeIsAccepted() {
        var view = open();

        var answer = tool(view, "add_order_line").execute(JSON.createObjectNode()
                .put("product", view.anyProductName()).put("quantity", 3));

        assertTrue(answer.contains("3"), answer);
        assertEquals(1, view.editor().getLines().size());
        assertEquals(3, view.editor().getLines().getFirst().quantity());
    }

    private String customerFieldId(PhoneOrderView view) {
        var state = formState(view);
        // The controller decides the ids, so read one back rather than assuming.
        var node = JSON.readTree(state);
        for (var field : node.path("fields")) {
            if (field.path("description").asString("").contains("existing customer")) {
                return field.path("id").asString("");
            }
        }
        throw new AssertionError("The customer field is not in the form state: " + state);
    }
}
