package com.vaadin.bakery.billing;

public enum InvoiceStatus {
    ISSUED, PAID, VOID;

    public String translationKey() {
        return "billing.status." + name();
    }
}
