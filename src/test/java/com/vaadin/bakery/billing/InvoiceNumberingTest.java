package com.vaadin.bakery.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** DOM-06 and DOM-07. A gap in an invoice sequence is a finance problem. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Transactional
class InvoiceNumberingTest {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoices;

    @Autowired
    private OrderRepository orders;

    @Test
    void numbersContinueTheYearlySequence() {
        String highest = invoices.findAll().stream()
                .map(Invoice::getNumber)
                .sorted()
                .reduce((first, second) -> second)
                .orElseThrow();
        int expected = Integer.parseInt(highest.substring(highest.indexOf('-') + 1)) + 1;

        assertEquals("2026-%06d".formatted(expected), invoiceService.nextNumber());
    }

    @Test
    void issuingSnapshotsTheOrderAndAddsUp() {
        var order = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.READY)
                .findFirst()
                .orElseThrow();

        var invoice = invoiceService.issue(order);

        assertEquals(order.getItems().size(), invoice.getLines().size());
        assertEquals(order.getTotalNetCents(), invoice.getNetCents());
        assertEquals(invoice.getNetCents() + invoice.getVatCents(), invoice.getGrossCents());
        assertEquals(invoice.getIssuedAt().plusDays(14), invoice.getDueAt());
        assertEquals(order.getCustomer().getEmail(), invoice.getBillingEmail());
    }

    @Test
    void aVoidedNumberIsNeverReused() {
        var order = orders.findAll().stream()
                .filter(candidate -> candidate.getState() == OrderState.READY)
                .findFirst()
                .orElseThrow();
        var first = invoiceService.issue(order);
        var firstNumber = first.getNumber();

        invoiceService.voidInvoice(first, "Wrong billing address");
        var replacement = invoiceService.issue(order);

        assertNotEquals(firstNumber, replacement.getNumber());
        assertTrue(replacement.getNumber().compareTo(firstNumber) > 0, "the sequence only moves forward");
    }

    @Test
    void voidingWithoutAReasonIsRefused() {
        var invoice = invoices.findAll().getFirst();
        var failure = assertThrows(DomainException.RuleViolation.class,
                () -> invoiceService.voidInvoice(invoice, "  "));
        assertEquals("billing.void.reasonRequired", failure.translationKey());
    }
}
