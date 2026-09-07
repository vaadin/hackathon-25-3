package com.vaadin.bakery.catalogue;

import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.ordering.OrderRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogueService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final AllergenRepository allergens;
    private final ProductImageRepository images;
    private final OrderRepository orders;

    public CatalogueService(ProductRepository products, CategoryRepository categories, AllergenRepository allergens,
            ProductImageRepository images, OrderRepository orders) {
        this.products = products;
        this.categories = categories;
        this.allergens = allergens;
        this.images = images;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<Category> categories() {
        return categories.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<Allergen> allergens() {
        return allergens.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Product> availableProducts() {
        return products.findByAvailableTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<Product> featured() {
        return products.findByFeaturedTrueAndAvailableTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Product> bySlug(String slug) {
        return products.findBySlug(slug);
    }

    @Transactional(readOnly = true)
    public Product require(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new DomainException.NotFound("catalogue.product.notFound", id));
    }

    @Transactional(readOnly = true)
    public Page<Product> page(Specification<Product> specification, Pageable pageable) {
        return products.findAll(specification, pageable);
    }

    @Transactional
    public Product save(Product product) {
        return products.save(product);
    }

    /**
     * A product that any order refers to is never deleted: the order lines keep
     * a price snapshot but still point at the row, and history that disappears
     * is worse than a catalogue with an unavailable item in it.
     */
    @Transactional
    public void delete(Product product) {
        boolean referenced = orders.count((root, query, builder) -> builder.equal(
                root.join("items").get("product"), product)) > 0;
        if (referenced) {
            throw new DomainException.Conflict("catalogue.product.inUse", product.getName());
        }
        images.deleteByProduct(product);
        products.delete(product);
    }

    @Transactional
    public Category save(Category category) {
        return categories.save(category);
    }

    @Transactional
    public void delete(Category category) {
        long count = products.countByCategory(category);
        if (count > 0) {
            throw new DomainException.Conflict("catalogue.category.inUse", category.getName(), count);
        }
        categories.delete(category);
    }

    @Transactional(readOnly = true)
    public Optional<ProductImage> image(Product product) {
        return images.findByProduct(product);
    }

    @Transactional
    public ProductImage storeImage(Product product, String filename, String contentType, byte[] data) {
        if (data.length > 2 * 1024 * 1024) {
            throw new DomainException.RuleViolation("catalogue.image.tooLarge", data.length);
        }
        if (!isSupportedImage(contentType, data)) {
            throw new DomainException.RuleViolation("catalogue.image.unsupported", contentType);
        }
        var image = images.findByProduct(product).orElseGet(ProductImage::new);
        image.setProduct(product);
        image.setFilename(filename);
        image.setContentType(contentType);
        image.setSizeBytes(data.length);
        image.setData(data);
        return images.save(image);
    }

    /** Content type is what the browser claims. The magic bytes are the truth. */
    static boolean isSupportedImage(String contentType, byte[] data) {
        if (contentType == null || data.length < 12) {
            return false;
        }
        boolean png = (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G';
        boolean jpeg = (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8;
        boolean webp = data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
        return switch (contentType) {
            case "image/png" -> png;
            case "image/jpeg" -> jpeg;
            case "image/webp" -> webp;
            default -> false;
        };
    }
}
