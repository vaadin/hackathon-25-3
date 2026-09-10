package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.SafeHtml;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * The quick answer to "what is in this one", asked without losing the queue.
 *
 * The quantities are what somebody is actually reading, so they are the largest
 * thing here and everything else arranges itself around them.
 */
public class OrderDetailsBand extends Div {

    public OrderDetailsBand(OrderService orders, String reference) {
        addClassName("order-board__band");

        // Resolved once: this is the same full-graph fetch the board's
        // expensive summary column is counted for, and the tiles and the
        // allergen chips both read from it.
        var lines = orders.detailLines(reference);

        var tiles = new Div();
        tiles.addClassName("order-board__tiles");
        lines.forEach(line -> {
            var tile = new Div();
            tile.addClassName("order-board__tile");

            var quantity = new Span(String.valueOf(line.quantity()));
            quantity.addClassName("order-board__tile-quantity");
            var name = new Span(line.productName());
            name.addClassName("order-board__tile-name");
            tile.add(quantity, name);

            if (line.comment() != null && !line.comment().isBlank()) {
                var comment = new Span(SafeHtml.text(line.comment()));
                comment.addClassName("order-board__tile-comment");
                tile.add(comment);
            }
            tiles.add(tile);
        });

        var allergens = new Div();
        allergens.addClassName("order-board__band-allergens");
        lines.stream()
                .flatMap(line -> line.allergenKeys().stream())
                .distinct()
                .forEach(key -> {
                    // The real component, not a Span wearing theme="badge".
                    // That attribute is a Lumo convention and Aura knows
                    // nothing about it, so the same chips came out coloured
                    // under one theme and as bare words under the other.
                    var chip = Translations.bindText(new Badge(), key);
                    chip.addThemeVariants(BadgeVariant.SMALL);
                    allergens.add(chip);
                });

        var history = new Div();
        history.addClassName("order-board__band-history");
        orders.recentHistoryKeys(reference, 2)
                .forEach(key -> history.add(new Div(Translations.bindText(new Span(), key))));

        add(tiles, allergens, history);
    }
}
