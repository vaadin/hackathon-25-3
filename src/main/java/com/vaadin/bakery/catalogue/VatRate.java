package com.vaadin.bakery.catalogue;

/** Spanish rates. The percentage is stored on the order line as a snapshot. */
public enum VatRate {
    ZERO(0), REDUCED(10), STANDARD(21);

    private final int percent;

    VatRate(int percent) {
        this.percent = percent;
    }

    public int percent() {
        return percent;
    }

    public String translationKey() {
        return "catalogue.vat." + name();
    }
}
