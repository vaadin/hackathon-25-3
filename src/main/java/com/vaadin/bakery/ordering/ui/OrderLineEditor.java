package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.Money;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.catalogue.Product;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.local.ValueSignal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The lines of an order, wherever the order came from.
 *
 * There is always an empty row at the end, so adding a product is typing rather
 * than first asking for somewhere to type. A quantity of zero removes the line,
 * because that is what everybody tries first.
 *
 * HasEnabled so a host can put the whole thing beyond reach in one call: a role
 * that may read an order but not reprice it still needs to see its lines.
 */
public class OrderLineEditor extends Composite<Div> implements HasEnabled {

    /** Fired whenever the lines or the total change, so a host can follow them. */
    public static class LinesChangeEvent extends ComponentEvent<OrderLineEditor> {
        public LinesChangeEvent(OrderLineEditor source) {
            super(source, false);
        }
    }

    private final CatalogueService catalogue;
    private final Div rows = new Div();
    private final List<Row> live = new ArrayList<>();
    private final Span total = new Span();

    // Bumped on every change to a row, so the computed text bindings that read
    // it re-run. A locale-only computed never re-evaluates on its own: it has
    // to genuinely depend on a signal, and this is that signal.
    private final ValueSignal<Integer> revision = new ValueSignal<>(0);

    // Guards the side effect that appends a fresh empty row when the user
    // fills in what was the last one. While bulk loading lines from setLines,
    // that appending happens once, explicitly, after every line is in place.
    private boolean populating;

    // What a row's ComboBox can offer: every product on sale, plus whatever is
    // already on the order being edited, even if it has since gone off sale.
    // Fetched once per setLines call rather than once per row or per keystroke.
    private List<Product> products = List.of();
    private Map<Long, Product> productsById = Map.of();

    public OrderLineEditor(CatalogueService catalogue) {
        this.catalogue = catalogue;
        getContent().addClassName("order-editor");
        rows.addClassName("order-editor__rows");
        refreshProducts(List.of());

        total.addClassName("order-editor__total");
        Translations.bindText(total, locale -> {
            revision.get();
            return getTranslation(locale, "board.editor.total", total().format(locale));
        });

        getContent().add(rows, total);
        appendEmptyRow();
    }

    public void setLines(List<CartLine> lines) {
        refreshProducts(lines);
        populating = true;
        try {
            live.clear();
            rows.removeAll();
            lines.forEach(this::appendRow);
            appendEmptyRow();
        } finally {
            populating = false;
        }
        changed();
    }

    /**
     * Every field of every row that holds a line, handed to whoever wants to
     * say something about them.
     *
     * The assistant writes lines through a tool of its own rather than through
     * the form controller, so the controller never sees these fields and never
     * marks them. This is how the tool tells it afterwards.
     */
    public void forEachLineField(java.util.function.Consumer<com.vaadin.flow.component.HasValue<?, ?>> visitor) {
        live.forEach(row -> {
            visitor.accept(row.product);
            visitor.accept(row.quantity);
            visitor.accept(row.comment);
        });
    }

    /** Only rows somebody actually filled in. */
    public List<CartLine> getLines() {
        return live.stream()
                .filter(row -> row.product.getValue() != null)
                .filter(row -> row.quantity.getValue() != null && row.quantity.getValue() > 0)
                .map(row -> new CartLine(row.product.getValue().getId(), row.quantity.getValue(),
                        row.comment.getValue() == null || row.comment.getValue().isBlank()
                                ? null : row.comment.getValue()))
                .toList();
    }

    /**
     * VAT-inclusive, like the total {@code Order.recalculateTotals()} stores and
     * like what the customer and the invoice both see. A staff member editing an
     * order must read the same figure everywhere, not a bare net price.
     */
    public Money total() {
        return live.stream()
                .filter(row -> row.product.getValue() != null)
                .filter(row -> row.quantity.getValue() != null && row.quantity.getValue() > 0)
                .map(row -> gross(row.product.getValue(), row.quantity.getValue()))
                .reduce(Money.ZERO, Money::plus);
    }

    /** Test seam: how many rows are on screen, empty one included. */
    public int rowCount() {
        return live.size();
    }

    /** Test seam: set a row's quantity the way a user would. */
    public void setQuantity(int index, int quantity) {
        live.get(index).quantity.setValue(quantity);
    }

    /** Test seam: choose a row's product the way a user would. */
    public void setProduct(int index, Long productId) {
        var product = productsById.get(productId);
        if (product != null) {
            live.get(index).product.setValue(product);
        }
    }

    /** Test seam: whether a row's quantity, comment and remove button are enabled. */
    public boolean isRowEnabled(int index) {
        return live.get(index).quantity.isEnabled();
    }

    /**
     * Test seam: whether this row shows its comment whether or not it has
     * focus. Focus itself is the stylesheet's half of the rule and cannot be
     * asserted without a browser; this is the half that is decided here.
     */
    public boolean alwaysShowsComment(int index) {
        return live.get(index).layout.hasClassName("order-editor__row--commented");
    }

    public Registration addLinesChangeListener(ComponentEventListener<LinesChangeEvent> listener) {
        return addListener(LinesChangeEvent.class, listener);
    }

