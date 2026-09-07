package com.vaadin.bakery.billing.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.ui.MainLayout;
import com.vaadin.bakery.billing.InvoiceRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * FIX-03. The print page carries no application shell.
 *
 * A page that exists to be printed has no business taking a drawer, a header
 * and a navigation menu onto the paper. The route opens in a new tab already;
 * what was missing was {@code autoLayout = false}, and nothing said so, because
 * a view inside a layout renders perfectly well.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class InvoicePrintBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private InvoiceRepository invoices;

    private void openAnInvoice() {
        var invoice = invoices.findAll().getFirst();
        UI.getCurrent().navigate("invoices/" + invoice.getNumber() + "/print",
                QueryParameters.of("t", invoice.getOrder().getTrackingToken()));
    }

    @Test
    void thePrintPageRendersOutsideTheShell() {
        openAnInvoice();

        assertFalse(find(InvoicePrintView.class).all().isEmpty(), "the invoice is on screen");
        assertTrue(find(MainLayout.class).all().isEmpty(),
                "and nothing of the application's navigation is around it");
    }

    /** The shell is still there for every other route, which is the control. */
    @Test
    void anOrdinaryRouteStillHasTheShell() {
        UI.getCurrent().navigate("shop");

        assertFalse(find(MainLayout.class).all().isEmpty(), "the storefront keeps its navigation");
    }
}
