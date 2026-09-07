package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.billing.Invoice;
import com.vaadin.bakery.billing.InvoiceRepository;
import com.vaadin.bakery.catalogue.Product;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.ordering.PickupClosureRepository;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * DOM-01. A dataset that lies is worse than no dataset: every one of these
 * checks caught something real while the generator was being written.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Transactional
class DatasetIntegrityTest {

    @Autowired
    private OrderRepository orders;

    @Autowired
    private ProductRepository products;

    @Autowired
    private InvoiceRepository invoices;

    @Autowired
    private PickupLocationRepository locations;

    @Autowired
    private PickupClosureRepository closures;

    @Test
    void volumesAreWhatTheSpecificationPromises() {
        assertEquals(1400, orders.count(), "orders");
        assertEquals(48, products.count(), "products");
        assertEquals(3, locations.count(), "pickup locations");
        assertEquals(14, closures.count(), "closures");
    }

    @Test
    void everyOrderHasItemsAndConsistentTotals() {
        List<String> problems = new ArrayList<>();
        for (Order order : orders.findAll()) {
            var items = order.getItems();
            if (items.isEmpty()) {
                problems.add(order.getReference() + " has no items");
                continue;
            }
            int net = items.stream().mapToInt(item -> item.net().cents()).sum();
            int vat = items.stream().mapToInt(item -> item.vat().cents()).sum();
            if (net != order.getTotalNetCents() || vat != order.getTotalVatCents()
                    || net + vat != order.getTotalGrossCents()) {
                problems.add(order.getReference() + " totals do not match its lines");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyInvoiceAddsUp() {
        List<String> problems = new ArrayList<>();
        for (Invoice invoice : invoices.findAll()) {
            if (invoice.getGrossCents() != invoice.getNetCents() + invoice.getVatCents()) {
                problems.add(invoice.getNumber() + " gross is not net plus vat");
            }
            int lineNet = invoice.getLines().stream().mapToInt(line -> line.getNetCents()).sum();
            if (lineNet != invoice.getNetCents()) {
                problems.add(invoice.getNumber() + " net does not match its lines");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void invoiceNumbersAreUniqueAndGapFree() {
        var numbers = invoices.findAll().stream().map(Invoice::getNumber).sorted().toList();
        assertEquals(numbers.size(), numbers.stream().distinct().count(), "numbers must be unique");
        for (int i = 0; i < numbers.size(); i++) {
            var expected = "2026-%06d".formatted(i + 1);
            assertEquals(expected, numbers.get(i), "the yearly sequence must have no gaps");
        }
    }

    @Test
    void noOrderSitsOnAClosedDayOrAClosedWeekday() {
        var closureDates = closures.findAll().stream()
                .collect(Collectors.groupingBy(closure -> closure.getDate(),
                        Collectors.mapping(closure -> closure.getLocation() == null ? null
                                : closure.getLocation().getId(), Collectors.toList())));

        List<String> problems = new ArrayList<>();
        for (Order order : orders.findAll()) {
            var location = order.getPickupLocation();
            if (!location.isOpenOn(order.getPickupDate().getDayOfWeek())) {
                problems.add(order.getReference() + " is on a weekday " + location.getName() + " is closed");
            }
            var closedFor = closureDates.get(order.getPickupDate());
            if (closedFor != null && (closedFor.contains(null) || closedFor.contains(location.getId()))) {
                problems.add(order.getReference() + " is on a closure date");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void noSlotIsOverbooked() {
        var counts = new HashMap<String, Integer>();
        var capacities = new HashMap<Long, Integer>();
        locations.findAll().forEach(location -> capacities.put(location.getId(), location.getSlotCapacity()));

        List<String> problems = new ArrayList<>();
        for (Order order : orders.findAll()) {
            if (order.getState() == OrderState.CANCELLED) {
                continue;
            }
            var key = order.getPickupLocation().getId() + "|" + order.getPickupDate() + "|" + order.getPickupTime();
            int booked = counts.merge(key, 1, Integer::sum);
            if (booked > capacities.get(order.getPickupLocation().getId())) {
                problems.add("Slot " + key + " is overbooked");
            }
        }
        assertTrue(problems.isEmpty(), problems.stream().distinct().limit(5).collect(Collectors.joining("\n")));
    }

    @Test
    void everyProductPointsAtAnImageThatCouldExist() throws Exception {
        var imageDir = Path.of("src/main/resources/META-INF/resources/images/products");
        List<String> missing = new ArrayList<>();
        for (Product product : products.findAll()) {
            assertFalse(product.getImagePath() == null || product.getImagePath().isBlank(),
                    product.getName() + " has no image path");
            if (Files.isDirectory(imageDir) && !Files.exists(imageDir.resolve(product.getImagePath()))) {
                missing.add(product.getImagePath());
            }
        }
        // The files land with the storefront epic. Until then the placeholder
        // path is enough, and this check turns into a real one as soon as the
        // directory exists.
        if (Files.isDirectory(imageDir)) {
            assertTrue(missing.isEmpty(), "Missing image files: " + missing);
        }
    }
}
