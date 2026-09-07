package com.vaadin.bakery.people;

import com.vaadin.bakery.base.validation.Billing;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

@Embeddable
public class Address implements Serializable {

    @NotBlank(groups = Billing.class)
    @Size(max = 160)
    @Column(length = 160)
    private String street;

    @NotBlank(groups = Billing.class)
    @Size(max = 16)
    @Column(length = 16)
    private String postalCode;

    @NotBlank(groups = Billing.class)
    @Size(max = 96)
    @Column(length = 96)
    private String city;

    @Size(min = 2, max = 2)
    @Column(length = 2)
    private String country = "ES";

    public boolean isComplete() {
        return street != null && !street.isBlank()
                && postalCode != null && !postalCode.isBlank()
                && city != null && !city.isBlank();
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
