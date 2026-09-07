package com.vaadin.bakery.ordering;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PickupClosureRepository extends JpaRepository<PickupClosure, Long> {

    /** Closures for one location, plus the ones that apply to every location. */
    @Query("""
            select c from PickupClosure c
            where c.date between :from and :to
              and (c.location is null or c.location = :location)
            order by c.date
            """)
    List<PickupClosure> findForLocation(PickupLocation location, LocalDate from, LocalDate to);

    List<PickupClosure> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);
}
