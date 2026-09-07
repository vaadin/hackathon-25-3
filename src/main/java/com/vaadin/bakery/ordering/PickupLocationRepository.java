package com.vaadin.bakery.ordering;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickupLocationRepository extends JpaRepository<PickupLocation, Long> {

    List<PickupLocation> findByActiveTrueOrderByNameAsc();
}
