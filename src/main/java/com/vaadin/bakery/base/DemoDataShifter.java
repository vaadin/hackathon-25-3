package com.vaadin.bakery.base;

import com.vaadin.bakery.billing.Invoice;
import com.vaadin.bakery.billing.InvoiceRepository;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.ordering.PickupClosureRepository;
import com.vaadin.bakery.people.CustomerRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The dataset is static SQL anchored on a Monday. At boot every date moves by a
 * whole number of weeks so the demo always looks like this week.
 *
 * The whole week part is the point. Shifting by a raw day count would turn a
 * seeded Tuesday into a Friday, which silently breaks "closed on Sundays",
 * misaligns the closures and destroys the slot load pattern. Whole weeks keep
 * every weekday exactly where the generator put it, at the cost of the data
 * being at most six days stale.
 */
@Component
public class DemoDataShifter implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataShifter.class);

    private final OrderRepository orders;
    private final PickupClosureRepository closures;
    private final CustomerRepository customers;
    private final InvoiceRepository invoices;
    private final Clock clock;
    private final boolean enabled;
    private final LocalDate anchor;

    public DemoDataShifter(OrderRepository orders, PickupClosureRepository closures, CustomerRepository customers,
            InvoiceRepository invoices, Clock clock,
            @Value("${demo.shift-dates:true}") boolean enabled,
            @Value("${demo.anchor:2026-01-05}") String anchor) {
        this.orders = orders;
        this.closures = closures;
        this.customers = customers;
        this.invoices = invoices;
        this.clock = clock;
        this.enabled = enabled;
        this.anchor = LocalDate.parse(anchor);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        long weekShift = weekShiftDays();
        if (weekShift == 0) {
            return;
        }
        var offset = Duration.ofDays(weekShift);

        List<Order> allOrders = orders.findAll();
        for (Order order : allOrders) {
            order.setPickupDate(order.getPickupDate().plusDays(weekShift));
            order.setPlacedAt(order.getPlacedAt().plus(offset));
            order.getHistory().forEach(item -> item.setTimestamp(item.getTimestamp().plus(offset)));
            order.getMessages().forEach(message -> message.setSentAt(message.getSentAt().plus(offset)));
        }
        orders.saveAll(allOrders);

        closures.findAll().forEach(closure -> closure.setDate(closure.getDate().plusDays(weekShift)));
        customers.findAll().forEach(customer -> customer.setCreatedAt(customer.getCreatedAt().plus(offset)));

        List<Invoice> allInvoices = invoices.findAll();
        for (Invoice invoice : allInvoices) {
            invoice.setIssuedAt(invoice.getIssuedAt().plusDays(weekShift));
            invoice.setDueAt(invoice.getDueAt().plusDays(weekShift));
            if (invoice.getPaidAt() != null) {
                invoice.setPaidAt(invoice.getPaidAt().plus(offset));
            }
        }

        LOG.info("Demo dataset shifted by {} days ({} weeks), anchor {} is now {}",
                weekShift, weekShift / 7, anchor, anchor.plusDays(weekShift));
        reconcileToday(allOrders);
    }

    long weekShiftDays() {
        long days = ChronoUnit.DAYS.between(anchor, LocalDate.now(clock));
        return Math.floorDiv(days, 7) * 7;
    }

    /**
     * After the shift the anchor week is at most six days behind, so the kitchen
     * board can be empty on the day somebody demos. Nudge a handful of today's
     * orders into the states a kitchen board is for.
     */
    private void reconcileToday(List<Order> allOrders) {
        LocalDate today = LocalDate.now(clock);
        var todays = allOrders.stream().filter(order -> today.equals(order.getPickupDate())).toList();
        boolean hasKitchenWork = todays.stream().anyMatch(order -> order.getState().isActiveInKitchen());
        if (hasKitchenWork || todays.isEmpty()) {
            return;
        }
        var states = List.of(OrderState.CONFIRMED, OrderState.IN_PREPARATION, OrderState.READY);
        for (int i = 0; i < Math.min(6, todays.size()); i++) {
            todays.get(i).setState(states.get(i % states.size()));
        }
        LOG.info("Reconciled {} orders so the kitchen board is not empty today", Math.min(6, todays.size()));
    }
}
