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

    /**
     * The people the bakery has actually served, the most recent order first.
     *
     * This is what the counter's customer picker offers before anybody types.
     * A plain read of the customer table answers with names in whatever order
     * the rows happen to be in, and the person about to be named at the counter
     * is far more likely to be one who was here this week. The orders are where
     * that is recorded, so the orders are what the list is built from.
     *
     * Ordered by a correlated subquery rather than by a group by: grouping on
     * an entity and then selecting all of its columns is a different amount of
     * legal on H2 and on PostgreSQL, and this application runs on both.
     */
    @Query("""
            select c from Customer c
            where exists (select 1 from Order o where o.customer = c)
            order by (select max(recent.placedAt) from Order recent where recent.customer = c) desc
            """)
    List<Customer> servedRecently(Pageable pageable);
}
