package com.vaadin.bakery.billing.ui;

import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.billing.Invoice;
import com.vaadin.bakery.billing.InvoiceRepository;
import com.vaadin.bakery.billing.InvoiceService;
import com.vaadin.bakery.billing.InvoiceStatus;
import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import jakarta.annotation.security.RolesAllowed;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Invoices, and the two things anybody does with them: find one and mark it
 * paid. The export writes machine formatted numbers, not locale formatted ones,
 * because it is going into a spreadsheet and not into a sentence.
 */
@Route("admin/invoices")
@PageTitle("Invoices")
@Menu(order = 26, title = "Invoices", icon = "vaadin:invoice")
@RolesAllowed({ Role.ADMIN_NAME, Role.BARISTA_NAME })
public class InvoiceListView extends VerticalLayout {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final InvoiceRepository invoices;
    private final InvoiceService invoiceService;
    private final Grid<Invoice> grid = new Grid<>();
    private final ValueSignal<String> search = new ValueSignal<>("");
    private final ValueSignal<InvoiceStatus> status = new ValueSignal<>(null);

    public InvoiceListView(InvoiceRepository invoices, InvoiceService invoiceService) {
        this.invoices = invoices;
        this.invoiceService = invoiceService;
        addClassName("invoice-list");
        setSizeFull();

        grid.setSizeFull();
        // A column is not in the component tree, so the grid owns the binding.
        var number = grid.addColumn(Invoice::getNumber).setAutoWidth(true);
        Translations.bind(grid, number::setHeader, "billing.invoice.number");
        var date = grid.addColumn(invoice -> invoice.getIssuedAt().format(DATE.withLocale(getLocale())))
                .setAutoWidth(true);
        Translations.bind(grid, date::setHeader, "billing.invoice.date");
        var customer = grid.addColumn(Invoice::getBillingName).setFlexGrow(2);
        Translations.bind(grid, customer::setHeader, "billing.invoice.customer");
        var gross = grid.addColumn(invoice -> invoice.gross().format(getLocale())).setAutoWidth(true);
        Translations.bind(grid, gross::setHeader, "cart.gross", "");
        var statusColumn = grid.addComponentColumn(this::statusBadge);
        Translations.bind(grid, statusColumn::setHeader, "billing.invoice.status");
        grid.addComponentColumn(this::actions).setHeader("");

        var searchField = new TextField();
        Translations.bind(searchField, searchField::setPlaceholder, "billing.invoice.search");
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setClearButtonVisible(true);
        searchField.addValueChangeListener(event -> search.set(event.getValue()));

        var statusFilter = new Select<InvoiceStatus>();
        Translations.bind(statusFilter, statusFilter::setLabel, "billing.invoice.status");
        statusFilter.setItems(InvoiceStatus.values());
        // The empty selection is passed to the label generator too, so it has to
        // survive a null.
        Translations.onLocale(statusFilter, locale -> statusFilter.setItemLabelGenerator(value -> value == null
                ? getTranslation(locale, "catalogue.category.all")
                : getTranslation(locale, value.translationKey())));
        statusFilter.setEmptySelectionAllowed(true);
        Translations.bind(statusFilter, statusFilter::setEmptySelectionCaption, "catalogue.category.all");
        statusFilter.addValueChangeListener(event -> status.set(event.getValue()));

        var export = Translations.bindText(new Button("", event -> exportCsv()), "billing.invoice.export");

        add(new Div(searchField, statusFilter, export), grid);
        // Re-running the load on a locale change is what redraws the cells whose
        // value provider formats a date or an amount.
        Translations.onLocale(this, locale -> reload(search.get(), status.get()));
    }

    private Span statusBadge(Invoice invoice) {
        var badge = Translations.bindText(new Span(), invoice.getStatus().translationKey());
        badge.getElement().getThemeList().add("badge " + switch (invoice.getStatus()) {
            case PAID -> "success";
            case VOID -> "error";
            case ISSUED -> "contrast";
        });
        return badge;
    }

    private Div actions(Invoice invoice) {
        var print = Translations.bindText(new Anchor("invoices/" + invoice.getNumber() + "/print?t="
                + invoice.getOrder().getTrackingToken(), ""), "billing.invoice.print");
        print.setTarget("_blank");

        var actions = new Div(print);
        if (invoice.getStatus() == InvoiceStatus.ISSUED) {
            var markPaid = Translations.bindText(new Button("", event -> {
                try {
                    invoiceService.markPaid(invoice);
                    reload(search.peek(), status.peek());
                } catch (DomainException failure) {
                    Notification.show(getTranslation(failure.translationKey(), failure.arguments()));
                }
            }), "billing.invoice.markPaid");
            markPaid.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            actions.add(markPaid);
        }
        return actions;
    }

    private List<Invoice> filtered(String term, InvoiceStatus wanted) {
        return invoices.findAll().stream()
                .filter(invoice -> wanted == null || invoice.getStatus() == wanted)
                .filter(invoice -> term == null || term.isBlank()
                        || invoice.getNumber().toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT))
                        || invoice.getBillingName().toLowerCase(Locale.ROOT)
                                .contains(term.toLowerCase(Locale.ROOT)))
                .sorted((left, right) -> right.getNumber().compareTo(left.getNumber()))
                .toList();
    }

    private void reload(String term, InvoiceStatus wanted) {
        grid.setItems(filtered(term, wanted));
    }

    /** Exactly the rows the filter shows, with numbers a spreadsheet understands. */
    String csv() {
        var rows = new StringBuilder("number,issued,customer,net,vat,gross,status\n");
        filtered(search.peek(), status.peek()).forEach(invoice -> rows
                .append(invoice.getNumber()).append(',')
                .append(invoice.getIssuedAt()).append(',')
                .append('"').append(invoice.getBillingName().replace("\"", "\"\"")).append('"').append(',')
                .append(invoice.getNetCents() / 100).append('.')
                .append(String.format("%02d", invoice.getNetCents() % 100)).append(',')
                .append(invoice.getVatCents() / 100).append('.')
                .append(String.format("%02d", invoice.getVatCents() % 100)).append(',')
                .append(invoice.getGrossCents() / 100).append('.')
                .append(String.format("%02d", invoice.getGrossCents() % 100)).append(',')
                .append(invoice.getStatus().name()).append('\n'));
        return rows.toString();
    }

    private void exportCsv() {
        var content = csv();
        var bytes = content.getBytes(StandardCharsets.UTF_8);
        Notification.show(getTranslation("billing.invoice.exported", bytes.length));
    }

    Grid<Invoice> grid() {
        return grid;
    }
}
