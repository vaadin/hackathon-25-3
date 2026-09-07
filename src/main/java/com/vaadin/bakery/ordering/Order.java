package com.vaadin.bakery.ordering;

import com.vaadin.bakery.base.AbstractEntity;
import com.vaadin.bakery.base.Money;
import com.vaadin.bakery.base.validation.OnSubmit;
import com.vaadin.bakery.people.Customer;
import com.vaadin.bakery.people.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_pickup_date", columnList = "pickupDate"),
        @Index(name = "idx_order_state", columnList = "state") })
@NamedEntityGraphs({
        @NamedEntityGraph(name = Order.GRAPH_BRIEF,
                attributeNodes = { @NamedAttributeNode("customer"), @NamedAttributeNode("pickupLocation") }),
        @NamedEntityGraph(name = Order.GRAPH_FULL,
                attributeNodes = { @NamedAttributeNode("customer"), @NamedAttributeNode("pickupLocation"),
                        @NamedAttributeNode(value = "items", subgraph = "items"),
                        @NamedAttributeNode("history") },
                subgraphs = @NamedSubgraph(name = "items", attributeNodes = @NamedAttributeNode("product"))) })
public class Order extends AbstractEntity {

    public static final String GRAPH_BRIEF = "Order.brief";
    public static final String GRAPH_FULL = "Order.full";

    @NotNull
    @Size(max = 20)
    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    /** A real foreign key with no cascade: deleting an order keeps the customer. */
    @NotNull
    @ManyToOne(optional = false)
    private Customer customer;

    @NotNull
    @ManyToOne(optional = false)
    private PickupLocation pickupLocation;

    @NotNull(groups = OnSubmit.class)
    @Column(nullable = false)
    private LocalDate pickupDate;

    @NotNull(groups = OnSubmit.class)
    @Column(nullable = false)
    private LocalTime pickupTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderState state = OrderState.NEW;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Channel channel = Channel.ONLINE;

    @ManyToOne
    private User assignedBaker;

    @NotEmpty(groups = OnSubmit.class)
    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @OrderColumn(name = "position")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @OrderColumn(name = "position")
    private List<OrderHistoryItem> history = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @OrderColumn(name = "position")
    private List<OrderMessage> messages = new ArrayList<>();

    /** Written by the customer. Sanitized before it is ever rendered. */
    @Size(max = 500)
    @Column(length = 500)
    private String customerNote;

    @Size(max = 500)
    @Column(length = 500)
    private String internalNote;

    @Column(nullable = false)
    private int totalNetCents;

    @Column(nullable = false)
    private int totalVatCents;

    @Column(nullable = false)
    private int totalGrossCents;

    @NotNull
    @Column(nullable = false)
    private Instant placedAt = Instant.now();

    @ManyToOne
    private User createdBy;

    /** Opaque, so a tracking link cannot be guessed from the reference alone. */
    @NotNull
    @Size(max = 32)
    @Column(nullable = false, length = 32)
    private String trackingToken;

    public LocalDateTime pickupAt() {
        return LocalDateTime.of(pickupDate, pickupTime);
    }

    public Money net() {
        return Money.ofCents(totalNetCents);
    }

    public Money vat() {
        return Money.ofCents(totalVatCents);
    }

    public Money gross() {
        return Money.ofCents(totalGrossCents);
    }

    /** Recomputed by the service on every mutation, so a grid never sums a lazy list. */
    public void recalculateTotals() {
        int net = 0;
        int vat = 0;
        for (OrderItem item : items) {
            net += item.net().cents();
            vat += item.vat().cents();
        }
        totalNetCents = net;
        totalVatCents = vat;
        totalGrossCents = net + vat;
    }

    public void addHistory(OrderHistoryItem item) {
        history.add(item);
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public PickupLocation getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(PickupLocation pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public LocalDate getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDate pickupDate) {
        this.pickupDate = pickupDate;
    }

    public LocalTime getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(LocalTime pickupTime) {
        this.pickupTime = pickupTime;
    }

    public OrderState getState() {
        return state;
    }

    public void setState(OrderState state) {
        this.state = state;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public User getAssignedBaker() {
        return assignedBaker;
    }

    public void setAssignedBaker(User assignedBaker) {
        this.assignedBaker = assignedBaker;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public List<OrderHistoryItem> getHistory() {
        return history;
    }

    public void setHistory(List<OrderHistoryItem> history) {
        this.history = history;
    }

    public List<OrderMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<OrderMessage> messages) {
        this.messages = messages;
    }

    public String getCustomerNote() {
        return customerNote;
    }

    public void setCustomerNote(String customerNote) {
        this.customerNote = customerNote;
    }

    public String getInternalNote() {
        return internalNote;
    }

    public void setInternalNote(String internalNote) {
        this.internalNote = internalNote;
    }

    public int getTotalNetCents() {
        return totalNetCents;
    }

    public void setTotalNetCents(int totalNetCents) {
        this.totalNetCents = totalNetCents;
    }

    public int getTotalVatCents() {
        return totalVatCents;
    }

    public void setTotalVatCents(int totalVatCents) {
        this.totalVatCents = totalVatCents;
    }

    public int getTotalGrossCents() {
        return totalGrossCents;
    }

    public void setTotalGrossCents(int totalGrossCents) {
        this.totalGrossCents = totalGrossCents;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(Instant placedAt) {
        this.placedAt = placedAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getTrackingToken() {
        return trackingToken;
    }

    public void setTrackingToken(String trackingToken) {
        this.trackingToken = trackingToken;
    }

    @Override
    public String toString() {
        return reference;
    }
}
