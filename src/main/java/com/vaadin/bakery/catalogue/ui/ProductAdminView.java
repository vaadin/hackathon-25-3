package com.vaadin.bakery.catalogue.ui;

import com.vaadin.bakery.base.Money;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.catalogue.Product;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.bakery.base.ui.RowActions;
import com.vaadin.bakery.catalogue.Category;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;

/**
 * The catalogue, as an administrator sees it.
 *
 * Price and today's stock are edited in the cell, because those are the two
 * things that change every morning and nobody wants a dialog for. Everything
 * else opens the editor.
 */
@Route("admin/products")
@PageTitle("Products")
@Menu(order = 20, title = "Products", icon = "vaadin:package")
@RolesAllowed(Role.ADMIN_NAME)
public class ProductAdminView extends VerticalLayout {

    private final CatalogueService catalogue;
    private final ProductRepository products;
    private final GridPro<Product> grid = new GridPro<>();
    private final ProductEditor editor;
    private TextField nameFilter;
    private ComboBox<Category> categoryFilter;

    public ProductAdminView(CatalogueService catalogue, ProductRepository products) {
        this.catalogue = catalogue;
        this.products = products;
        addClassName("product-admin");
        setSizeFull();

        this.editor = new ProductEditor(catalogue, catalogue.categories(), catalogue.allergens(),
                saved -> refresh());
        add(editor);

        grid.setSizeFull();
        grid.setSelectionMode(GridPro.SelectionMode.NONE);

        // What you can do to a row, first and as narrow as two icons, the way
        // the order board opens an order. Words in that column cost the product
        // name the room it needs and repeat themselves on every line.
        grid.addComponentColumn(this::rowActions)
                .setKey("actions").setHeader("").setWidth("5.5rem").setFlexGrow(0).setFrozen(true)
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);

        // The grid owns the header bindings: a column is not in the component
        // tree, so it cannot decide when a binding starts or stops.
        var name = grid.addColumn(Product::getName).setSortable(true);
        // A floor and a share of the slack. This is the column that gave way
        // first: at 900 pixels the names were three letters and an ellipsis and
        // the search field under the header had nowhere left to be. There is no
        // setMinWidth on a column in 25.3, and there does not need to be: a
        // grid column is a flex item that does not shrink, so its width is its
        // minimum and flex grow is what it does with anything spare.
        name.setWidth("14rem").setFlexGrow(2);
        Translations.bind(grid, name::setHeader, "admin.product.name");

        // Two editable cells, validated before they are written. Money on the
        // screen and cents in the row: the column reads as an amount and the
        // column beside it as a count, which is what they are.
        var price = grid.addEditColumn(product -> Money.ofCents(product.getPriceCents()).format(getLocale()))
                .text((product, value) -> updatePrice(product, value));
        price.setSortable(true);
        price.setComparator(java.util.Comparator.comparingInt(Product::getPriceCents));
        Translations.bind(grid, price::setHeader, "admin.product.price");
        var stock = grid.addEditColumn(Product::getStockToday)
                .text((product, value) -> updateStock(product, value));
        stock.setSortable(true);
        Translations.bind(grid, stock::setHeader, "admin.product.stock");

        // The category sits after the two daily numbers so that the search can
        // have the width of every column that does not filter itself. A header
        // row joins adjacent cells only, and with the category between them the
        // search was stuck over one column.
        var category = grid.addColumn(product -> product.getCategory().getName()).setSortable(true);
        Translations.bind(grid, category::setHeader, "admin.product.category");

        var available = grid.addComponentColumn(product -> toggle(product.isAvailable(), value -> {
            product.setAvailable(value);
            catalogue.save(product);
        }));
        Translations.bind(grid, available::setHeader, "admin.product.available");

        var featured = grid.addComponentColumn(product -> toggle(product.isFeatured(), value -> {
            product.setFeatured(value);
            catalogue.save(product);
        }));
        Translations.bind(grid, featured::setHeader, "admin.product.featured");

        // The filters belong to the columns they filter, so they live in a
        // header row inside the grid rather than in a bar above it. Scrolling
        // the columns scrolls the filters with them, which is the whole point.
        //
        // Prepended rather than appended, and not for looks: a grid refuses to
        // join cells anywhere but the top-most header row, so a filter row
        // under the titles is a filter row where every filter is exactly one
        // column wide. The row in specs/FEEDBACK-25.3.md says so.
        var filters = grid.prependHeaderRow();

        var search = new TextField();
        Translations.bind(search, search::setPlaceholder, "admin.product.search");
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setClearButtonVisible(true);
        search.setWidthFull();
        search.addValueChangeListener(event -> refresh());
        this.nameFilter = search;
        // Over the name, and over the two number columns beside it: neither has
        // a filter of its own and a search field the width of one column is a
        // search field nobody can read what they typed into.
        filters.join(filters.getCell(name), filters.getCell(price), filters.getCell(stock))
                .setComponent(search);

        var byCategory = new ComboBox<Category>();
        Translations.bind(byCategory, byCategory::setPlaceholder, "catalogue.category.all");
        byCategory.setItems(catalogue.categories());
        byCategory.setItemLabelGenerator(Category::getName);
        byCategory.setClearButtonVisible(true);
        byCategory.setWidthFull();
        byCategory.addValueChangeListener(event -> refresh());
        this.categoryFilter = byCategory;
        filters.getCell(category).setComponent(byCategory);

