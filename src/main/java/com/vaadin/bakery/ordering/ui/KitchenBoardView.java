package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.security.CurrentUser;
import com.vaadin.bakery.ordering.KitchenBoard;
import com.vaadin.bakery.ordering.KitchenTicket;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import jakarta.annotation.security.RolesAllowed;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The board on the kitchen wall.
 *
 * Everything on this screen comes from one shared signal, so a ticket moved by
 * the baker at the oven moves here, on the counter screen, and on the
 * customer's tracking page, with nothing else written to make that happen.
 *
 * The layout is built for the room it lives in. The three columns fill the
 * height of the screen and scroll inside themselves, so the column headings
 * stay put and a busy Saturday does not push the last column off the bottom of
 * the wall. The day's production summary is a detail panel rather than
 * something below the fold: on a wall display nobody ever scrolls, so anything
 * down there may as well not exist.
 */
@Route("kitchen")
@PageTitle("Kitchen")
@Menu(order = 11, title = "Kitchen", icon = "vaadin:fire")
@RolesAllowed({ Role.ADMIN_NAME, Role.BAKER_NAME })
public class KitchenBoardView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final KitchenBoard board;
    private final CurrentUser currentUser;
    private final Div columns = new Div();
    private final Div summary = new Div();
    private final MasterDetailLayout layout = new MasterDetailLayout();
    private boolean summaryOpen;

    public KitchenBoardView(KitchenBoard board, CurrentUser currentUser) {
        this.board = board;
        this.currentUser = currentUser;
        addClassName("kitchen-board");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        board.ensureLoaded();
        columns.addClassName("kitchen-board__columns");
        summary.addClassName("kitchen-board__summary");

        layout.addClassName("kitchen-board__layout");
        layout.setSizeFull();
        layout.setMaster(new Div(header(), columns) {
            {
                addClassName("kitchen-board__master");
            }
        });
        // The summary rides over the board as an overlay, so the columns keep
        // their width. With no explicit size the overlay takes the minimum width
        // of its content, which the nowrap rows of the table pin to the table.
        layout.setForceOverlay(true);
        add(layout);

        // One effect over the tickets and the locale: the board redraws when a
        // ticket moves, and again when the language changes.
        Translations.onLocale(this, locale -> {
            var tickets = board.tickets().get().stream().map(signal -> signal.get()).toList();
            renderColumns(tickets, locale);
            renderSummary(tickets, locale);
        });
    }

    private Div header() {
        var title = Translations.bindText(new H2(), "kitchen.title");

        var toggle = Translations.bindText(new Button(new Icon(VaadinIcon.CLIPBOARD_TEXT),
                event -> toggleSummary()), "kitchen.summary");
        toggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        var header = new Div(title, toggle);
        header.addClassName("kitchen-board__header");
        return header;
    }

    private void toggleSummary() {
        summaryOpen = !summaryOpen;
        layout.setDetail(summaryOpen ? summary : null);
    }

    private void renderColumns(List<KitchenTicket> tickets, Locale locale) {
        columns.removeAll();
        for (OrderState state : OrderState.kitchenColumns()) {
            var inState = tickets.stream().filter(ticket -> ticket.state() == state).toList();

            var heading = new Div(new H3(getTranslation(locale, state.translationKey())),
                    new Span(String.valueOf(inState.size())));
            heading.addClassName("kitchen-board__column-header");

            // The scroller, not the column, is what overflows. The heading stays
            // where the baker last looked for it.
            var scroller = new Div();
            scroller.addClassName("kitchen-board__scroller");
            if (inState.isEmpty()) {
                var empty = new Span(getTranslation(locale, "kitchen.empty"));
                empty.addClassName("kitchen-board__empty");
                scroller.add(empty);
            }
            inState.forEach(ticket -> scroller.add(card(ticket, locale)));

            var column = new Div(heading, scroller);
            column.addClassName("kitchen-board__column");
            columns.add(column);
        }
    }

    private Component card(KitchenTicket ticket, Locale locale) {
        var card = new Div();
        card.addClassName("kitchen-board__ticket");

        var time = new Span(ticket.pickupTime().format(TIME));
        time.addClassName("kitchen-board__ticket-time");
        var who = new Span(ticket.customerFirstName());
        who.addClassName("kitchen-board__ticket-who");
        var reference = new Span(ticket.reference());
        reference.addClassName("kitchen-board__ticket-reference");

        var header = new Div(time, who, reference);
        header.addClassName("kitchen-board__ticket-header");
        card.add(header);

        var lines = new Div();
        lines.addClassName("kitchen-board__ticket-lines");
        ticket.lines().forEach(line -> lines.add(new Div(new Span(line))));
        card.add(lines);

        if (!ticket.allergenKeys().isEmpty()) {
            var allergens = new Span(ticket.allergenKeys().stream()
                    .map(key -> getTranslation(locale, key))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
            allergens.getElement().getThemeList().add("badge error small");
            card.add(allergens);
        }

        var actions = new Div();
        actions.addClassName("kitchen-board__ticket-actions");

        var claim = new Button(ticket.isClaimed()
                ? getTranslation(locale, "kitchen.claimedBy", ticket.assignedBakerName())
                : getTranslation(locale, "kitchen.claim"), event -> claim(ticket));
        claim.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        actions.add(claim);

        ticket.state().allowedTargets().stream()
                .filter(OrderState::isActiveInKitchen)
                .forEach(target -> {
                    var advance = new Button(getTranslation(locale, "board.action." + target.name()),
                            event -> board.advance(ticket, target, currentUser.get().orElse(null)));
                    advance.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    actions.add(advance);
                });

        var actor = currentUser.get().orElse(null);
        if (ticket.state() == OrderState.READY && actor != null
                && OrderState.PICKED_UP.settableBy(actor.getRole())) {
            actions.add(new Button(getTranslation(locale, "board.action.PICKED_UP"),
                    event -> board.advance(ticket, OrderState.PICKED_UP, currentUser.get().orElse(null))));
        }

        card.add(actions);
        return card;
    }

    private void claim(KitchenTicket ticket) {
        var baker = currentUser.get().orElse(null);
        if (baker == null) {
            return;
        }
        board.claim(ticket, baker).ifPresent(name -> {
            var dialog = new ConfirmDialog();
            dialog.setHeader(getTranslation("kitchen.steal.title"));
            dialog.setText(getTranslation("kitchen.steal.body", name));
            dialog.setCancelable(true);
            dialog.setConfirmText(getTranslation("kitchen.steal.confirm"));
            dialog.addConfirmListener(event -> {
                board.tickets().peek().stream()
                        .filter(signal -> signal.peek().orderId().equals(ticket.orderId()))
                        .findFirst()
                        .ifPresent(signal -> signal.update(value -> value.withBaker(baker.getId(),
                                baker.getFullName())));
                Notification.show(getTranslation("kitchen.steal.done"));
            });
            dialog.open();
        });
    }

    /**
     * Units per product for the day, as a real table. The Table family produces
     * table markup rather than a grid of divs, which is what a screen reader and
     * a sheet printed for the wall both want.
     */
    private void renderSummary(List<KitchenTicket> tickets, Locale locale) {
        summary.removeAll();
        Map<String, Integer> perProduct = new LinkedHashMap<>();
        tickets.forEach(ticket -> ticket.lines().forEach(line -> {
            var parts = line.split(" x ", 2);
            if (parts.length == 2) {
                var name = parts[1].replaceAll("\\s*\\(.*\\)$", "");
                perProduct.merge(name, Integer.parseInt(parts[0].trim()), Integer::sum);
            }
        }));

        var table = new Table();
        table.addHeaderRow(getTranslation(locale, "kitchen.summary.product"),
                getTranslation(locale, "kitchen.summary.units"));
        perProduct.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> table.addRowWithHeader(entry.getKey(), String.valueOf(entry.getValue())));

        var close = new Button(new Icon(VaadinIcon.CLOSE_SMALL), event -> toggleSummary());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        close.setAriaLabel(getTranslation(locale, "kitchen.summary.close"));

        var heading = new Div(new H3(getTranslation(locale, "kitchen.summary")), close);
        heading.addClassName("kitchen-board__summary-header");

        var body = new Div(table);
        body.addClassName("kitchen-board__summary-body");

        summary.add(heading, new Span(getTranslation(locale, "kitchen.summary.caption")), body);
    }
}
