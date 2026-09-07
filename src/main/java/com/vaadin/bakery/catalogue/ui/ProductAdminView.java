package com.vaadin.bakery.catalogue.ui;

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
        // The grid owns the header bindings: a column is not in the component
        // tree, so it cannot decide when a binding starts or stops.
        var name = grid.addColumn(Product::getName).setSortable(true);
        name.setFlexGrow(2);
        Translations.bind(grid, name::setHeader, "admin.product.name");
        var category = grid.addColumn(product -> product.getCategory().getName()).setSortable(true);
        Translations.bind(grid, category::setHeader, "admin.product.category");

        // Two editable cells, validated before they are written.
        var price = grid.addEditColumn(Product::getPriceCents)
                .text((product, value) -> updatePrice(product, value));
        price.setSortable(true);
        Translations.bind(grid, price::setHeader, "admin.product.price");
        var stock = grid.addEditColumn(Product::getStockToday)
                .text((product, value) -> updateStock(product, value));
        stock.setSortable(true);
        Translations.bind(grid, stock::setHeader, "admin.product.stock");

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

        grid.addComponentColumn(product -> {
            var edit = Translations.bindText(new Button("", event -> editor.editProduct(
                    products.findById(product.getId()).orElseThrow())), "admin.edit");
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            var delete = Translations.bindText(new Button("", event -> delete(product)), "admin.delete");
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
            return new Div(edit, delete);
        }).setHeader("");

        // The filters belong to the columns they filter, so they live in a
        // header row inside the grid rather than in a bar above it. Scrolling
        // the columns scrolls the filters with them, which is the whole point.
        var filters = grid.appendHeaderRow();

        var search = new TextField();
        Translations.bind(search, search::setPlaceholder, "admin.product.search");
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setClearButtonVisible(true);
        search.setWidthFull();
        search.addValueChangeListener(event -> refresh());
        this.nameFilter = search;
        filters.getCell(name).setComponent(search);

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

        var toolbar = new Div(create);
        toolbar.addClassName("product-admin__toolbar");

        add(toolbar, grid);
        refresh();
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
        try {
            int cents = Integer.parseInt(value.trim());
            if (cents < 1) {
                throw new NumberFormatException();
            }
            product.setPriceCents(cents);
            catalogue.save(product);
        } catch (NumberFormatException invalid) {
            error(getTranslation("admin.product.price.invalid"));
            refresh();
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
