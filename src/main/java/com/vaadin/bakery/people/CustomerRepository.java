package com.vaadin.bakery.people;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByEmailIgnoreCase(String email);

    /** Powers the partial match customer picker: any fragment, any field. */
    @Query("""
            select c from Customer c
            where lower(c.firstName) like lower(concat('%', :term, '%'))
               or lower(c.lastName) like lower(concat('%', :term, '%'))
               or lower(c.email) like lower(concat('%', :term, '%'))
               or replace(c.phone, ' ', '') like concat('%', :term, '%')
            order by c.lastName, c.firstName
            """)
    List<Customer> search(String term, Pageable pageable);
}