    /**
     * A snapshot of what a row's ComboBox may offer: the catalogue's available
     * products, unioned with whatever these specific lines already refer to. A
     * product a line points at is never deleted while referenced (see
     * {@code CatalogueService.delete}), only ever marked unavailable, so this
     * union always resolves; without it, a line whose product went off sale
     * would silently vanish from {@link #getLines()} the moment it is loaded.
     */
    private void refreshProducts(List<CartLine> lines) {
        var byId = new LinkedHashMap<Long, Product>();
        catalogue.availableProducts().forEach(product -> byId.put(product.getId(), product));
        for (CartLine line : lines) {
            byId.computeIfAbsent(line.productId(), catalogue::require);
        }
        products = List.copyOf(byId.values());
        productsById = Map.copyOf(byId);
    }

    private static Money gross(Product product, int quantity) {
        var net = product.price().times(quantity);
        return net.plus(net.percentage(product.getVatRate().percent()));
    }

    private void appendRow(CartLine line) {
        var row = appendEmptyRow();
        var product = productsById.get(line.productId());
        if (product != null) {
            row.product.setValue(product);
        }
        row.quantity.setValue(line.quantity());
        row.comment.setValue(line.comment() == null ? "" : line.comment());
    }

    private Row appendEmptyRow() {
        var row = new Row();
        live.add(row);
        rows.add(row.layout);
        return row;
    }

    private void changed() {
        revision.update(value -> value + 1);
        fireEvent(new LinesChangeEvent(this));
    }

    /** One line: what, how many, what it costs, why, and a way out. */
    private final class Row {
        private final ComboBox<Product> product = new ComboBox<>();
        private final IntegerField quantity = new IntegerField();
        private final TextField comment = new TextField();
        private final Span price = new Span();
        private final Button remove = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
        private final Div layout;

        private Row() {
            // Placeholders rather than labels. A label is a 25 pixel band above
            // every field, and six fields down a phone-width panel spend 150
            // pixels saying "Product", "Quantity" and "Comment" three times
            // each. The words stay as the accessible names.
            Translations.bind(product, product::setPlaceholder, "board.editor.product");
            Translations.bind(product, product::setAriaLabel, "board.editor.product");
            product.setItems(products);
            // A product name is a product name in every language, so this one
            // is set once rather than dressed up as a locale binding.
            product.setItemLabelGenerator(Product::getName);

            Translations.bind(quantity, quantity::setAriaLabel, "board.editor.quantity");
            quantity.setMin(0);
            quantity.setMax(99);
            quantity.setStepButtonsVisible(true);
            quantity.setValue(1);

            Translations.bind(comment, comment::setPlaceholder, "board.editor.comment");
            Translations.bind(comment, comment::setAriaLabel, "board.editor.comment");
            Translations.bind(remove, remove::setAriaLabel, "board.editor.remove");
            remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

            product.addClassName("order-editor__product");
            quantity.addClassName("order-editor__quantity");
            comment.addClassName("order-editor__comment");
            remove.addClassName("order-editor__remove");
            price.addClassName("order-editor__price");
            Translations.bindText(price, locale -> {
                revision.get();
                var chosen = product.getValue();
                var count = quantity.getValue() == null ? 0 : quantity.getValue();
                // Zero rather than nothing on the empty trailing row: an
                // absent price collapses its column and the row above it stops
                // lining up with the rest.
                return chosen == null ? Money.ZERO.format(locale) : gross(chosen, count).format(locale);
            });

            setEnabled(false);
            price.setClassName("order-editor__price--pending", true);
            product.addValueChangeListener(event -> {
                setEnabled(event.getValue() != null);
                // The trailing row's zero is a placeholder holding its column
                // open, not a price, so it reads as one.
                price.setClassName("order-editor__price--pending", event.getValue() == null);
                // Filling what is currently the last row is what earns a new
                // empty one. Bulk loading appends its own trailing row once,
                // explicitly, so this side effect stays out of its way.
                if (!populating && event.getValue() != null && live.getLast() == this) {
                    appendEmptyRow();
                }
                changed();
            });
            quantity.addValueChangeListener(event -> {
                if (event.getValue() != null && event.getValue() == 0) {
                    removeRow();
                    return;
                }
                changed();
            });
            comment.addValueChangeListener(event -> {
                showComment();
                changed();
            });
            remove.addClickListener(event -> removeRow());

            layout = new Div(product, quantity, comment, price, remove);
            layout.addClassName("order-editor__row");
        }

        /**
         * A row that already carries a comment shows it whether or not anybody
         * is in the row. The rest reveal theirs on focus, which the stylesheet
         * does on its own: a comment nobody has written is worth a line of the
         * panel only while somebody is looking at that line.
         */
        private void showComment() {
            var written = comment.getValue() != null && !comment.getValue().isBlank();
            layout.setClassName("order-editor__row--commented", written);
        }

        private void setEnabled(boolean enabled) {
            quantity.setEnabled(enabled);
            comment.setEnabled(enabled);
            remove.setEnabled(enabled);
        }

        private void removeRow() {
            live.remove(this);
            rows.remove(layout);
            if (live.isEmpty()) {
                appendEmptyRow();
            }
            changed();
        }
    }
}
