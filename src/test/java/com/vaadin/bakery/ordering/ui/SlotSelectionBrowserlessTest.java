package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.bakery.ordering.Channel;
import com.vaadin.bakery.ordering.DaySlotLoad;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.SlotOption;
import com.vaadin.bakery.people.CustomerService;
import com.vaadin.bakery.ordering.CartSignals;
import com.vaadin.bakery.ordering.PickupClosure;
import com.vaadin.bakery.ordering.PickupClosureRepository;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.bakery.ordering.SlotService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * CART-04, CART-05 and CART-08. The picker itself is a browser component, so
 * what is asserted here is the rule set it is configured with: which days are
 * disabled, why, and what the default time is.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
// One test here fills a slot, and a filled slot must not follow the others out.
@org.springframework.transaction.annotation.Transactional
class SlotSelectionBrowserlessTest {

    @Autowired
    private SlotService slots;

    @Autowired
    private PickupLocationRepository locations;

    @Autowired
    private PickupClosureRepository closures;

    @Autowired
    private ProductRepository products;

    @Autowired
    private Clock clock;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customers;

    @Test
    void closedWeekdaysAreNeverSelectable() {
        var bakery = locations.findAll().stream()
                .filter(location -> location.getClosedWeekdays().contains(DayOfWeek.SUNDAY))
                .findFirst()
                .orElseThrow();

        var load = slots.load(bakery, LocalDate.now(clock), LocalDate.now(clock).plusDays(30), 0);

        assertTrue(load.stream()
                .filter(day -> day.date().getDayOfWeek() == DayOfWeek.SUNDAY)
                .allMatch(day -> !day.isSelectable()), "every Sunday is closed for this location");
    }

    @Test
    void aClosureDisablesTheDayAndSaysWhy() {
        var location = locations.findAll().getFirst();
        var closure = closures.findAll().stream()
                .filter(candidate -> candidate.getLocation() == null)
                .filter(candidate -> !candidate.getDate().isBefore(LocalDate.now(clock)))
                .findFirst()
                .orElseGet(() -> {
                    var created = new PickupClosure();
                    created.setDate(LocalDate.now(clock).plusDays(9));
                    created.setReason("Test closure");
                    created.setKind(PickupClosure.ClosureKind.HOLIDAY);
                    return closures.save(created);
                });

        var load = slots.load(location, closure.getDate(), closure.getDate(), 0);

        assertEquals(1, load.size());
        assertFalse(load.getFirst().isSelectable());
        assertEquals(closure.getReason(), load.getFirst().closedReason(),
                "the reason is what the picker shows, not a generic message");
        assertEquals("closed", load.getFirst().partName());
    }

    @Test
    void leadTimeMovesTheEarliestPossibleDay() {
        var location = locations.findAll().getFirst();
        var cake = products.findAll().stream()
                .filter(product -> product.getLeadTimeDays() >= 2)
                .findFirst()
                .orElseThrow();

        var load = slots.load(location, LocalDate.now(clock), LocalDate.now(clock).plusDays(10),
                cake.getLeadTimeDays());

        var tomorrow = LocalDate.now(clock).plusDays(1);
        assertFalse(load.stream()
                .filter(day -> day.date().equals(tomorrow))
                .findFirst()
                .orElseThrow()
                .isSelectable(), "a two day cake cannot be picked up tomorrow");
    }

    @Test
    void aDayCarriesItsRemainingCapacityAndAPartName() {
        var location = locations.findAll().getFirst();
        var load = slots.load(location, LocalDate.now(clock), LocalDate.now(clock).plusDays(20), 0).stream()
                .filter(day -> day.isSelectable())
                .findFirst()
                .orElseThrow();

        assertTrue(load.capacity() > 0);
        assertTrue(load.remaining() > 0);
        assertTrue(load.partName().equals("free") || load.partName().equals("busy"), load.partName());
    }

    @Test
    void theDefaultTimeIsTheNextFreeSlot() {
        var location = locations.findAll().getFirst();
        var date = slots.load(location, LocalDate.now(clock).plusDays(3), LocalDate.now(clock).plusDays(20), 0)
                .stream()
                .filter(day -> day.isSelectable())
                .findFirst()
                .orElseThrow()
                .date();

        var next = slots.nextFreeTime(location, date);

        assertTrue(next.isPresent());
        var options = slots.options(location, date);
        var firstFree = options.stream().filter(option -> option.isAvailable()).findFirst().orElseThrow();
        assertEquals(firstFree.time(), next.get(), "the first free slot of the day, not just the first slot");
    }

    /**
     * CART-11. A time with nothing left is offered as taken, and the picker's
     * default steps over it.
     *
     * This assertion used to be that no option had a negative remaining count,
     * which {@code SlotOption.remaining} guarantees with a {@code Math.max}
     * whatever the state of the database. It passed on an empty calendar, it
     * would have passed with the rule deleted, and it never filled a slot.
     */
    @Test
    void fullSlotsAreNotOffered() {
        var location = locations.findAll().getFirst();
        var product = products.findByAvailableTrueOrderBySortOrderAsc().stream()
                .filter(candidate -> candidate.getLeadTimeDays() == 0)
                .findFirst()
                .orElseThrow();
        var date = slots.load(location, LocalDate.now(clock).plusDays(10),
                        LocalDate.now(clock).plusDays(20), 0).stream()
                .filter(DaySlotLoad::isSelectable)
                .findFirst()
                .orElseThrow()
                .date();
        var time = slots.options(location, date).stream()
                .filter(SlotOption::isAvailable)
                .findFirst()
                .orElseThrow()
                .time();

        // Fill it to the brim, the way customers do.
        for (int taken = 0; taken < location.getSlotCapacity(); taken++) {
            var customer = customers.findOrCreate("Full", "Slot" + taken,
                    "full.slot." + taken + "@example.test", "+34 600 000 002");
            orderService.place(List.of(new CartLine(product.getId(), 1, null)), customer, location, date, time,
                    Channel.ONLINE, null, null);
        }

        var afterwards = slots.options(location, date).stream()
                .filter(option -> option.time().equals(time))
                .findFirst()
                .orElseThrow();

        assertEquals(0, afterwards.remaining(), "nothing left in that slot");
        assertFalse(afterwards.isAvailable(), "so it is not available");
        assertFalse(slots.hasCapacity(location, date, time), "and the authoritative check agrees");
        assertTrue(slots.nextFreeTime(location, date).stream().noneMatch(free -> free.equals(time)),
                "the default the picker offers steps over it");
    }
}
