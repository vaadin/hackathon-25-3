package com.vaadin.bakery.catalogue;

import com.vaadin.bakery.base.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * An entity rather than an enum: the list is regulatory, and an administrator
 * has to be able to add one without a redeploy.
 */
@Entity
public class Allergen extends AbstractEntity {

    @NotBlank
    @Size(max = 32)
    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String translationKey() {
        return "catalogue.allergen." + code;
    }

    @Override
    public String toString() {
        return name;
    }
}