        var create = Translations.bindText(new Button("", event -> editor.newProduct()),
                "admin.product.new");
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.setIcon(new com.vaadin.flow.component.icon.Icon(
                com.vaadin.flow.component.icon.VaadinIcon.PLUS));

        // Under the table and at the end of the row, where People and Closures
        // put theirs. A button above the grid is the first thing read on a
        // screen whose subject is the list.
        var toolbar = new Div(create);
        toolbar.addClassName("product-admin__toolbar");

        add(grid, toolbar);
        // A cell rendered as money has to be redrawn when the language changes:
        // the value provider formats it, and only a reload asks again.
        Translations.onLocale(this, locale -> refresh());
        refresh();
    }

    /**
     * The two controls of one row.
     *
     * A method rather than a lambda inside the column, because a component
     * rendered into a grid cell is invisible to a browserless test's component
     * tree: this is the only way to assert on what the column actually builds.
     */
    Div rowActions(Product product) {
        var edit = iconAction(com.vaadin.flow.component.icon.VaadinIcon.EDIT, "admin.edit",
                () -> editor.editProduct(products.findById(product.getId()).orElseThrow()));
        var delete = iconAction(com.vaadin.flow.component.icon.VaadinIcon.TRASH, "admin.delete",
                () -> delete(product));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        return RowActions.of(edit, delete);
    }

    /**
     * A row action with no word on it. The name lives in the accessible name
     * and the tooltip, which is where a one column icon has to keep it.
     */
    private Button iconAction(com.vaadin.flow.component.icon.VaadinIcon icon, String labelKey,
            Runnable action) {
        var button = new Button(new com.vaadin.flow.component.icon.Icon(icon), event -> action.run());
        button.addThemeVariants(ButtonVariant.TERTIARY);
        Translations.bind(grid, text -> {
            button.setAriaLabel(text);
            button.setTooltipText(text);
        }, labelKey);
        return button;
    }

    private Switch toggle(boolean value, java.util.function.Consumer<Boolean> onChange) {
        var toggle = new Switch(value);
        toggle.addValueChangeListener(event -> {
            onChange.accept(event.getValue());
            Notification.show(getTranslation("admin.saved"));
        });
        return toggle;
    }

    private void updatePrice(Product product, String value) {
        var cents = parseCents(value);
        if (cents == null || cents < 1) {
            error(getTranslation("admin.product.price.invalid"));
            refresh();
            return;
        }
        product.setPriceCents(cents);
        catalogue.save(product);
    }

    /**
     * An amount of money as somebody types it, in cents.
     *
     * The cell now shows "4,55 €" rather than "455", so the cell is edited in
     * the same words: the currency symbol and the spaces around it are thrown
     * away, and what is left is read without caring which language decided
     * where the comma goes. The rule is the one a reader uses: the last
     * separator with one or two digits behind it is the decimal point, and
     * every other separator groups thousands. "1.234,56", "1,234.56" and
     * "1234.56" are the same amount, which they are.
     *
     * The storage is unchanged. This is a way of writing a number, not a new
     * kind of price.
     */
    static Integer parseCents(String typed) {
        if (typed == null) {
            return null;
        }
        var cleaned = typed.replaceAll("[^0-9.,]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        var lastComma = cleaned.lastIndexOf(',');
        var lastDot = cleaned.lastIndexOf('.');
        var decimal = Math.max(lastComma, lastDot);
        var fraction = decimal < 0 ? 0 : cleaned.length() - decimal - 1;
        var whole = decimal < 0 || fraction > 2
                ? cleaned.replaceAll("[.,]", "")
                : cleaned.substring(0, decimal).replaceAll("[.,]", "")
                        + "." + cleaned.substring(decimal + 1);
        try {
            return new java.math.BigDecimal(whole)
                    .movePointRight(2)
                    .setScale(0, java.math.RoundingMode.HALF_UP)
                    .intValueExact();
        } catch (ArithmeticException | NumberFormatException invalid) {
            return null;
        }
    }

    private void updateStock(Product product, String value) {
        try {
            int stock = Integer.parseInt(value.trim());
            if (stock < 0) {
                throw new NumberFormatException();
            }
            product.setStockToday(stock);
            catalogue.save(product);
        } catch (NumberFormatException invalid) {
            error(getTranslation("admin.product.stock.invalid"));
            refresh();
        }
    }

    private void delete(Product product) {
        try {
            catalogue.delete(product);
            refresh();
        } catch (DomainException failure) {
            error(getTranslation(failure.translationKey(), failure.arguments()));
        }
    }

    private void error(String message) {
        var notification = Notification.show(message);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /** Whatever the two filters in the header row currently say. */
    private void refresh() {
        var term = nameFilter == null ? "" : nameFilter.getValue();
        var category = categoryFilter == null ? null : categoryFilter.getValue();

        List<Product> items = products.findAll().stream()
                .filter(product -> term == null || term.isBlank()
                        || product.getName().toLowerCase().contains(term.toLowerCase()))
                .filter(product -> category == null || category.equals(product.getCategory()))
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .toList();
        grid.setItems(items);
    }

    TextField nameFilter() {
        return nameFilter;
    }

    ComboBox<Category> categoryFilter() {
        return categoryFilter;
    }

    GridPro<Product> grid() {
        return grid;
    }

    /** Test seam: the editor is a dialog, so it only exists once it is open. */
    ProductEditor editor() {
        return editor;
    }
}
