package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/** ADM-02 and the availability switch reaching the storefront. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "admin@bakery.test", roles = { "ADMIN" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductAdminBrowserlessTest extends SpringBrowserlessTest {

    @org.junit.jupiter.api.BeforeEach
    void signIn() {
        // After the browserless environment is up, see TestLogin for why.
        TestLogin.asAdmin();
    }

    @Autowired
    private ProductRepository products;

    @Autowired
    private CatalogueService catalogue;

    @Test
    void theGridListsEveryProduct() {
        navigate(ProductAdminView.class);
        var grid = find(com.vaadin.flow.component.gridpro.GridPro.class).single();
        assertEquals(products.count(), grid.getGenericDataView().getItems().count());
    }

    @Test
    void turningTheAvailabilitySwitchOffRemovesItFromTheStorefront() {
        var product = products.findBySlug("ciabatta").orElseThrow();
        assertTrue(product.isAvailable());
        navigate(ProductAdminView.class);

        // Components rendered inside a Grid component column are not part of the
        // searchable component tree in a browserless test, so the switch itself
        // is exercised by the browser test. What matters here is the effect.
        product.setAvailable(false);
        catalogue.save(product);

        assertTrue(catalogue.availableProducts().stream()
                .noneMatch(candidate -> candidate.getSlug().equals("ciabatta")),
                "the storefront query stops offering it at once");

        var restored = products.findBySlug("ciabatta").orElseThrow();
        restored.setAvailable(true);
        products.saveAndFlush(restored);
    }

    /**
     * POL2-09. Edit and Delete were two words at the end of every row, which is
     * the same two words on every line of the table and the width the product
     * name wanted. They are icons in the first column now, and an icon keeps
     * its name where a screen reader can still read it.
     */
    @Test
    void theRowCarriesTwoNamedIconsInTheFirstColumn() {
        var view = navigate(ProductAdminView.class);

        assertEquals("actions", view.grid().getColumns().getFirst().getKey(),
                "what you can do to a row is the first thing on it");

        // The renderer's own output: a component inside a grid cell is not part
        // of the component tree a browserless test can search.
        var actions = view.rowActions(products.findBySlug("ciabatta").orElseThrow());
        var buttons = actions.getChildren()
                .filter(com.vaadin.flow.component.button.Button.class::isInstance)
                .map(com.vaadin.flow.component.button.Button.class::cast)
                .toList();

        assertEquals(2, buttons.size(), "edit and delete");
        assertEquals(List.of("Edit", "Delete"),
                buttons.stream().map(button -> button.getAriaLabel().orElse("")).toList(),
                "each one named, because neither carries a word");
        assertTrue(buttons.stream().allMatch(button -> button.getText().isBlank()),
                "and neither carries a word");
    }

    /**
     * POL2-10. The editor's photo frame only ever had a source after somebody
     * uploaded one, so an existing product with a picture opened showing an
     * empty box and looked like a product with no picture.
     */
    @Test
    void theEditorOpensShowingTheProductsOwnPhoto() {
        var view = navigate(ProductAdminView.class);
        var editor = view.editor();

        editor.editProduct(products.findBySlug("ciabatta").orElseThrow());

        var source = editor.photoSource();
        assertTrue(source != null && !source.isBlank(), "the frame points somewhere: " + source);
        assertTrue(source.contains("/images/"), "at a product image: " + source);
        editor.close();
    }

    @Test
    void aProductWithOrdersCannotBeDeletedFromTheAdminEither() {
        var product = products.findBySlug("butter-croissant").orElseThrow();
        navigate(ProductAdminView.class);

        var failure = org.junit.jupiter.api.Assertions.assertThrows(
                com.vaadin.bakery.base.error.DomainException.Conflict.class,
                () -> catalogue.delete(product));
        assertEquals("catalogue.product.inUse", failure.translationKey());
    }
}
