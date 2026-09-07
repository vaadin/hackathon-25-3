package com.vaadin.bakery.ordering;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dynamic filtering for the staff board. Specifications replace the add on
 * based filterable data provider the old Bakery depended on.
 */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> matching(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        var like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> {
            var customer = root.join("customer");
            return builder.or(
                    builder.like(builder.lower(root.get("reference")), like),
                    builder.like(builder.lower(customer.get("firstName")), like),
                    builder.like(builder.lower(customer.get("lastName")), like),
                    builder.like(builder.lower(customer.get("email")), like),
                    builder.like(builder.lower(customer.get("phone")), like));
        };
    }

    public static Specification<Order> fromDate(LocalDate from) {
        return from == null ? null
                : (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("pickupDate"), from);
    }

    public static Specification<Order> inStates(List<OrderState> states) {
        return states == null || states.isEmpty() ? null
                : (root, query, builder) -> root.get("state").in(states);
    }

    public static Specification<Order> all(List<Specification<Order>> parts) {
        Specification<Order> result = null;
        for (Specification<Order> part : parts) {
            if (part == null) {
                continue;
            }
            result = result == null ? part : result.and(part);
        }
        return result;
    }
}
