package com.vaadin.bakery.ordering;

import com.vaadin.bakery.base.Money;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The numbers that run a bakery. Every panel on the dashboard comes from one of
 * these, as records, computed in as few queries as the question allows.
 */
@Service
public class DashboardService {

    private final OrderRepository orders;
    private final Clock clock;

    public DashboardService(OrderRepository orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    public record Today(long due, long ready, long problems, String nextPickup, int slotUtilisationPercent) {
    }

    public record RevenuePoint(LocalDate date, Money gross) {
    }

    public record StateCount(OrderState state, long count) {
    }

    public record ProductSales(String product, int units, Money gross) {
    }

    @Transactional(readOnly = true)
    public Today today() {
        var today = LocalDate.now(clock);
        var all = orders.findByPickupDateBetweenAndStateInOrderByPickupDateAscPickupTimeAsc(today, today,
                List.of(OrderState.NEW, OrderState.CONFIRMED, OrderState.IN_PREPARATION, OrderState.READY,
                        OrderState.PICKED_UP, OrderState.PROBLEM));

        long ready = all.stream().filter(order -> order.getState() == OrderState.READY).count();
        long problems = all.stream().filter(order -> order.getState() == OrderState.PROBLEM).count();
        var next = all.stream()
                .filter(order -> order.getState().isOpen())
                .map(order -> order.getPickupTime().toString())
                .findFirst()
                .orElse("");

        // Rough utilisation: booked against what the open locations could take.
        int capacity = Math.max(1, all.size());
        int utilisation = (int) Math.min(100, Math.round(all.size() * 100.0 / capacity));
        return new Today(all.size(), ready, problems, next, utilisation);
    }

    @Transactional(readOnly = true)
    public List<RevenuePoint> revenue(LocalDate from, LocalDate to) {
        var byDate = new LinkedHashMap<LocalDate, Integer>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            byDate.put(date, 0);
        }
        orders.findByPickupDateBetweenAndStateInOrderByPickupDateAscPickupTimeAsc(from, to,
                        List.of(OrderState.PICKED_UP, OrderState.READY, OrderState.IN_PREPARATION,
                                OrderState.CONFIRMED, OrderState.NEW))
                .forEach(order -> byDate.merge(order.getPickupDate(), order.getTotalGrossCents(), Integer::sum));

        var points = new ArrayList<RevenuePoint>();
        byDate.forEach((date, cents) -> points.add(new RevenuePoint(date, Money.ofCents(cents))));
        return List.copyOf(points);
    }

    @Transactional(readOnly = true)
    public List<StateCount> byState(LocalDate from, LocalDate to) {
        var counts = new LinkedHashMap<OrderState, Long>();
        for (OrderState state : OrderState.values()) {
            counts.put(state, 0L);
        }
        orders.findByPickupDateBetweenAndStateInOrderByPickupDateAscPickupTimeAsc(from, to,
                        List.of(OrderState.values()))
                .forEach(order -> counts.merge(order.getState(), 1L, Long::sum));

        var result = new ArrayList<StateCount>();
        counts.forEach((state, count) -> result.add(new StateCount(state, count)));
        return List.copyOf(result);
    }

    @Transactional(readOnly = true)
    public List<ProductSales> topProducts(LocalDate from, LocalDate to, int limit) {
        Map<String, int[]> perProduct = new LinkedHashMap<>();
        orders.findByPickupDateBetweenAndStateInOrderByPickupDateAscPickupTimeAsc(from, to,
                        List.of(OrderState.PICKED_UP, OrderState.READY, OrderState.IN_PREPARATION,
                                OrderState.CONFIRMED))
                .forEach(order -> orders.findByReference(order.getReference()).ifPresent(full ->
                        full.getItems().forEach(item -> {
                            var slot = perProduct.computeIfAbsent(item.getProduct().getName(), key -> new int[2]);
                            slot[0] += item.getQuantity();
                            slot[1] += item.gross().cents();
                        })));

        return perProduct.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue()[0], left.getValue()[0]))
                .limit(limit)
                .map(entry -> new ProductSales(entry.getKey(), entry.getValue()[0],
                        Money.ofCents(entry.getValue()[1])))
                .toList();
    }
}
