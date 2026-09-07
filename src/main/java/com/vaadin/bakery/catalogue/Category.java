package com.vaadin.bakery.catalogue;

import com.vaadin.bakery.base.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
public class Category extends AbstractEntity {

    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @NotBlank
    @Pattern(regexp = "[a-z0-9-]+")
    @Size(max = 64)
    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    @Min(0)
    @Column(nullable = false)
    private int displayOrder;

    @Size(max = 64)
    @Column(length = 64)
    private String iconName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    @Override
    public String toString() {
        return name;
    }
}
