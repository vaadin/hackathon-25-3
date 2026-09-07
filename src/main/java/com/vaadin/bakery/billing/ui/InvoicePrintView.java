package com.vaadin.bakery.billing.ui;

import com.vaadin.bakery.base.Money;
import com.vaadin.bakery.billing.InvoiceService;
import com.vaadin.bakery.billing.InvoiceStatus;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.bakery.base.ui.AppearanceSettings;
import com.vaadin.bakery.base.ui.ThemeBinding;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

/**
 * The document somebody prints.
 *
 * Built with the Table family, so what comes out of the printer is a real table
 * with header cells, and the print stylesheet hides the application shell. No
 * PDF library: printing to PDF from the browser is the deliverable, and saying
 * so is more honest than shipping a second rendering engine.
 *
 * Anonymous by route, protected by the invoice number plus the order's tracking
 * token, exactly like the tracking page.
 */
// autoLayout = false: a page that exists to be printed has no business
// carrying a drawer, a header and a navigation menu into the paper.
@Route(value = "invoices/:number/print", autoLayout = false)
@PageTitle("Invoice")
@AnonymousAllowed
public class InvoicePrintView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final InvoiceService invoices;

    public InvoicePrintView(InvoiceService invoices, AppearanceSettings appearance) {
        this.invoices = invoices;
        // Outside the shell as well, since autoLayout is off, so it applies the
        // theme itself or it prints unstyled.
        ThemeBinding.apply(this, appearance);
        addClassName("invoice-print");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        var number = event.getRouteParameters().get("number").orElse("");
        var token = event.getLocation().getQueryParameters().getParameters()
                .getOrDefault("t", java.util.List.of()).stream().findFirst().orElse(null);

        var invoice = invoices.document(number, token).orElse(null);
        if (invoice == null) {
            add(new H1(getTranslation("billing.print.notFound")));
            return;
        }
        render(invoice);
    }

    private void render(com.vaadin.bakery.billing.InvoiceDocument invoice) {
        if (invoice.status() == InvoiceStatus.VOID) {
            var watermark = new Div(new Span(getTranslation("billing.print.void")));
            watermark.addClassName("invoice-print__watermark");
            add(watermark);
        }

        var header = new Div();
        header.addClassName("invoice-print__header");
        header.add(new H1(getTranslation("app.name")),
                new Paragraph(getTranslation("billing.print.number", invoice.number())),
                new Paragraph(getTranslation("billing.print.issued",
                        invoice.issuedAt().format(DATE.withLocale(getLocale())),
                        invoice.dueAt().format(DATE.withLocale(getLocale())))));

        var billTo = new Div();
        billTo.addClassName("invoice-print__billing");
        billTo.add(new Paragraph(getTranslation("billing.print.billTo")),
                new Paragraph(invoice.billingName()),
                new Paragraph(invoice.billingEmail()));
        if (invoice.hasAddress()) {
            billTo.add(new Paragraph(invoice.street()),
                    new Paragraph(invoice.postalCode() + " " + invoice.city()));
        }
        if (invoice.vatId() != null && !invoice.vatId().isBlank()) {
            billTo.add(new Paragraph(getTranslation("billing.print.vatId", invoice.vatId())));
        }

        var lines = new Table();
        lines.addClassName("invoice-print__lines");
        lines.setCaptionText(getTranslation("billing.print.lines"));
        lines.addHeaderRow(getTranslation("billing.print.description"), getTranslation("billing.print.quantity"),
                getTranslation("billing.print.unit"), getTranslation("billing.print.vat"),
                getTranslation("billing.print.net"));
        invoice.lines().forEach(line -> lines.addRowWithHeader(line.description(),
                String.valueOf(line.quantity()),
                line.unitPrice().format(getLocale()),
                line.vatRatePercent() + " percent",
                line.net().format(getLocale())));

        // VAT summarised per rate, which is what an accountant actually reads.
        var perRate = new LinkedHashMap<Integer, int[]>();
        invoice.lines().forEach(line -> {
            var slot = perRate.computeIfAbsent(line.vatRatePercent(), key -> new int[2]);
            slot[0] += line.net().cents();
            slot[1] += line.vat().cents();
        });

        var vatSummary = new Table();
        vatSummary.addClassName("invoice-print__vat");
        vatSummary.setCaptionText(getTranslation("billing.print.vatSummary"));
        vatSummary.addHeaderRow(getTranslation("billing.print.rate"), getTranslation("billing.print.net"),
                getTranslation("billing.print.vat"));
        perRate.forEach((rate, amounts) -> vatSummary.addRowWithHeader(rate + " percent",
                Money.ofCents(amounts[0]).format(getLocale()),
                Money.ofCents(amounts[1]).format(getLocale())));

        var totals = new Div();
        totals.addClassName("invoice-print__totals");
        totals.add(new Paragraph(getTranslation("cart.net", invoice.net().format(getLocale()))),
                new Paragraph(getTranslation("cart.vat", invoice.vat().format(getLocale()))),
                new Paragraph(getTranslation("cart.gross", invoice.gross().format(getLocale()))));

        var terms = new Paragraph(invoice.paid()
                ? getTranslation("billing.print.paid")
                : getTranslation("billing.print.terms", invoice.dueAt().format(DATE.withLocale(getLocale()))));
        terms.addClassName("invoice-print__terms");

        add(header, billTo, lines, vatSummary, totals, terms);
    }
}
