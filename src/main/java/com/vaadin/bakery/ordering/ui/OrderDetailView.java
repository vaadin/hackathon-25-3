package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.SafeHtml;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.base.security.CurrentUser;
import com.vaadin.bakery.billing.InvoiceService;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderActivity;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.people.User;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One order, and the things this role may do to it right now. State changes are
 * buttons rather than a combo box, because what a barista can do to an order
 * depends on where the order is, and a list of impossible options is noise.
 */
@Route(value = OrderBoardView.ROUTE + "/:reference", layout = OrderBoardView.class)
@PageTitle("Order")
@RolesAllowed({ Role.ADMIN_NAME, Role.BAKER_NAME, Role.BARISTA_NAME })
public class OrderDetailView extends VerticalLayout implements BeforeEnterObserver, BoardPanel {

    private static final Logger LOG = LoggerFactory.getLogger(OrderDetailView.class);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE-d-MMM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final OrderService orders;
    private final InvoiceService invoices;
    private final CurrentUser currentUser;
    private final CatalogueService catalogue;
    private final OrderActivity activity;
    private final UnsavedChanges unsaved = new UnsavedChanges();

    public OrderDetailView(OrderService orders, InvoiceService invoices, CurrentUser currentUser,
            CatalogueService catalogue, OrderActivity activity) {
        this.orders = orders;
        this.activity = activity;
        this.invoices = invoices;
        this.currentUser = currentUser;
        this.catalogue = catalogue;
        addClassName("order-detail");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        unsaved.settled();
        var reference = event.getRouteParameters().get("reference").orElse("");
        var order = orders.byReference(reference).orElse(null);
        if (order == null) {
            add(Translations.bindText(new H2(), "ordering.order.notFound"));
            return;
        }
        render(order);
    }

    private void render(Order order) {
        var actor = currentUser.get().orElse(null);
        // The spec's ruling on a baker: reads everything, changes only the
        // states their role allows. The lines decide what the customer is
        // charged, because updateLines reprices from the live catalogue, so
        // they are on screen for every role and writable for two of the three.
        // An invoiced order is a document somebody has been given: its lines and
        // its state stop being editable, and that is the only case where the
        // state field is read only. Talking about it stays open, because a
        // question about an order does not stop being asked once it is paid.
        var invoice = invoices.forOrder(order).orElse(null);
        boolean mayEditLines = actor != null && invoice == null
                && (actor.getRole() == Role.ADMIN || actor.getRole() == Role.BARISTA);

        // Two parts: a body that scrolls and a footer that does not. What a
        // person needs while reading the middle of a long order is the total
        // and the save button, and those are exactly what scrolls away.
        var body = new Div();
        body.addClassName("order-detail__body");

        var top = new Div(new Breadcrumbs(Breadcrumbs.Mode.ROUTER));
        top.addClassName("order-detail__top");
        body.add(top);

        var stateField = stateSelector(order, actor, invoice != null);
        var header = new Div(Translations.bindText(new H2(), "tracking.title", order.getReference()),
                stateField);
        header.addClassName("order-detail__header");
        body.add(header);

        // Five facts, each in its own box, flowing onto as many lines as the
        // panel needs. A sentence of comma separated facts reads as prose and
        // has to be parsed; boxes with an icon each are scanned. The note takes
        // a row of its own because it is the only one of variable length.
        var facts = new Div();
        facts.addClassName("order-detail__facts");
        facts.add(fact(VaadinIcon.USER, locale -> order.getCustomer().getFullName()));
        facts.add(fact(VaadinIcon.PHONE, locale -> order.getCustomer().getPhone().replace(" ", "")));
        facts.add(fact(VaadinIcon.MAP_MARKER, locale -> order.getPickupLocation().getName()));
        facts.add(fact(VaadinIcon.CLOCK, locale -> order.getPickupDate().format(DATE.withLocale(locale))
                + " " + order.getPickupTime().format(TIME)));
        if (order.getCustomerNote() != null && !order.getCustomerNote().isBlank()) {
            var note = fact(VaadinIcon.COMMENT, locale -> SafeHtml.text(order.getCustomerNote()));
            note.addClassName("order-detail__fact--note");
            facts.add(note);
        }
        body.add(facts);

        // One row, growing as somebody types rather than a box of empty lines.
        var internalNote = new TextArea();
        internalNote.addClassName("order-detail__note");
        Translations.bind(internalNote, internalNote::setLabel, "board.detail.internalNote");
        internalNote.setValue(order.getInternalNote() == null ? "" : order.getInternalNote());
        internalNote.setMaxLength(500);
        internalNote.setMinRows(1);
        internalNote.setMaxRows(6);
        internalNote.setWidthFull();
        internalNote.setReadOnly(invoice != null);
        // Staged, like the state and the lines. Saving it on every change bumped
        // the row's version under the panel holding it, and the next line save
        // failed with an optimistic locking error against nobody.
        internalNote.addValueChangeListener(changed -> {
            if (changed.isFromClient()) {
                unsaved.touched();
            }
        });
        body.add(internalNote);

        var editor = new OrderLineEditor(catalogue);
        editor.setLines(orders.detailLines(order.getReference()).stream()
                .map(line -> new CartLine(line.productId(), line.quantity(), line.comment()))
                .toList());
        editor.setEnabled(mayEditLines);
        body.add(editor);

        // One list, one chronology: what happened to the order and what was said
        // about it, under one heading. Opening it marks the messages read.
        // Never closed on the staff side, because an order that cannot be
        // changed can still be asked about, and the person asking is usually
        // asking because it cannot be changed.
        body.add(Translations.bindText(new H2(), "tracking.history"));
        body.add(new ConversationPanel(orders, activity, order.getReference(), true, actor,
                actor == null ? getTranslation("conversation.bakery") : actor.getFullName(),
                false, () -> historyEvents(order)));

        var footer = new Div();
        footer.addClassName("order-detail__footer");
        // Closing is a button beside saving rather than a cross in the corner:
        // the two things somebody does when they have finished with a panel
        // belong next to each other, and the cross was the only control on the
        // screen that had to be hunted for.
        var close = Translations.bindText(new Button("", event -> close()), "board.panel.close");
        footer.add(close);
        if (mayEditLines) {
            unsaved.follow(editor);
        }
        // One button for the whole panel: an invoiced order has nothing to save,
        // and a baker who may move the state but not the lines still needs it.
        if (invoice == null && (mayEditLines || !stateField.isReadOnly())) {
            var save = Translations.bindText(new Button("",
                    event -> save(order, editor, stateField, internalNote, actor, mayEditLines)), "board.editor.save");
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            footer.add(save);
        }
        // The editor's total, not the order's: while somebody is adding a line
        // or changing a quantity, the number they are watching is the one they
        // are about to save, and the saved one is the number they had before.
        var total = new Span();
        total.addClassName("order-detail__total");
        Translations.onLocale(total, locale ->
                total.setText(getTranslation(locale, "cart.gross", editor.total().format(locale))));
        editor.addLinesChangeListener(event ->
                total.setText(getTranslation("cart.gross", editor.total().format(getLocale()))));
        footer.add(total);

        add(body, footer);
    }

