package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.people.CustomerService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.PartialMatchMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

/** ADM-07 and ADM-08. Matching a fragment, not just a prefix. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "admin@bakery.test", roles = { "ADMIN" })
class PartialMatchBrowserlessTest extends SpringBrowserlessTest {

    @org.junit.jupiter.api.BeforeEach
    void signIn() {
        // After the browserless environment is up, see TestLogin for why.
        TestLogin.asAdmin();
    }

    @Autowired
    private CustomerService customers;

    @Autowired
    private com.vaadin.bakery.catalogue.ProductRepository products;

    @Test
    void theCategoryPickerIsConfiguredForPartialMatching() {
        var view = navigate(ProductAdminView.class);
        // A dialog has no content until it opens, so open it on a real product.
        view.editor().editProduct(products.findAll().getFirst());
        var pickers = find(ComboBox.class, view.editor()).all();

        assertTrue(pickers.stream().anyMatch(box -> box.getPartialMatchMode() == PartialMatchMode.FIRST_MATCH),
                "the category picker commits a partial match");
    }

    @Test
    void theCustomerSearchMatchesAnyFragment() {
        var customer = customers.findOrCreate("Bartolome", "Escalante", "bart.partial@example.com",
                "+34 611 222 333");

        assertTrue(customers.search("scalan", 10).contains(customer), "middle of the surname");
        assertTrue(customers.search("t.partial", 10).contains(customer), "middle of the email");
        assertTrue(customers.search("611 222", 10).contains(customer), "phone with a space in it");
        assertEquals(0, customers.search("   ", 10).size(), "whitespace finds nothing");
    }
}
