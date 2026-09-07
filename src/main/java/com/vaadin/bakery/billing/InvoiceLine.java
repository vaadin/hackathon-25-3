package com.vaadin.bakery.billing;

import com.vaadin.bakery.base.AbstractEntity;
import com.vaadin.bakery.base.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** No foreign key to the product: a line survives a catalogue deletion. */
@Entity
public class InvoiceLine extends AbstractEntity {

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String description;

    @Min(1)
    @Column(nullable = false)
    private int quantity;

    @Min(0)
    @Column(nullable = false)
    private int unitPriceCents;

    @Min(0)
    @Column(nullable = false)
    private int vatRatePercent;

    @Min(0)
    @Column(nullable = false)
    private int netCents;

    @Min(0)
    @Column(nullable = false)
    private int vatCents;

    @Min(0)
    @Column(nullable = false)
    private int grossCents;

    public Money net() {
        return Money.ofCents(netCents);
    }

    public Money vat() {
        return Money.ofCents(vatCents);
    }

    public Money gross() {
        return Money.ofCents(grossCents);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getUnitPriceCents() {
        return unitPriceCents;
    }

    public void setUnitPriceCents(int unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public int getVatRatePercent() {
        return vatRatePercent;
    }

    public void setVatRatePercent(int vatRatePercent) {
        this.vatRatePercent = vatRatePercent;
    }

    public int getNetCents() {
        return netCents;
    }

    public void setNetCents(int netCents) {
        this.netCents = netCents;
    }

    public int getVatCents() {
        return vatCents;
    }

    public void setVatCents(int vatCents) {
        this.vatCents = vatCents;
    }

    public int getGrossCents() {
        return grossCents;
    }

    public void setGrossCents(int grossCents) {
        this.grossCents = grossCents;
    }
}