    /**
     * The panel's one save: the lines first, then the state.
     *
     * That order matters. `updateLines` compares the version the screen was
     * given against the row, which is how two baristas editing one order stop
     * being a silent last-writer-wins, and `changeState` reloads the row by id.
     * Saving the state first would bump the version and make the line save
     * fail against work nobody else had touched.
     */
    /** One fact: an icon and a value, in a box that flows with the others. */
    private Div fact(VaadinIcon icon, com.vaadin.flow.function.SerializableFunction<java.util.Locale, String> value) {
        var chip = new Div(new Icon(icon), Translations.bindText(new Span(), value));
        chip.addClassName("order-detail__fact");
        return chip;
    }

    private void save(Order order, OrderLineEditor editor, ComboBox<OrderState> stateField,
            TextArea internalNote, User actor, boolean mayEditLines) {
        try {
            if (mayEditLines) {
                orders.updateLines(order, editor.getLines(), actor);
            }
            var target = stateField.getValue();
            if (target != null && target != order.getState()) {
                var updated = orders.changeState(order, target,
                        "ordering.history." + target.name().toLowerCase(), actor);
                if (target == OrderState.PICKED_UP) {
                    invoices.issue(updated);
                }
            }
            // Last, and by reference: it loads the row itself, so it cannot
            // trip the version check the two above rely on.
            if (!internalNote.isReadOnly()) {
                orders.updateInternalNote(order.getReference(), internalNote.getValue());
            }
            unsaved.settled();
            OrderBoardView.refreshBehind(this);
            close();
        } catch (DomainException failure) {
            Notification.show(getTranslation(failure.translationKey(), failure.arguments()))
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (RuntimeException failure) {
            // The two reachable ones are an optimistic locking failure, because
            // the internal note listener bumps the same row, and whatever
            // Hibernate raises from mutating the items collection. Neither is a
            // sentence anybody can act on, but silence is worse: it reaches a
            // barista as an internal error screen with no clue that the save did
            // not happen.
            LOG.error("Saving the lines of order {} failed", order.getReference(), failure);
            Notification.show(getTranslation("board.editor.saveFailed"))
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * The state, as a field that can be changed rather than a row of buttons.
     * It offers the current state plus only the transitions this order can make
     * from this role, and it carries the state's own colour: the field is the
     * status light.
     *
     * It stages, it does not save. Committing on selection made three separate
     * problems: the panel re-rendered under the person using it, the option
     * list shrank to whatever the new state allows, and there was no way back
     * because the way back is rarely a legal transition. Staged, the original
     * value stays in the list until Save, so changing your mind is just
     * choosing it again.
     */
    private ComboBox<OrderState> stateSelector(Order order, User actor, boolean invoiced) {
        var combo = new ComboBox<OrderState>();
        combo.addClassName("order-detail__state");
        combo.getElement().setAttribute("data-state", order.getState().name());
        combo.setAllowCustomValue(false);
        // Small, and narrow enough to sit beside the reference rather than
        // under it: it is a status, not a form field somebody fills in.
        combo.addThemeVariants(com.vaadin.flow.component.combobox.ComboBoxVariant.LUMO_SMALL);
        combo.setWidth("9.5rem");

        var options = new java.util.LinkedHashSet<OrderState>();
        options.add(order.getState());
        for (OrderState target : order.getState().allowedTargets()) {
            boolean admin = actor != null && actor.getRole() == Role.ADMIN;
            if (target == OrderState.CANCELLED && !admin && order.getState() == OrderState.READY) {
                continue;
            }
            if (actor != null && !target.settableBy(actor.getRole())) {
                continue;
            }
            options.add(target);
        }
        combo.setItems(options);
        // Set now, not when the locale effect first runs: that happens on
        // attach, which is after setValue, and a combo box with no label
        // generator renders the enum's own name. "IN_PREPARATION" in the field
        // was exactly that.
        combo.setItemLabelGenerator(state -> getTranslation(state.translationKey()));
        Translations.onLocale(combo, locale -> {
            combo.setItemLabelGenerator(state -> getTranslation(locale, state.translationKey()));
            // Re-setting the generator does not redraw the value that is
            // already in the field.
            var held = combo.getValue();
            combo.setValue(null);
            combo.setValue(held);
        });
        combo.setValue(order.getState());
        // Read only for one reason only: the order has an invoice. Never
        // because it has run out of transitions.
        combo.setReadOnly(invoiced);

        combo.addValueChangeListener(event -> {
            if (!event.isFromClient()) {
                return;
            }
            var chosen = event.getValue() == null ? order.getState() : event.getValue();
            if (event.getValue() == null) {
                // A cleared field is not a state. Put the order's own back.
                combo.setValue(order.getState());
                return;
            }
            // The colour follows the selection rather than what is saved, so
            // the field shows what Save is about to do.
            combo.getElement().setAttribute("data-state", chosen.name());
            if (chosen != order.getState()) {
                unsaved.touched();
            }
        });
        return combo;
    }

    /**
     * What happened to the order, as events for the one list that also holds
     * what was said about it. Read model rather than the entity: the author is
     * a lazy proxy and the view runs with no session.
     */
    private java.util.List<ConversationPanel.Event> historyEvents(Order order) {
        return orders.historyLines(order.getReference()).stream()
                .map(entry -> new ConversationPanel.Event(entry.timestamp(),
                        entry.authorName() == null
                                ? getTranslation("board.detail.byCustomer")
                                : firstName(entry.authorName()),
                        text(entry, getLocale())))
                .toList();
    }

    /**
     * A first name is enough on a log where the same four people appear all
     * day, and it is what leaves room for what they actually did.
     */
    static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        var space = fullName.indexOf(' ');
        return space < 0 ? fullName : fullName.substring(0, space);
    }

    /**
     * What happened, and what it changed. The detail is stored as data,
     * "NEW>CANCELLED" or "2900>3400" in cents, so both halves are translated
     * and formatted here rather than frozen into the row when it was written.
     */
    private String text(com.vaadin.bakery.ordering.OrderHistoryLine entry, java.util.Locale locale) {
        var message = getTranslation(locale, entry.messageKey());
        if (entry.detail() == null || !entry.detail().contains(">")) {
            return message;
        }
        var halves = entry.detail().split(">", 2);
        var change = isNumeric(halves[0]) && isNumeric(halves[1])
                ? com.vaadin.bakery.base.Money.ofCents(Integer.parseInt(halves[0])).format(locale)
                        + " \u2192 " + com.vaadin.bakery.base.Money.ofCents(Integer.parseInt(halves[1])).format(locale)
                : getTranslation(locale, "ordering.state." + halves[0])
                        + " \u2192 " + getTranslation(locale, "ordering.state." + halves[1]);
        return message + ": " + change;
    }

    private static boolean isNumeric(String value) {
        return value.chars().allMatch(Character::isDigit) && !value.isEmpty();
    }

    /** Also the way the board closes the panel, so every gesture asks the same question. */
    @Override
    public void close() {
        unsaved.leave(this, () -> getUI().ifPresent(ui -> ui.navigate(OrderBoardView.ROUTE)));
    }

}
