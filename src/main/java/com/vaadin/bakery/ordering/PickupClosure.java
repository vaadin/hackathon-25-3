package com.vaadin.bakery.ordering;

import com.vaadin.bakery.base.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** A day, or part of one, on which a location does not serve. */
@Entity
public class PickupClosure extends AbstractEntity {

    /** Null means every location. */
    @ManyToOne
    private PickupLocation location;

    @NotNull
    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private boolean wholeDay = true;

    private LocalTime fromTime;

    private LocalTime toTime;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ClosureKind kind = ClosureKind.HOLIDAY;

    public enum ClosureKind {
        HOLIDAY, MAINTENANCE;

        public String translationKey() {
            return "ordering.closure." + name();
        }
    }

    public boolean covers(LocalTime time) {
        if (wholeDay) {
            return true;
        }
        return fromTime != null && toTime != null && !time.isBefore(fromTime) && time.isBefore(toTime);
    }

    public PickupLocation getLocation() {
        return location;
    }

    public void setLocation(PickupLocation location) {
        this.location = location;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isWholeDay() {
        return wholeDay;
    }

    public void setWholeDay(boolean wholeDay) {
        this.wholeDay = wholeDay;
    }

    public LocalTime getFromTime() {
        return fromTime;
    }

    public void setFromTime(LocalTime fromTime) {
        this.fromTime = fromTime;
    }

    public LocalTime getToTime() {
        return toTime;
    }

    public void setToTime(LocalTime toTime) {
        this.toTime = toTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ClosureKind getKind() {
        return kind;
    }

    public void setKind(ClosureKind kind) {
        this.kind = kind;
    }
}
