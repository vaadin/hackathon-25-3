package com.vaadin.bakery.ordering;

import com.vaadin.bakery.base.AbstractEntity;
import com.vaadin.bakery.people.Address;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
public class PickupLocation extends AbstractEntity {

    @NotBlank
    @Size(max = 96)
    @Column(nullable = false, unique = true, length = 96)
    private String name;

    @Embedded
    private Address address = new Address();

    @NotNull
    @Column(nullable = false)
    private LocalTime opensAt = LocalTime.of(8, 0);

    @NotNull
    @Column(nullable = false)
    private LocalTime closesAt = LocalTime.of(20, 0);

    @Min(15)
    @Max(120)
    @Column(nullable = false)
    private int slotMinutes = 30;

    @Min(1)
    @Column(nullable = false)
    private int slotCapacity = 8;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "location_closed_weekday", joinColumns = @JoinColumn(name = "location_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 16)
    private Set<DayOfWeek> closedWeekdays = EnumSet.of(DayOfWeek.SUNDAY);

    @Column(nullable = false)
    private boolean active = true;

    public boolean isOpenOn(DayOfWeek day) {
        return active && !closedWeekdays.contains(day);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public LocalTime getOpensAt() {
        return opensAt;
    }

    public void setOpensAt(LocalTime opensAt) {
        this.opensAt = opensAt;
    }

    public LocalTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(LocalTime closesAt) {
        this.closesAt = closesAt;
    }

    public int getSlotMinutes() {
        return slotMinutes;
    }

    public void setSlotMinutes(int slotMinutes) {
        this.slotMinutes = slotMinutes;
    }

    public int getSlotCapacity() {
        return slotCapacity;
    }

    public void setSlotCapacity(int slotCapacity) {
        this.slotCapacity = slotCapacity;
    }

    public Set<DayOfWeek> getClosedWeekdays() {
        return closedWeekdays;
    }

    public void setClosedWeekdays(Set<DayOfWeek> closedWeekdays) {
        this.closedWeekdays = closedWeekdays;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return name;
    }
}
