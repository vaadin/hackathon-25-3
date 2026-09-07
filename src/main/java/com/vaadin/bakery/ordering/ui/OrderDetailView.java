package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.SafeHtml;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.base.security.CurrentUser;
import com.vaadin.bakery.billing.InvoiceService;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.people.User;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.button.Button;
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
import java.time.ZoneId;
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

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("d MMM HH:mm");

    private final OrderService orders;
    private final InvoiceService invoices;
    private final CurrentUser currentUser;
    private final CatalogueService catalogue;
    private final UnsavedChanges unsaved = new UnsavedChanges();

    public OrderDetailView(OrderService orders, InvoiceService invoices, CurrentUser currentUser,
            CatalogueService catalogue) {
        this.orders = orders;
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
        boolean mayEditLines = actor != null
                && (actor.getRole() == Role.ADMIN || actor.getRole() == Role.BARISTA);

        var close = new Button(new Icon(VaadinIcon.CLOSE_SMALL), event -> close());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        Translations.bind(close, close::setAriaLabel, "board.panel.close");
        add(close);

        add(new Breadcrumbs(Breadcrumbs.Mode.ROUTER));
        add(Translations.bindText(new H2(), "tracking.title", order.getReference()));

        var state = Translations.bindText(new Span(), order.getState().translationKey());
        state.getElement().getThemeList().add("badge");
        add(state);

        add(Translations.bindText(new Paragraph(), "board.detail.customer",
                order.getCustomer().getFullName(), order.getCustomer().getPhone()));
        add(Translations.bindText(new Paragraph(), locale -> getTranslation(locale, "tracking.slot",
                order.getPickupLocation().getName(),
                order.getPickupDate().format(DATE.withLocale(locale)),
                order.getPickupTime().format(TIME))));

        var editor = new OrderLineEditor(catalogue);
        editor.setLines(orders.detailLines(order.getReference()).stream()
                .map(line -> new CartLine(line.productId(), line.quantity(), line.comment()))
                .toList());

        editor.setEnabled(mayEditLines);
        add(editor);

        if (mayEditLines) {
            unsaved.follow(editor);
            var save = Translations.bindText(new Button("", event -> {
                try {
                    orders.updateLines(order, editor.getLines(), actor);
                    unsaved.settled();
                    OrderBoardView.refreshBehind(this);
                    close();
                } catch (DomainException failure) {
                    Notification.show(getTranslation(failure.translationKey(), failure.arguments()))
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                } catch (RuntimeException failure) {
                    // The two reachable ones are an optimistic locking failure,
                    // because the internal note listener bumps the same row, and
                    // whatever Hibernate raises from mutating the items
                    // collection. Neither is a sentence anybody can act on, but
                    // silence is worse: it reaches a barista as an internal
                    // error screen with no clue that the save did not happen.
                    LOG.error("Saving the lines of order {} failed", order.getReference(), failure);
                    Notification.show(getTranslation("board.editor.saveFailed"))
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }), "board.editor.save");
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            add(save);
        }

        if (order.getCustomerNote() != null && !order.getCustomerNote().isBlank()) {
            add(Translations.bindText(new Paragraph(), "board.detail.customerNote",
                    SafeHtml.text(order.getCustomerNote())));
        }

        var internalNote = new TextArea();
        Translations.bind(internalNote, internalNote::setLabel, "board.detail.internalNote");
        internalNote.setValue(order.getInternalNote() == null ? "" : order.getInternalNote());
        internalNote.setMaxLength(500);
        internalNote.addValueChangeListener(changed -> {
            order.setInternalNote(changed.getValue());
            orders.save(order);
        });
        add(internalNote);

        add(Translations.bindText(new H2(), "tracking.history"), timeline(order), actions(order, actor));

        // The staff side of the same conversation. Opening it marks it read.
        add(new ConversationPanel(orders, order.getReference(), true, actor,
                actor == null ? getTranslation("conversation.bakery") : actor.getFullName(),
                !order.getState().isOpen()));
    }

    private Div timeline(Order order) {
        var timeline = new Div();
        timeline.addClassName("order-detail__timeline");
        // Read model again: the author is a lazy proxy on the entity, and the
        // view runs with no session.
        orders.historyLines(order.getReference()).forEach(entry -> {
            var when = Translations.bindText(new Span(), locale ->
                    entry.timestamp().atZone(ZoneId.systemDefault()).format(STAMP.withLocale(locale)));
            var who = entry.authorName() == null
                    ? Translations.bindText(new Span(), "board.detail.byCustomer")
                    : new Span(entry.authorName());
            timeline.add(new Div(when, Translations.bindText(new Span(), entry.messageKey()), who));
        });
        return timeline;
    }

    /** Only the transitions this order can actually make, from this role. */
    private Div actions(Order order, User actor) {
        var actions = new Div();
        actions.addClassName("order-detail__actions");
        boolean admin = actor != null && actor.getRole() == Role.ADMIN;

        for (OrderState target : order.getState().allowedTargets()) {
            if (target == OrderState.CANCELLED && !admin && order.getState() == OrderState.READY) {
                continue;
            }
            if (actor != null && !target.settableBy(actor.getRole())) {
                continue;
            }
            var button = Translations.bindText(new Button("", event -> {
                try {
                    var updated = orders.changeState(order, target, "ordering.history." + target.name().toLowerCase(),
                            actor);
                    if (target == OrderState.PICKED_UP) {
                        invoices.issue(updated);
                    }
                    // Not Page.reload(): the board is a persistent layout now,
                    // and a browser reload throws away the list, its scroll
                    // position, the selection a bulk action was being built from
                    // and every expanded row. Re-rendering the panel over a
                    // refreshed board is the same answer without the cost.
                    OrderBoardView.refreshBehind(this);
                    removeAll();
                    unsaved.settled();
                    render(orders.byReference(order.getReference()).orElseThrow());
                } catch (DomainException failure) {
                    var notification = Notification.show(
                            getTranslation(failure.translationKey(), failure.arguments()));
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }), "board.action." + target.name());
            if (target == OrderState.CANCELLED) {
                button.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            }
            actions.add(button);
        }
        return actions;
    }

    /** Also the way the board closes the panel, so every gesture asks the same question. */
    @Override
    public void close() {
        unsaved.leave(this, () -> getUI().ifPresent(ui -> ui.navigate(OrderBoardView.ROUTE)));
    }

}
