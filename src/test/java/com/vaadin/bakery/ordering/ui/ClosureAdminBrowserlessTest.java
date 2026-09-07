package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.ordering.PickupClosure;
import com.vaadin.bakery.ordering.PickupClosureRepository;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.bakery.ordering.SlotService;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * ADM-12 and ADM-13. A closure has to reach the public calendar, and it has to
 * say what it would strand on the way.
 *
 * The warning is the part {@code Crud} knows nothing about. It moved from a
 * button handler to a save listener when this view adopted the component, and
 * a rule that survives a refactor only because nobody exercised it is not a
 * rule.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ClosureAdminBrowserlessTest extends com.vaadin.browserless.SpringBrowserlessTest {

    @Autowired
    private PickupClosureRepository closures;

    @Autowired
    private PickupLocationRepository locations;

    @Autowired
    private SlotService slots;

    @Autowired
    private Clock clock;

    @Autowired
    private com.vaadin.bakery.ordering.OrderRepository orders;

    /** ADM-12. Saving over existing orders asks, and lists what it would hit. */
    @Test
    void aClosureOverExistingOrdersWarnsAndNamesThem() {
        com.vaadin.bakery.TestLogin.asAdmin();
        var busy = orders.findAll().stream()
                .filter(order -> order.getState().isOpen())
                .filter(order -> !order.getPickupDate().isBefore(LocalDate.now(clock)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the dataset has no open order in the future"));

        navigate(ClosureAdminView.class);
        var view = find(ClosureAdminView.class).single();

        var closure = new PickupClosure();
        closure.setDate(busy.getPickupDate());
        closure.setLocation(busy.getPickupLocation());
        closure.setReason("Test closure");
        long before = closures.count();
        view.saveUnlessItStrandsOrders(closure);

        // Nothing is saved yet: the administrator is being asked first.
        var dialogs = find(com.vaadin.flow.component.confirmdialog.ConfirmDialog.class).all();
        assertFalse(dialogs.isEmpty(), "a closure over open orders asks before it saves");
        // ConfirmDialog has no getText, and its text is not in the element tree
        // browserless walks, so this reads the property the component set.
        var said = String.valueOf(dialogs.getFirst().getElement().getProperty("message", ""))
                + dialogs.getFirst().getElement().getTextRecursively();
        assertTrue(said.contains(busy.getReference()), "and names the order it would strand: [" + said + "]");
        assertEquals(before, closures.count(), "and saves nothing until it is answered");
    }

    @Test
    void addingAClosureDisablesThatDayInThePublicCalendar() {
        var location = locations.findAll().getFirst();
        var day = slots.load(location, LocalDate.now(clock).plusDays(5), LocalDate.now(clock).plusDays(25), 0)
                .stream()
                .filter(candidate -> candidate.isSelectable())
                .findFirst()
                .orElseThrow()
                .date();

        var closure = new PickupClosure();
        closure.setDate(day);
        closure.setReason("Annual deep clean");
        closure.setKind(PickupClosure.ClosureKind.MAINTENANCE);
        closures.save(closure);

        var after = slots.load(location, day, day, 0).getFirst();
        assertFalse(after.isSelectable(), "the day is gone from the calendar");
        assertEquals("Annual deep clean", after.closedReason(), "and it says why");
    }

    @Test
    void aClosureForOneLocationLeavesTheOthersOpen() {
        var all = locations.findAll();
        var closed = all.getFirst();
        var open = all.get(1);
        var day = slots.load(open, LocalDate.now(clock).plusDays(6), LocalDate.now(clock).plusDays(25), 0)
                .stream()
                .filter(candidate -> candidate.isSelectable())
                .findFirst()
                .orElseThrow()
                .date();

        var closure = new PickupClosure();
        closure.setDate(day);
        closure.setLocation(closed);
        closure.setReason("Oven maintenance");
        closure.setKind(PickupClosure.ClosureKind.MAINTENANCE);
        closures.save(closure);

        assertFalse(slots.load(closed, day, day, 0).getFirst().isSelectable());
        assertTrue(slots.load(open, day, day, 0).getFirst().isSelectable(),
                "the other shop is still serving");
    }
}
