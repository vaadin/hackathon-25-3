package com.example;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * The named parameter is the point: resolving `:reference` needs the parameter
 * name, which needs the `-parameters` compiler flag.
 */
public interface Tickets extends JpaRepository<Ticket, Long> {

    @Query("select t from Ticket t where t.reference = :reference")
    Optional<Ticket> byReference(String reference);
}
