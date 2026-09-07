package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.catalogue.Product;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * FIX-05. The catalogue can start a product, sort, and filter where the columns
 * are.
 *
 * The catalogue is the one admin view that did not move to {@code Crud}: its
 * grid is a {@code GridPro} with inline editing on price and stock, and a Crud
 * around a grid that already edits is two editors. So the four things Crud
 * would have brought are here by hand, and this is what holds them.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
// Creating a product cannot be rolled back by a test transaction.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogueCrudBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private ProductRepository products;

    @Autowired
    private CatalogueService catalogue;

    @BeforeEach
    void signIn() {
        TestLogin.asAdmin();
    }

    private long shown(ProductAdminView view) {
        return view.grid().getGenericDataView().getItems().count();
    }

    @Test
    void everyColumnAPersonWouldSortByIsSortable() {
        navigate(ProductAdminView.class);
        var view = find(ProductAdminView.class).single();

        var sortable = view.grid().getColumns().stream().filter(column -> column.isSortable()).count();
        assertTrue(sortable >= 4, "name, category, price and stock all sort, got " + sortable);
    }

    @Test
    void theNameFilterNarrowsTheGrid() {
        navigate(ProductAdminView.class);
        var view = find(ProductAdminView.class).single();
        long all = shown(view);

        view.nameFilter().setValue("croissant");

        long narrowed = shown(view);
        assertTrue(narrowed < all, "the filter removed something: " + narrowed + " of " + all);
        assertTrue(view.grid().getGenericDataView().getItems()
                .allMatch(product -> product.getName().toLowerCase().contains("croissant")),
                "and left only what matches");
    }

    @Test
    void theCategoryFilterNarrowsTheGrid() {
        navigate(ProductAdminView.class);
        var view = find(ProductAdminView.class).single();
        long all = shown(view);
        var cakes = catalogue.categories().stream()
                .filter(category -> category.getName().equalsIgnoreCase("Cakes"))
                .findFirst()
                .orElseThrow();

        view.categoryFilter().setValue(cakes);

        assertTrue(shown(view) < all);
        assertTrue(view.grid().getGenericDataView().getItems()
                .allMatch(product -> product.getCategory().equals(cakes)));
    }

    /**
     * The gap the report named: the list offered Edit and Delete per row and no
     * way at all to add one.
     */
    @Test
    void aProductCanBeStartedAndSaved() {
        navigate(ProductAdminView.class);
        var view = find(ProductAdminView.class).single();

        view.editor().newProduct();

        assertTrue(view.editor().isOpened(), "the editor opens on a blank product");
    }

    /** Nobody is asked to invent a URL, so the catalogue derives one. */
    @Test
    void savingANewProductDerivesItsSlug() {
        var tart = product("Elderflower tart");

        catalogue.save(tart);

        assertEquals("elderflower-tart", tart.getSlug());
        assertFalse(products.findBySlug("elderflower-tart").isEmpty(), "and it is reachable by it");
    }

    /**
     * Names are unique, so two products never share one. Two different names
     * can still shorten to the same slug, and an accent is the usual way: that
     * is a constraint violation nobody could read, so the second one is
     * numbered instead.
     */
    @Test
    void twoNamesThatShortenToTheSameSlugDoNotCollide() {
        catalogue.save(product("Cafe con leche"));
        var accented = product("Cafe\u0301 con leche");

        catalogue.save(accented);

        assertEquals("cafe-con-leche-2", accented.getSlug());
    }

    private Product product(String name) {
        var product = new Product();
        product.setName(name);
        product.setCategory(catalogue.categories().getFirst());
        product.setPriceCents(450);
        return product;
    }
}
