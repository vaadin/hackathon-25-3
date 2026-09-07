package com.vaadin.bakery.catalogue;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;

/**
 * Uploaded photos are served by the application, never from the upload path, so
 * nothing a user sends becomes a file the web server will execute.
 */
@RestController
public class ProductImageController {

    private final ProductImageRepository images;
    private final ProductRepository products;

    public ProductImageController(ProductImageRepository images, ProductRepository products) {
        this.images = images;
        this.products = products;
    }

    @GetMapping("/images/product/{id}")
    public ResponseEntity<byte[]> image(@PathVariable Long id) {
        return products.findById(id)
                .flatMap(images::findByProduct)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.getContentType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                        .body(image.getData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
