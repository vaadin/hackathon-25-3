package com.vaadin.bakery.ordering;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @EntityGraph(value = Order.GRAPH_FULL)
    Optional<Order> findByReference(String reference);

    @EntityGraph(value = Order.GRAPH_FULL)
    Optional<Order> findWithItemsById(Long id);

    @EntityGraph(value = Order.GRAPH_BRIEF)
    List<Order> findByPickupDateAndStateInOrderByPickupTimeAsc(LocalDate date, List<OrderState> states);

    @EntityGraph(value = Order.GRAPH_BRIEF)
    List<Order> findByPickupDateBetweenAndStateInOrderByPickupDateAscPickupTimeAsc(
            LocalDate from, LocalDate to, List<OrderState> states);

    @EntityGraph(value = Order.GRAPH_BRIEF)
    Page<Order> findAll(org.springframework.data.jpa.domain.Specification<Order> specification, Pageable pageable);

    long countByPickupLocationAndPickupDateAndPickupTimeAndStateIn(
            PickupLocation location, LocalDate date, LocalTime time, List<OrderState> states);

    /** Slot load for a whole range in one query, for the date metadata provider. */
    @Query("""
            select o.pickupDate as date, o.pickupTime as time, count(o) as booked
            from Order o
            where o.pickupLocation = :location
              and o.pickupDate between :from and :to
              and o.state <> com.vaadin.bakery.ordering.OrderState.CANCELLED
            group by o.pickupDate, o.pickupTime
            """)
    List<SlotLoadRow> loadPerSlot(PickupLocation location, LocalDate from, LocalDate to);

    interface SlotLoadRow {
        LocalDate getDate();

        LocalTime getTime();

        long getBooked();
    }

    long countByStateAndPickupDate(OrderState state, LocalDate date);

    boolean existsByReference(String reference);
}
