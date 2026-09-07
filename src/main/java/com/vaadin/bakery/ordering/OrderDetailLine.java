package com.vaadin.bakery.ordering;

import com.vaadin.bakery.base.Money;
import java.util.List;

/**
 * What a row detail needs, read inside a transaction and handed to the view as
 * data. Views do not touch entities with lazy collections.
 */
public record OrderDetailLine(Long productId, int quantity, String productName, String comment,
        List<String> allergenKeys, Money gross) {
}
