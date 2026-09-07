package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
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
