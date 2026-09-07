package com.vaadin.bakery.catalogue.ui;

import com.vaadin.bakery.catalogue.ProductCard;
import com.vaadin.flow.signals.local.ValueSignal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * The state of the catalogue, as signals. Everything the storefront shows is
 * computed from these four, and they are mirrored into the URL so a filtered
 * catalogue can be shared.
 */
public class CatalogueFilters {

    public enum Sort {
        RELEVANCE, PRICE_ASC, PRICE_DESC, NAME;

        public String translationKey() {
            return "catalogue.sort." + name();
        }
    }

    private final ValueSignal<String> search = new ValueSignal<>("");
    private final ValueSignal<String> category = new ValueSignal<>("");
    private final ValueSignal<Set<String>> excludedAllergens = new ValueSignal<>(Set.of());
    private final ValueSignal<Sort> sort = new ValueSignal<>(Sort.RELEVANCE);

    public ValueSignal<String> search() {
        return search;
    }

    public ValueSignal<String> category() {
        return category;
    }

    public ValueSignal<Set<String>> excludedAllergens() {
        return excludedAllergens;
    }

    public ValueSignal<Sort> sort() {
        return sort;
    }

    public boolean isEmpty() {
        return search.peek().isBlank() && category.peek().isBlank() && excludedAllergens.peek().isEmpty();
    }

    public void clear() {
        search.set("");
        category.set("");
        excludedAllergens.set(Set.of());
    }

    /** Pure function of the four signals, which is what makes it a computed value. */
    public List<ProductCard> apply(List<ProductCard> all) {
        String term = search.get().trim().toLowerCase();
        String categoryName = category.get();
        Set<String> excluded = excludedAllergens.get();

        var filtered = all.stream()
                .filter(card -> term.isEmpty() || card.name().toLowerCase().contains(term))
                .filter(card -> categoryName.isBlank() || card.categoryName().equals(categoryName))
                .filter(card -> excluded.stream().noneMatch(card.allergenKeys()::contains))
                .toList();

        Comparator<ProductCard> comparator = switch (sort.get()) {
            case PRICE_ASC -> Comparator.comparing(card -> card.price().cents());
            case PRICE_DESC -> Comparator.<ProductCard, Integer>comparing(card -> card.price().cents()).reversed();
            case NAME -> Comparator.comparing(ProductCard::name);
            case RELEVANCE -> Comparator.comparing(ProductCard::featured).reversed()
                    .thenComparing(ProductCard::name);
        };
        return filtered.stream().sorted(comparator).toList();
    }
}
