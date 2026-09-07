package com.vaadin.bakery.catalogue;

/**
 * Where a product photo comes from, in order: an uploaded image in the
 * database, then the seeded file on disk, then the category placeholder.
 */
public final class ProductImages {

    public static final String STATIC_PATH = "/images/products/";
    public static final String UPLOADED_PATH = "/images/product/";
    public static final String PLACEHOLDER = "/images/products/placeholder.webp";

    private ProductImages() {
    }

    public static String url(Product product, boolean hasUploadedImage) {
        if (hasUploadedImage) {
            return UPLOADED_PATH + product.getId();
        }
        if (product.getImagePath() != null && !product.getImagePath().isBlank()) {
            return STATIC_PATH + product.getImagePath();
        }
        return PLACEHOLDER;
    }

    public static String placeholderUrl(Product product) {
        if (product.getImagePath() == null || product.getImagePath().isBlank()) {
            return PLACEHOLDER;
        }
        return STATIC_PATH + product.getImagePath().replace(".webp", "-lqip.webp");
    }
}
