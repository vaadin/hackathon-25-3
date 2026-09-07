package com.vaadin.bakery.catalogue;

import com.vaadin.bakery.base.Money;
import java.util.List;

/**
 * What the storefront grid needs, as a record. The old Bakery pushed display
 * DTOs full of preformatted strings into Lit templates; here the view formats
 * at the edge, with the user's locale.
 */
public record ProductCard(Long id, String name, String slug, String categoryName, Money price,
        List<String> allergenKeys, int leadTimeDays, String imageUrl, String placeholderUrl, boolean featured) {

    public static ProductCard of(Product product, boolean hasUploadedImage) {
        return new ProductCard(product.getId(), product.getName(), product.getSlug(),
                product.getCategory().getName(), product.price(),
                product.getAllergens().stream().map(Allergen::translationKey).sorted().toList(),
                product.getLeadTimeDays(), ProductImages.url(product, hasUploadedImage),
                ProductImages.placeholderUrl(product), product.isFeatured());
    }

    public boolean needsLeadTime() {
        return leadTimeDays > 0;
    }
}
