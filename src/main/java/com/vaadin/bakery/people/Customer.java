package com.vaadin.bakery.people;

import com.vaadin.bakery.base.AbstractEntity;
import com.vaadin.bakery.base.validation.OnDraft;
import com.vaadin.bakery.base.validation.OnSubmit;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import java.time.Instant;

/**
 * Shared and deduplicated by email. In the old Bakery every order owned a
 * private copy of its customer, so the same person was a different row on every
 * visit and nothing could be said about them.
 */
@Entity
public class Customer extends AbstractEntity {

    @NotBlank
    @Size(max = 80, groups = { Default.class, OnDraft.class })
    @Column(nullable = false, length = 80)
    private String firstName;

    @NotBlank
    @Size(max = 80, groups = { Default.class, OnDraft.class })
    @Column(nullable = false, length = 80)
    private String lastName;

    @NotBlank(groups = OnSubmit.class)
    @Email(groups = { Default.class, OnDraft.class })
    @Size(max = 160, groups = { Default.class, OnDraft.class })
    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @NotBlank(groups = OnSubmit.class)
    @Pattern(regexp = "^$|^(\\+\\d{1,3})?[ -]?(\\d[ -]?){6,14}$",
            groups = { Default.class, OnDraft.class })
    @Size(max = 32, groups = { Default.class, OnDraft.class })
    @Column(length = 32)
    private String phone;

    @Embedded
    @Valid
    private Address address = new Address();

    @Size(max = 20)
    @Column(length = 20)
    private String vatId;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean marketingOptIn;

    @NotNull
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    @PreUpdate
    void normalise() {
        if (email != null) {
            email = email.toLowerCase();
        }
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isBusiness() {
        return vatId != null && !vatId.isBlank();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getVatId() {
        return vatId;
    }

    public void setVatId(String vatId) {
        this.vatId = vatId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isMarketingOptIn() {
        return marketingOptIn;
    }

    public void setMarketingOptIn(boolean marketingOptIn) {
        this.marketingOptIn = marketingOptIn;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
