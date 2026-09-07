package com.vaadin.bakery.billing;

import com.vaadin.bakery.base.Money;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything the printable document needs, read inside a transaction. The view
 * never touches the entity, because an invoice is exactly the kind of thing
 * somebody opens from a link a week later, with no session anywhere.
 */
public record InvoiceDocument(String number, InvoiceStatus status, boolean paid, LocalDate issuedAt,
        LocalDate dueAt, String billingName, String billingEmail, String street, String postalCode, String city,
        String vatId, List<Line> lines, Money net, Money vat, Money gross) {

    public record Line(String description, int quantity, Money unitPrice, int vatRatePercent, Money net,
            Money vat) {
    }

    public boolean hasAddress() {
        return street != null && !street.isBlank();
    }
}
