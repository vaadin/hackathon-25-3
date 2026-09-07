package com.vaadin.bakery.catalogue;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllergenRepository extends JpaRepository<Allergen, Long> {

    Optional<Allergen> findByCode(String code);

    List<Allergen> findAllByOrderByNameAsc();
}
