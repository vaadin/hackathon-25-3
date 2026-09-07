package com.vaadin.bakery.ordering;

import com.vaadin.bakery.base.AbstractEntity;
import com.vaadin.bakery.base.Money;
import com.vaadin.bakery.catalogue.Product;
import com.vaadin.bakery.catalogue.VatRate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class OrderItem extends AbstractEntity {

    @NotNull
    @ManyToOne(optional = false)
    private Product product;

    @Min(1)
    @Max(99)
    @Column(nullable = false)
    private int quantity = 1;

    /** Snapshot. A later price change never rewrites history. */
    @Min(0)
    @Column(nullable = false)
    private int unitPriceCents;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VatRate vatRate = VatRate.REDUCED;

    @Size(max = 255)
    @Column(length = 255)
    private String comment;

    public Money net() {
        return Money.ofCents(unitPriceCents).times(quantity);
    }

    public Money vat() {
        return net().percentage(vatRate.percent());
    }

    public Money gross() {
        return net().plus(vat());
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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

    public VatRate getVatRate() {
        return vatRate;
    }

    public void setVatRate(VatRate vatRate) {
        this.vatRate = vatRate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
