package com.vaadin.bakery.catalogue;

import com.vaadin.bakery.base.AbstractEntity;
import com.vaadin.bakery.base.Money;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
public class Product extends AbstractEntity {

    @NotBlank
    @Size(max = 128)
    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @NotBlank
    @Pattern(regexp = "[a-z0-9-]+")
    @Size(max = 128)
    @Column(nullable = false, unique = true, length = 128)
    private String slug;

    @NotNull
    @ManyToOne(optional = false)
    private Category category;

    @Size(max = 4000)
    @Column(length = 4000)
    private String descriptionMarkdown;

    @Min(1)
    @Max(100000)
    @Column(nullable = false)
    private int priceCents;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VatRate vatRate = VatRate.REDUCED;

    @Size(max = 255)
    private String imagePath;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "product_allergen",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "allergen_id"))
    private Set<Allergen> allergens = new LinkedHashSet<>();

    /** Empty means every day. Feeds the disabled weekdays of the date picker. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_weekday", joinColumns = @JoinColumn(name = "product_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 16)
    private Set<DayOfWeek> availableWeekdays = EnumSet.noneOf(DayOfWeek.class);

    @Min(0)
    @Max(14)
    @Column(nullable = false)
    private int leadTimeDays;

    @Min(0)
    private Integer dailyCapacity;

    @Min(0)
    @Column(nullable = false)
    private int stockToday;

    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private int sortOrder;

    public Money price() {
        return Money.ofCents(priceCents);
    }

    public boolean isAvailableOn(DayOfWeek day) {
        return availableWeekdays.isEmpty() || availableWeekdays.contains(day);
    }

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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getDescriptionMarkdown() {
        return descriptionMarkdown;
    }

    public void setDescriptionMarkdown(String descriptionMarkdown) {
        this.descriptionMarkdown = descriptionMarkdown;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(int priceCents) {
        this.priceCents = priceCents;
    }

    public VatRate getVatRate() {
        return vatRate;
    }

    public void setVatRate(VatRate vatRate) {
        this.vatRate = vatRate;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Set<Allergen> getAllergens() {
        return allergens;
    }

    public void setAllergens(Set<Allergen> allergens) {
        this.allergens = allergens;
    }

    public Set<DayOfWeek> getAvailableWeekdays() {
        return availableWeekdays;
    }

    public void setAvailableWeekdays(Set<DayOfWeek> availableWeekdays) {
        this.availableWeekdays = availableWeekdays;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(int leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public Integer getDailyCapacity() {
        return dailyCapacity;
    }

    public void setDailyCapacity(Integer dailyCapacity) {
        this.dailyCapacity = dailyCapacity;
    }

    public int getStockToday() {
        return stockToday;
    }

    public void setStockToday(int stockToday) {
        this.stockToday = stockToday;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public String toString() {
        return name;
    }
}
