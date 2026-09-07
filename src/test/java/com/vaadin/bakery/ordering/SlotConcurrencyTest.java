package com.vaadin.bakery.ordering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.bakery.people.CustomerService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * DOM-03. Capacity is advisory in the calendar and authoritative here. The
 * interesting case is the last free place, so this test fills a slot to its
 * limit and then asks for one more.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Transactional
class SlotConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private SlotService slots;

    @Autowired
    private ProductRepository products;

    @Autowired
    private PickupLocationRepository locations;

    @Autowired
    private CustomerService customers;

    @Autowired
    private Clock clock;

    @Test
    void aFullSlotIsRefusedWithTheSlotNamed() {
        var location = locations.findAll().stream()
                .filter(candidate -> candidate.getSlotCapacity() <= 8)
                .findFirst()
                .orElseThrow();
        var product = products.findByAvailableTrueOrderBySortOrderAsc().stream()
                .filter(candidate -> candidate.getLeadTimeDays() == 0)
                .findFirst()
                .orElseThrow();

        var date = slots.load(location, LocalDate.now(clock).plusDays(10), LocalDate.now(clock).plusDays(20), 0)
                .stream()
                .filter(DaySlotLoad::isSelectable)
                .findFirst()
                .orElseThrow()
                .date();
        LocalTime time = slots.options(location, date).stream()
                .filter(SlotOption::isAvailable)
                .findFirst()
                .orElseThrow()
                .time();

        var counter = new AtomicInteger();
        int capacity = location.getSlotCapacity();
        int placed = 0;
        for (int i = 0; i < capacity + 3; i++) {
            var customer = customers.findOrCreate("Test", "Customer" + i,
                    "slot.test" + counter.incrementAndGet() + "@example.com", "+34 600 000 001");
            try {
                orderService.place(List.of(new CartLine(product.getId(), 1, null)), customer, location, date, time,
                        Channel.ONLINE, null, null);
                placed++;
            } catch (DomainException.Conflict conflict) {
                assertEquals("ordering.slot.full", conflict.translationKey());
                assertTrue(conflict.arguments().length > 0, "the refusal names the slot");
                break;
            }
        }

        assertTrue(placed <= capacity, "never more orders in a slot than its capacity, placed " + placed);
        assertTrue(slots.options(location, date).stream()
                .filter(option -> option.time().equals(time))
                .allMatch(option -> option.remaining() == 0), "the slot is now full");
    }
}
