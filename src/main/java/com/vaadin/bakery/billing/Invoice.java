package com.vaadin.bakery.billing;

import com.vaadin.bakery.base.AbstractEntity;
import com.vaadin.bakery.base.Money;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.people.Address;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * An immutable snapshot of a picked up order. Nothing here points at a live
 * product or a live customer: when a customer edits their profile, an invoice
 * issued last month must not change.
 */
@Entity
public class Invoice extends AbstractEntity {

    @NotBlank
    @Size(max = 16)
    @Column(nullable = false, unique = true, length = 16)
    private String number;

    @NotNull
    @OneToOne(optional = false)
    private Order order;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceStatus status = InvoiceStatus.ISSUED;

    @NotNull
    @Column(nullable = false)
    private LocalDate issuedAt;

    @NotNull
    @Column(nullable = false)
    private LocalDate dueAt;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String billingName;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String billingEmail;

    @Embedded
    private Address billingAddress = new Address();

    @Size(max = 20)
    @Column(length = 20)
    private String vatId;

    @NotEmpty
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "invoice_id")
    @OrderColumn(name = "position")
    private List<InvoiceLine> lines = new ArrayList<>();

    @Min(0)
    @Column(nullable = false)
    private int netCents;

    @Min(0)
    @Column(nullable = false)
    private int vatCents;

    @Min(0)
    @Column(nullable = false)
    private int grossCents;

    @Column(nullable = false)
    private boolean paid;

    private Instant paidAt;

    @Size(max = 200)
    @Column(length = 200)
    private String voidReason;

    public Money net() {
        return Money.ofCents(netCents);
    }

    public Money vat() {
        return Money.ofCents(vatCents);
    }

    public Money gross() {
        return Money.ofCents(grossCents);
    }

    public boolean isImmutable() {
        return status != InvoiceStatus.ISSUED || paid;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public LocalDate getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDate issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDate getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDate dueAt) {
        this.dueAt = dueAt;
    }

    public String getBillingName() {
        return billingName;
    }

    public void setBillingName(String billingName) {
        this.billingName = billingName;
    }

    public String getBillingEmail() {
        return billingEmail;
    }

    public void setBillingEmail(String billingEmail) {
        this.billingEmail = billingEmail;
    }

    public Address getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(Address billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getVatId() {
        return vatId;
    }

    public void setVatId(String vatId) {
        this.vatId = vatId;
    }

    public List<InvoiceLine> getLines() {
        return lines;
    }

    public void setLines(List<InvoiceLine> lines) {
        this.lines = lines;
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

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }

    @Override
    public String toString() {
        return number;
    }
}
