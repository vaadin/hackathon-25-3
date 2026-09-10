package com.vaadin.bakery.billing;

import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderItem;
import com.vaadin.bakery.ordering.OrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final int PAYMENT_TERM_DAYS = 14;

    private final InvoiceRepository invoices;
    private final OrderRepository orders;
    private final BakeryDetailsRepository details;
    private final Clock clock;

    public InvoiceService(InvoiceRepository invoices, OrderRepository orders,
            BakeryDetailsRepository details, Clock clock) {
        this.invoices = invoices;
        this.orders = orders;
        this.details = details;
        this.clock = clock;
    }

    /**
     * The letterhead every invoice prints, as the markdown somebody wrote.
     *
     * Empty rather than absent when nobody has written it: the document then
     * falls back to the bakery's name, which is what it always printed.
     */
    @Transactional(readOnly = true)
    public String bakeryHeader() {
        return details.findAll().stream()
                .findFirst()
                .map(BakeryDetails::getHeaderMarkdown)
                .orElse("");
    }

    /**
     * Writes the letterhead. The row is whichever one is there, and a bakery
     * that has never had one gets it created here rather than by a migration
     * nobody would run. The id is left to the sequence: a hand-assigned one is
     * how a merge quietly turns into a second row.
     */
    @Transactional
    public void saveBakeryHeader(String markdown) {
        var row = details.findAll().stream().findFirst().orElseGet(BakeryDetails::new);
        row.setHeaderMarkdown(markdown);
        details.save(row);
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> forOrder(Order order) {
        return invoices.findByOrder(order);
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> byNumber(String number) {
        return invoices.findByNumber(number);
    }

    /** The document, as data. The token has to match the order's tracking token. */
    @Transactional(readOnly = true)
    public Optional<InvoiceDocument> document(String number, String token) {
        return invoices.findByNumber(number)
                .filter(invoice -> token != null && token.equals(invoice.getOrder().getTrackingToken()))
                .map(invoice -> new InvoiceDocument(invoice.getNumber(), invoice.getStatus(), invoice.isPaid(),
                        invoice.getIssuedAt(), invoice.getDueAt(), invoice.getBillingName(),
                        invoice.getBillingEmail(),
                        invoice.getBillingAddress() == null ? null : invoice.getBillingAddress().getStreet(),
                        invoice.getBillingAddress() == null ? null : invoice.getBillingAddress().getPostalCode(),
                        invoice.getBillingAddress() == null ? null : invoice.getBillingAddress().getCity(),
                        invoice.getVatId(),
                        invoice.getLines().stream()
                                .map(line -> new InvoiceDocument.Line(line.getDescription(), line.getQuantity(),
                                        com.vaadin.bakery.base.Money.ofCents(line.getUnitPriceCents()),
                                        line.getVatRatePercent(), line.net(), line.vat()))
                                .toList(),
                        invoice.net(), invoice.vat(), invoice.gross()));
    }

    /**
     * Issued when the order is picked up, in the same transaction. Everything is
     * snapshotted: an invoice must not change when the customer later edits
     * their profile or the product is renamed.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Invoice issue(Order detached) {
        // Reload with the full graph: the caller usually holds a detached order
        // whose items cannot be walked outside a session.
        var order = orders.findByReference(detached.getReference()).orElse(detached);
        var existing = invoices.findByOrder(order);
        if (existing.isPresent() && existing.get().getStatus() != InvoiceStatus.VOID) {
            return existing.get();
        }
        var customer = order.getCustomer();
        if (customer.isBusiness() && !customer.getAddress().isComplete()) {
            throw new DomainException.RuleViolation("billing.address.incomplete", customer.getFullName());
        }

        var invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setNumber(nextNumber());
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(LocalDate.now(clock));
        invoice.setDueAt(LocalDate.now(clock).plusDays(PAYMENT_TERM_DAYS));
        invoice.setBillingName(customer.getFullName());
        invoice.setBillingEmail(customer.getEmail());
        invoice.setBillingAddress(customer.getAddress());
        invoice.setVatId(customer.getVatId());

        int net = 0;
        int vat = 0;
        for (OrderItem item : order.getItems()) {
            var line = new InvoiceLine();
            line.setDescription(item.getProduct().getName());
            line.setQuantity(item.getQuantity());
            line.setUnitPriceCents(item.getUnitPriceCents());
            line.setVatRatePercent(item.getVatRate().percent());
            line.setNetCents(item.net().cents());
            line.setVatCents(item.vat().cents());
            line.setGrossCents(item.gross().cents());
            invoice.getLines().add(line);
            net += line.getNetCents();
            vat += line.getVatCents();
        }
        invoice.setNetCents(net);
        invoice.setVatCents(vat);
        invoice.setGrossCents(net + vat);
        return invoices.save(invoice);
    }

    @Transactional
    public Invoice markPaid(Invoice invoice) {
        var current = invoices.findById(invoice.getId())
                .orElseThrow(() -> new DomainException.NotFound("billing.invoice.notFound"));
        if (current.getStatus() == InvoiceStatus.VOID) {
            throw new DomainException.RuleViolation("billing.invoice.void");
        }
        current.setPaid(true);
        current.setPaidAt(Instant.now(clock));
        current.setStatus(InvoiceStatus.PAID);
        return invoices.save(current);
    }

    /** Corrections void and reissue. A number is never reused. */
    @Transactional
    public Invoice voidInvoice(Invoice invoice, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new DomainException.RuleViolation("billing.void.reasonRequired");
        }
        var current = invoices.findById(invoice.getId())
                .orElseThrow(() -> new DomainException.NotFound("billing.invoice.notFound"));
        current.setStatus(InvoiceStatus.VOID);
        current.setVoidReason(reason);
        return invoices.save(current);
    }

    String nextNumber() {
        String year = String.valueOf(LocalDate.now(clock).getYear());
        String highest = invoices.findHighestNumberForYear(year).orElse(null);
        int next = 1;
        if (highest != null) {
            next = Integer.parseInt(highest.substring(highest.indexOf('-') + 1)) + 1;
        }
        return "%s-%06d".formatted(year, next);
    }
}
