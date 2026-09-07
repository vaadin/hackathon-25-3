package com.vaadin.bakery.billing;

import com.vaadin.bakery.ordering.Order;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByNumber(String number);

    Optional<Invoice> findByOrder(Order order);

    @Query("select max(i.number) from Invoice i where i.number like concat(:year, '-%')")
    Optional<String> findHighestNumberForYear(String year);
}
