package com.vaadin.bakery.ordering;

import com.vaadin.bakery.base.Money;
import com.vaadin.bakery.catalogue.Product;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The cart is a session scoped bean holding signals, not a table. Everything
 * that shows the cart, from the header badge to the checkout summary, reads
 * this one source, which is the whole reason signals exist.
 */
@Component
@VaadinSessionScope
public class CartSignals {

    private final ProductRepository products;
    private final ListSignal<CartLine> lines = new ListSignal<>();
    private final Signal<Integer> itemCount;
    private final Signal<Money> net;
    private final Signal<Money> vat;
    private final Signal<Money> gross;

    public CartSignals(ProductRepository products) {
        this.products = products;
        this.itemCount = Signal.computed(() -> lines.get().stream()
                .mapToInt(line -> line.get().quantity())
                .sum());
        this.net = Signal.computed(() -> Money.ofCents(lines.get().stream()
                .mapToInt(line -> priceCents(line.get()) * line.get().quantity())
                .sum()));
        this.vat = Signal.computed(() -> Money.ofCents(lines.get().stream()
                .mapToInt(line -> {
                    var product = product(line.get());
                    int lineNet = priceCents(line.get()) * line.get().quantity();
                    return product.map(p -> Money.ofCents(lineNet).percentage(p.getVatRate().percent()).cents())
                            .orElse(0);
                })
                .sum()));
        this.gross = Signal.computed(() -> net.get().plus(vat.get()));
    }

    public ListSignal<CartLine> lines() {
        return lines;
    }

    public Signal<Integer> itemCount() {
        return itemCount;
    }

    /**
     * Reading a computed signal from outside a reactive context needs an
     * explicit opt out, so these are the accessors for services, tests and any
     * plain listener that just wants the number.
     */
    public int count() {
        return Signal.untracked(() -> itemCount.get());
    }

    public Money netAmount() {
        return Signal.untracked(() -> net.get());
    }

    public Money vatAmount() {
        return Signal.untracked(() -> vat.get());
    }

    public Money grossAmount() {
        return Signal.untracked(() -> gross.get());
    }

    public Signal<Money> net() {
        return net;
    }

    public Signal<Money> vat() {
        return vat;
    }

    public Signal<Money> gross() {
        return gross;
    }

    public Optional<Product> product(CartLine line) {
        return products.findById(line.productId());
    }

    private int priceCents(CartLine line) {
        return product(line).map(Product::getPriceCents).orElse(0);
    }

    /** Adding the same product twice raises the quantity instead of duplicating a row. */
    public void add(Long productId, int quantity, String comment) {
        // peek, not get: this runs from a click listener, which is not a
        // reactive context, and reading with get() there is an error in 25.3.
        var existing = lines.peek().stream()
                .filter(line -> line.peek().productId().equals(productId))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().update(line -> line.withQuantity(line.quantity() + quantity));
            return;
        }
        lines.insertLast(new CartLine(productId, quantity, comment));
    }

    public void setQuantity(Long productId, int quantity) {
        var existing = lines.peek().stream()
                .filter(line -> line.peek().productId().equals(productId))
                .findFirst();
        if (existing.isEmpty()) {
            return;
        }
        if (quantity <= 0) {
            lines.remove(existing.get());
            return;
        }
        existing.get().update(line -> line.withQuantity(quantity));
    }

    public void remove(Long productId) {
        lines.peek().stream()
                .filter(line -> line.peek().productId().equals(productId))
                .findFirst()
                .ifPresent(lines::remove);
    }

    public void clear() {
        new ArrayList<>(lines.peek()).forEach(lines::remove);
    }

    public List<CartLine> snapshot() {
        return lines.peek().stream().map(Signal::peek).toList();
    }

    /** The slowest product in the cart decides the earliest possible pickup day. */
    public int maxLeadTimeDays() {
        return snapshot().stream()
                .map(this::product)
                .filter(Optional::isPresent)
                .mapToInt(product -> product.get().getLeadTimeDays())
                .max()
                .orElse(0);
    }
}
