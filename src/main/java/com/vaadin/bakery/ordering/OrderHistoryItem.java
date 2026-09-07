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
