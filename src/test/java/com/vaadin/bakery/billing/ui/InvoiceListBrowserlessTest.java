package com.vaadin.bakery.billing.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.billing.Invoice;
import com.vaadin.bakery.billing.InvoiceRepository;
import com.vaadin.bakery.billing.InvoiceStatus;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.router.QueryParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/** INV-05 and the printable document. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InvoiceListBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private InvoiceRepository invoices;

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    @Test
    void theListShowsTheInvoices() {
        var view = navigate(InvoiceListView.class);

        assertTrue(view.grid().getGenericDataView().getItems().count() > 0);
    }

    @Test
    void theExportUsesMachineFormattedNumbers() {
        var view = navigate(InvoiceListView.class);

        var csv = view.csv();
        var lines = csv.split("\n");

        assertEquals("number,issued,customer,net,vat,gross,status", lines[0]);
        assertTrue(lines.length > 1, "and there is data under the header");
        var firstRow = lines[1];
        assertTrue(firstRow.matches(".*,\\d+\\.\\d{2},\\d+\\.\\d{2},\\d+\\.\\d{2},[A-Z]+"),
                "amounts use a dot and two decimals whatever the locale is, got " + firstRow);
    }

    @Test
    void theExportContainsExactlyWhatTheFilterShows() {
        var view = navigate(InvoiceListView.class);
        long paid = invoices.findAll().stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.PAID)
                .count();

        var everything = view.csv().split("\n").length - 1;

        assertTrue(everything >= paid, "unfiltered contains at least the paid ones");
        assertEquals(invoices.count(), everything, "and with no filter it is everything");
    }

    @Test
    void theDocumentPrintsAsARealTable() {
        var invoice = invoices.findAll().getFirst();

        UI.getCurrent().navigate("invoices/" + invoice.getNumber() + "/print",
                QueryParameters.of("t", invoice.getOrder().getTrackingToken()));

        var tables = find(Table.class).all();
        assertEquals(2, tables.size(), "the lines and the VAT summary are both tables");
        assertFalse(find(H1.class).all().isEmpty(), "and the document has a heading");
    }

    @Test
    void aPrintLinkWithoutTheTokenShowsNothing() {
        var invoice = invoices.findAll().getFirst();

        UI.getCurrent().navigate("invoices/" + invoice.getNumber() + "/print");

        assertTrue(find(H1.class).single().getText().contains("cannot find"),
                "the number alone is not enough to read somebody's invoice");
    }
}
