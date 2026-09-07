package com.vaadin.bakery.ordering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.bakery.people.CustomerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** DOM-02, DOM-05 and the rules that guard placing an order. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orders;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ProductRepository products;

    @Autowired
    private PickupLocationRepository locations;

    @Autowired
    private CustomerService customers;

    @Autowired
    private SlotService slots;

    @Autowired
    private com.vaadin.bakery.people.UserRepository users;

    @Autowired
    private Clock clock;

    private LocalDate freeDay() {
        var location = locations.findAll().getFirst();
        return slots.load(location, LocalDate.now(clock).plusDays(4), LocalDate.now(clock).plusDays(20), 0).stream()
                .filter(DaySlotLoad::isSelectable)
                .findFirst()
                .orElseThrow()
                .date();
    }

    @Test
    void placingAnOrderSnapshotsPricesAndTotals() {
        var location = locations.findAll().getFirst();
        var product = products.findByAvailableTrueOrderBySortOrderAsc().stream()
                .filter(candidate -> candidate.getLeadTimeDays() == 0)
                .findFirst()
                .orElseThrow();
        var customer = customers.findOrCreate("Ana", "Ruiz", "ana.ruiz@example.com", "+34 600 111 222");
        var date = freeDay();
        var time = slots.nextFreeTime(location, date).orElseThrow();

        var order = orderService.place(List.of(new CartLine(product.getId(), 3, "Sliced")), customer, location,
                date, time, Channel.ONLINE, null, null);

        assertNotNull(order.getReference());
        assertTrue(order.getReference().startsWith("ORD-"));
        assertNotNull(order.getTrackingToken());
        assertEquals(1, order.getItems().size());
        assertEquals(product.getPriceCents(), order.getItems().getFirst().getUnitPriceCents(),
                "the line keeps the price it was created with");
        assertEquals(product.getPriceCents() * 3, order.getTotalNetCents());
        assertEquals(order.getTotalNetCents() + order.getTotalVatCents(), order.getTotalGrossCents());
        assertEquals(OrderState.NEW, order.getState());
        assertEquals(1, order.getHistory().size(), "placing writes the first history entry");
    }

    @Test
    void anEmptyCartIsRefused() {
        var location = locations.findAll().getFirst();
        var customer = customers.findOrCreate("Ana", "Ruiz", "ana.empty@example.com", "+34 600 111 222");
        assertThrows(DomainException.RuleViolation.class, () -> orderService.place(List.of(), customer, location,
                freeDay(), slots.slotTimes(location).getFirst(), Channel.ONLINE, null, null));
    }

    @Test
    void aLeadTimeProductCannotBePickedUpTomorrow() {
        var location = locations.findAll().getFirst();
        var cake = products.findAll().stream()
                .filter(product -> product.getLeadTimeDays() >= 2)
                .findFirst()
                .orElseThrow();
        var customer = customers.findOrCreate("Ana", "Ruiz", "ana.lead@example.com", "+34 600 111 222");
        var tomorrow = LocalDate.now(clock).plusDays(1);

        var failure = assertThrows(DomainException.RuleViolation.class,
                () -> orderService.place(List.of(new CartLine(cake.getId(), 1, null)), customer, location,
                        tomorrow, slots.slotTimes(location).getFirst(), Channel.ONLINE, null, null));
        assertEquals("ordering.slot.leadTimeViolated", failure.translationKey());
    }

    @Test
    void updatingLinesReplacesTheItemsAndReprices() {
        var location = locations.findAll().getFirst();
        var product = products.findByAvailableTrueOrderBySortOrderAsc().stream()
                .filter(candidate -> candidate.getLeadTimeDays() == 0)
                .findFirst()
                .orElseThrow();
        var other = products.findByAvailableTrueOrderBySortOrderAsc().stream()
                .filter(candidate -> !candidate.getId().equals(product.getId()))
                .findFirst()
                .orElseThrow();
        var otherId = other.getId();
        var otherPriceCents = other.getPriceCents();
        var customer = customers.findOrCreate("Ana", "Ruiz", "ana.lines@example.com", "+34 600 111 222");
        var date = freeDay();
        var time = slots.nextFreeTime(location, date).orElseThrow();
        var order = orderService.place(List.of(new CartLine(product.getId(), 1, null)), customer, location, date,
                time, Channel.ONLINE, null, null);

        // Flushing and clearing forces the next findById to be a genuine load
        // from the database, the shape updateLines sees in the running
        // application. Without this, "order" stays the very instance place()
        // built in memory: Hibernate never actually loaded its items
        // collection from a row, so that collection carries no loaded
        // persister, and the orphan-removal check that setItems(...) used to
        // trip is guarded on exactly that (loadedPersister != null &&
        // loadedPersister.hasOrphanDelete()) and never fires. It is not about
        // sharing one session versus two: sharing a session is what makes the
        // check MORE likely to fire, once the collection genuinely was loaded.
        em.flush();
        em.clear();
        var reloaded = orders.findById(order.getId()).orElseThrow();

        var updated = orderService.updateLines(reloaded, List.of(new CartLine(otherId, 2, null)), null);
        // This is where a collection replaced instead of mutated in place
        // throws, now that it carries a real loaded persister.
        em.flush();

        assertEquals(1, updated.getItems().size());
        assertEquals(otherId, updated.getItems().getFirst().getProduct().getId());
        assertEquals(otherPriceCents * 2, updated.getTotalNetCents());
    }

    @Test
    void anUnavailableProductIsRefused() {
        var location = locations.findAll().getFirst();
        var product = products.findByAvailableTrueOrderBySortOrderAsc().getFirst();
        product.setAvailable(false);
        products.save(product);
        var customer = customers.findOrCreate("Ana", "Ruiz", "ana.gone@example.com", "+34 600 111 222");

        var failure = assertThrows(DomainException.RuleViolation.class,
                () -> orderService.place(List.of(new CartLine(product.getId(), 1, null)), customer, location,
                        freeDay(), slots.slotTimes(location).getFirst(), Channel.ONLINE, null, null));
        assertEquals("ordering.product.unavailable", failure.translationKey());
    }

    private com.vaadin.bakery.people.User staff(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow();
    }

    /** 04-security: a baker sets the states an order passes through while it is baked. */
    @Test
    void aBakerMovesAnOrderThroughTheKitchen() {
        var order = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.CONFIRMED)
                .findFirst()
                .orElseThrow();

        var moved = orderService.changeState(order, OrderState.IN_PREPARATION,
                "ordering.history.in_preparation", staff("baker@bakery.test"));

        assertEquals(OrderState.IN_PREPARATION, moved.getState());
    }

    /** 04-security: handing the order over is not baking, it is where the invoice is issued. */
    @Test
    void aBakerCannotHandTheOrderOver() {
        var order = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.READY)
                .findFirst()
                .orElseThrow();

        var refusal = assertThrows(DomainException.class, () -> orderService.changeState(order,
                OrderState.PICKED_UP, "ordering.history.picked_up", staff("baker@bakery.test")));

        assertEquals(OrderState.READY, orders.findById(order.getId()).orElseThrow().getState(),
                "and the order did not move");
        assertTrue(refusal.translationKey().contains("Role"), "it says why, got " + refusal.translationKey());
    }

    /** 04-security: cancelling is a commercial decision about a customer, not a judgement about an oven. */
    @Test
    void aBakerCannotCancel() {
        var order = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.CONFIRMED)
                .findFirst()
                .orElseThrow();

        assertThrows(DomainException.class, () -> orderService.changeState(order, OrderState.CANCELLED,
                "ordering.history.cancelled", staff("baker@bakery.test")));

        assertEquals(OrderState.CONFIRMED, orders.findById(order.getId()).orElseThrow().getState());
    }

    /** The counter keeps every state, including the ones the kitchen normally drives. */
    @Test
    void aBaristaCanStillDoEverythingIncludingUnsticking() {
        var confirmed = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.CONFIRMED)
                .findFirst()
                .orElseThrow();
        var ready = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.READY)
                .findFirst()
                .orElseThrow();
        var barista = staff("barista@bakery.test");

        assertEquals(OrderState.IN_PREPARATION, orderService.changeState(confirmed,
                OrderState.IN_PREPARATION, "ordering.history.in_preparation", barista).getState(),
                "the counter can unstick a kitchen state");
        assertEquals(OrderState.PICKED_UP, orderService.changeState(ready,
                OrderState.PICKED_UP, "ordering.history.picked_up", barista).getState());
    }
}
