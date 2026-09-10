package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.base.security.CurrentUser;
import com.vaadin.bakery.ordering.KitchenBoard;
import com.vaadin.bakery.ordering.KitchenTicket;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.people.Role;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;

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
@Menu(order = 12, title = "Kitchen", icon = "vaadin:fire")
@RolesAllowed({ Role.ADMIN_NAME, Role.BAKER_NAME })
public class KitchenBoardView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final KitchenBoard board;
    private final CurrentUser currentUser;
    /** Who a ticket can be given to, which is a question about people. */
    private final com.vaadin.bakery.people.UserService people;
    private final Duration staleAfter;
    /** Checked more often than the threshold, so the marker is never a whole
     * period late in appearing. */
    private final Duration staleCheckEvery;
    private final Div columns = new Div();
    private final Div summary = new Div();
    private final MasterDetailLayout layout = new MasterDetailLayout();
    private final Span staleMarker = new Span();
    private boolean summaryOpen;

    /**
     * What each ticket looked like the last time this screen drew it, so the
     * next draw can tell which ones somebody else moved.
     */
    private final Map<Long, KitchenTicket> asLastDrawn = new HashMap<>();

    /**
     * Tickets this screen moved itself. They are not flashed here: the baker
     * who pressed the button knows what they pressed, and a screen that flashes
     * its own work teaches everybody to ignore the flash.
     */
    private final Set<Long> movedHere = new HashSet<>();

    public KitchenBoardView(KitchenBoard board, CurrentUser currentUser,
            com.vaadin.bakery.people.UserService people,
            @Value("${bakery.kitchen.stale-after:PT2M}") Duration staleAfter) {
        this.board = board;
        this.currentUser = currentUser;
        this.people = people;
        this.staleAfter = staleAfter;
        this.staleCheckEvery = staleAfter.dividedBy(4);
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

        // A board on a wall is read from three metres away and nobody touches
        // it for an hour. The one thing it cannot do is look current while the
        // connection behind it has gone.
        whenAttached(attach -> attach.getUI().get().triggerAfter(staleCheckEvery, this::checkStaleness));
    }

    /**
     * Whether the screen has heard from the server recently enough to be
     * believed, from {@code UI.getLastUpdateSentTimestamp}.
     *
     * It re-arms itself: {@code triggerAfter} fires once, so each check books
     * the next one. The registration goes with the UI, and a detached UI never
     * gets the callback, which is why nothing here has to be unsubscribed.
     */
    private void checkStaleness() {
        getUI().ifPresent(ui -> {
            // Wall clock, not the application's Clock bean. That one is frozen
            // in tests and shifted for the demo dataset, and this is measuring
            // how long a browser has been quiet, which is real time.
            showStale(isStale(ui.getLastUpdateSentTimestamp(), Instant.now()));
            ui.triggerAfter(staleCheckEvery, this::checkStaleness);
        });
    }

    /** The arithmetic on its own, so a test does not have to wait two minutes. */
    boolean isStale(Instant lastUpdate, Instant now) {
        return Duration.between(lastUpdate, now).compareTo(staleAfter) >= 0;
    }

    void showStale(boolean stale) {
        staleMarker.setVisible(stale);
    }

    boolean isShowingStale() {
        return staleMarker.isVisible();
    }

    /** Reloads from the service, which is what the marker is asking for. */
    void refresh() {
        board.reload();
        showStale(false);
    }

    private Div header() {
        var title = Translations.bindText(new H2(), "kitchen.title");

        Translations.bindText(staleMarker, "kitchen.stale");
        staleMarker.addClassName("kitchen-board__stale");
        staleMarker.getElement().getThemeList().add("badge contrast small");
        staleMarker.setVisible(false);

        var reload = Translations.bindText(new Button(new Icon(VaadinIcon.REFRESH),
                event -> refresh()), "kitchen.refresh");
        reload.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        var toggle = Translations.bindText(new Button(new Icon(VaadinIcon.CLIPBOARD_TEXT),
                event -> toggleSummary()), "kitchen.summary");
        toggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        var header = new Div(title, staleMarker, reload, toggle);
        header.addClassName("kitchen-board__header");
        return header;
    }

    private void toggleSummary() {
        summaryOpen = !summaryOpen;
        layout.setDetail(summaryOpen ? summary : null);
    }

    /**
     * Whether this ticket changed since this screen last drew it, and somebody
     * else did it. Both halves matter: without the first every redraw flashes
     * the whole board, and without the second a baker's own button flashes back
     * at them.
     */
    /** Moving a ticket from this screen, which is the half that does not flash. */
    private void moveHere(KitchenTicket ticket, OrderState target) {
        movedHere.add(ticket.orderId());
        board.advance(ticket, target, currentUser.get().orElse(null));
    }

    private boolean movedElsewhere(KitchenTicket ticket) {
        var before = asLastDrawn.get(ticket.orderId());
        return before != null && !before.equals(ticket) && !movedHere.contains(ticket.orderId());
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
            dropInto(scroller, state, locale);
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

        // What this draw saw becomes what the next one compares against.
        asLastDrawn.clear();
        tickets.forEach(ticket -> asLastDrawn.put(ticket.orderId(), ticket));
        movedHere.clear();
    }

    /**
     * A column that accepts a card dragged onto it.
     *
     * The drop is the same move the buttons on the card make, refused the same
     * way and written to the same signal, so the two gestures cannot disagree.
     * The buttons stay: dragging is a mouse gesture and a kitchen board has to
     * be usable from a keyboard, so this is the shortcut and they are the path.
     */
    private void dropInto(Div scroller, OrderState target, Locale locale) {
        var drop = DropTarget.create(scroller);
        drop.setDropEffect(com.vaadin.flow.component.dnd.DropEffect.MOVE);
        drop.addDropListener(event -> {
            var dragged = event.getDragData().orElse(null);
            if (dragged instanceof KitchenTicket ticket && !dropOnto(ticket, target)) {
                Notification.show(getTranslation(locale, "kitchen.move.refused",
                        getTranslation(locale, target.translationKey())));
            }
        });
    }

    /**
     * What a drop decides, with no gesture in it.
     *
     * The rules are the ones the buttons obey, and a column is not permission:
     * a ticket only moves where its state allows and where this person's role
     * allows. Separate from the listener because the gesture cannot be made in
     * a test and the decision can, and because the two must not be allowed to
     * drift into two different sets of rules.
     *
     * @return whether the ticket moved
     */
    boolean dropOnto(KitchenTicket ticket, OrderState target) {
        if (ticket.state() == target) {
            return true;
        }
        var actor = currentUser.get().orElse(null);
        if (!ticket.state().canMoveTo(target)
                || (actor != null && !target.settableBy(actor.getRole()))) {
            return false;
        }
        moveHere(ticket, target);
        return true;
    }

    private Component card(KitchenTicket ticket, Locale locale) {
        var card = new Div();
        card.addClassName("kitchen-board__ticket");
        // The whole card is the handle: it is what a baker points at, and on a
        // wall tablet there is no room for a grip of its own.
        var drag = DragSource.create(card);
        drag.setDragData(ticket);
        drag.setEffectAllowed(com.vaadin.flow.component.dnd.EffectAllowed.MOVE);
        drag.addDragStartListener(event -> columns.addClassName("kitchen-board__columns--dragging"));
        drag.addDragEndListener(event -> columns.removeClassName("kitchen-board__columns--dragging"));
        if (movedElsewhere(ticket)) {
            // The class rides on a freshly built card and a CSS animation ends
            // it, so nothing has to be scheduled to take it off again: the next
            // redraw builds the card without it.
            card.addClassName("kitchen-board__ticket--moved");
        }

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
        actions.add(assignment(ticket, locale));

        ticket.state().allowedTargets().stream()
                .filter(OrderState::isActiveInKitchen)
                .forEach(target -> {
                    var advance = new Button(getTranslation(locale, "board.action." + target.name()),
                            event -> moveHere(ticket, target));
                    advance.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    actions.add(advance);
                });

        var actor = currentUser.get().orElse(null);
        if (ticket.state() == OrderState.READY && actor != null
                && OrderState.PICKED_UP.settableBy(actor.getRole())) {
            actions.add(new Button(getTranslation(locale, "board.action.PICKED_UP"),
                    event -> moveHere(ticket, OrderState.PICKED_UP)));
        }

        card.add(actions);
        return card;
    }

    /**
     * Who is making this one, as a picker rather than as a button.
     *
     * "Claim this" only ever meant one thing, which is right for a baker at the
     * oven and wrong for everybody else: an administrator pressing it was
     * assigning the ticket to an administrator. So the control names a person.
     * The current user comes first when they are a baker, and is ticked when
     * the ticket is already theirs, so claiming your own work is still the top
     * of the list; anybody else assigns freely from the same list.
     */
    private Component assignment(KitchenTicket ticket, Locale locale) {
        var menu = new com.vaadin.flow.component.menubar.MenuBar();
        menu.addThemeVariants(com.vaadin.flow.component.menubar.MenuBarVariant.LUMO_TERTIARY);
        menu.addClassName("kitchen-board__assign");
        var root = menu.addItem(ticket.isClaimed()
                ? getTranslation(locale, "kitchen.claimedBy", ticket.assignedBakerName())
                : getTranslation(locale, "kitchen.claim"));
        root.setAriaLabel(getTranslation(locale, "kitchen.assign"));

        var me = currentUser.get().orElse(null);
        var candidates = new java.util.ArrayList<>(people.bakers());
        if (me != null && me.getRole() == Role.BAKER) {
            candidates.removeIf(baker -> baker.getId().equals(me.getId()));
            candidates.addFirst(me);
        }
        if (candidates.isEmpty()) {
            root.setEnabled(false);
            return menu;
        }
        for (var baker : candidates) {
            var entry = root.getSubMenu().addItem(baker.getFullName(),
                    event -> assign(ticket, baker));
            entry.setCheckable(true);
            entry.setChecked(baker.getId().equals(ticket.assignedBakerId()));
        }
        return menu;
    }

    /** Package private so a test can assign without opening a menu. */
    void assign(KitchenTicket ticket, com.vaadin.bakery.people.User baker) {
        if (baker == null) {
            return;
        }
        movedHere.add(ticket.orderId());
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
