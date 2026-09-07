package com.vaadin.bakery.catalogue;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    List<Product> findByAvailableTrueOrderBySortOrderAsc();

    List<Product> findByFeaturedTrueAndAvailableTrueOrderBySortOrderAsc();

    Page<Product> findByCategory(Category category, Pageable pageable);

    long countByCategory(Category category);
}
