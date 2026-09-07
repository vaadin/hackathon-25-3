package com.vaadin.bakery.catalogue;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    Optional<ProductImage> findByProduct(Product product);

    void deleteByProduct(Product product);
}
