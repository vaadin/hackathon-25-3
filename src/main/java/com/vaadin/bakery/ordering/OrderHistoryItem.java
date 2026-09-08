package com.vaadin.bakery.ordering;

import com.vaadin.bakery.base.AbstractEntity;
import com.vaadin.bakery.people.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Entity
public class OrderHistoryItem extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private OrderState newState;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String message;

    /**
     * What changed, as data rather than as a sentence: "NEW>CANCELLED" for a
     * transition, "2900>3400" in cents for a total. The view translates and
     * formats it, so a row written in one language reads correctly in the
     * other, which is the same rule the message key follows.
     */
    @Size(max = 120)
    @Column(length = 120)
    private String detail;

    @NotNull
    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    @ManyToOne
    private User createdBy;

    public OrderHistoryItem() {
    }

    public OrderHistoryItem(OrderState newState, String message, User createdBy) {
        this.newState = newState;
        this.message = message;
        this.createdBy = createdBy;
    }

    public OrderHistoryItem(OrderState newState, String message, String detail, User createdBy) {
        this(newState, message, createdBy);
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public OrderState getNewState() {
        return newState;
    }

    public void setNewState(OrderState newState) {
        this.newState = newState;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
}
