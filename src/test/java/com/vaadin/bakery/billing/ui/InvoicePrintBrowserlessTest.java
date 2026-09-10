package com.vaadin.bakery.billing.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.ui.MainLayout;
import com.vaadin.bakery.billing.InvoiceRepository;
import com.vaadin.bakery.billing.InvoiceService;
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

    @Autowired
    private InvoiceService invoiceService;

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

    /**
     * POL2-12. The bakery's own details are written once and print on every
     * invoice. Before this the document carried the application's name and
     * nothing else, so the address, the company number and the VAT
     * registration that a real invoice has to state were nowhere at all.
     */
    @Test
    void theBakerysOwnDetailsPrintOnTheDocument() {
        var written = invoiceService.bakeryHeader();
        try {
            invoiceService.saveBakeryHeader("## Panaderia Vaadin\nCalle Mayor 1, Madrid");
            openAnInvoice();

            // The letterhead is a Markdown component, and markdown is rendered
            // in the browser from a property: none of it is in the server side
            // element text, which is why this reads the property.
            var letterhead = find(com.vaadin.flow.component.markdown.Markdown.class).single();
            assertTrue(letterhead.getContent().contains("Panaderia Vaadin"), letterhead.getContent());
            assertTrue(letterhead.getContent().contains("Calle Mayor 1, Madrid"), letterhead.getContent());
            assertTrue(letterhead.isLineBreaks(), "and a line somebody typed prints as a line");
        } finally {
            invoiceService.saveBakeryHeader(written);
        }
    }

    /** With nothing written, the document prints what it always printed. */
    @Test
    void withNoDetailsWrittenTheDocumentStillCarriesTheName() {
        var written = invoiceService.bakeryHeader();
        try {
            invoiceService.saveBakeryHeader("");
            openAnInvoice();

            assertTrue(find(com.vaadin.flow.component.markdown.Markdown.class).all().isEmpty(),
                    "no letterhead is rendered at all");
            assertTrue(find(com.vaadin.flow.component.html.H1.class).single().getText().contains("Bakery"),
                    "the name is the fallback, not an empty letterhead");
        } finally {
            invoiceService.saveBakeryHeader(written);
        }
    }

    /** The shell is still there for every other route, which is the control. */
    @Test
    void anOrdinaryRouteStillHasTheShell() {
        UI.getCurrent().navigate("shop");

        assertFalse(find(MainLayout.class).all().isEmpty(), "the storefront keeps its navigation");
    }
}
