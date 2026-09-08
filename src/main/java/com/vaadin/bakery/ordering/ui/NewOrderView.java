package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.ui.Fields;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.base.security.CurrentUser;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.ordering.Channel;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.bakery.ordering.SlotService;
import com.vaadin.bakery.people.Role;
import com.vaadin.flow.component.InputMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * An order taken at the counter, in the panel where every other order is read.
 *
 * There is no customer session cart here, so the slot picker cannot be handed
 * one: it is handed a lead time computed from whatever the editor actually
 * holds right now, and told to recompute it whenever the lines change.
 *
 * Not open to a baker: a baker changes only the states their role allows, and
 * taking an order decides both what was ordered and what it costs.
 */
@Route(value = OrderBoardView.ROUTE + "/" + NewOrderView.SEGMENT, layout = OrderBoardView.class)
@PageTitle("New order")
@RolesAllowed({ Role.ADMIN_NAME, Role.BARISTA_NAME })
public class NewOrderView extends VerticalLayout implements BoardPanel {

    /** What the board's toolbar navigates to, so the two cannot drift apart. */
    public static final String SEGMENT = "new-counter";

    private final UnsavedChanges unsaved = new UnsavedChanges();

    public NewOrderView(OrderService orders, CatalogueService catalogue, PickupLocationRepository locations,
            SlotService slots, CurrentUser currentUser) {
        addClassName("order-detail");

        // The same control in the same corner as the order panel: two panels in
        // one slot should not be dismissed two different ways.
        var close = new Button(new Icon(VaadinIcon.CLOSE_SMALL), event -> close());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        Translations.bind(close, close::setAriaLabel, "board.panel.close");
        add(close);

        add(Translations.bindText(new H2(), "board.new"));

        var firstName = new TextField();
        Translations.bind(firstName, firstName::setLabel, "checkout.firstName");
        var lastName = new TextField();
        Translations.bind(lastName, lastName::setLabel, "checkout.lastName");
        var email = Fields.email("checkout.email");
        var phone = new TextField();
        Translations.bind(phone, phone::setLabel, "checkout.phone");
        phone.setInputMode(InputMode.TEL);
        add(new FormLayout(firstName, lastName, email, phone));

        var editor = new OrderLineEditor(catalogue);
        add(editor);

        var picker = new SlotPicker(slots, () -> leadTimeDays(editor, catalogue),
                locations.findByActiveTrueOrderByNameAsc());
        add(picker);
        // The lead time depends on what has actually been ordered, so every
        // change to the lines has to be followed by a recomputed calendar.
        editor.addLinesChangeListener(event -> picker.refresh());
        unsaved.follow(editor);

        var save = Translations.bindText(new Button("", event -> {
            var refusal = refuse(editor, email, picker);
            if (refusal != null) {
                Notification.show(getTranslation(refusal)).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                orders.place(editor.getLines(), firstName.getValue(), lastName.getValue(), email.getValue(),
                        phone.getValue(), picker.getLocation(), picker.getDate(), picker.getTime(),
                        Channel.COUNTER, null, currentUser.get().orElse(null));
                unsaved.settled();
                // The board behind this panel is the same instance that will be
                // shown again, so without this the order just taken is not in
                // the list it was taken from.
                OrderBoardView.refreshBehind(this);
                getUI().ifPresent(ui -> ui.navigate(OrderBoardView.ROUTE));
            } catch (DomainException failure) {
                Notification.show(getTranslation(failure.translationKey(), failure.arguments()))
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }), "board.editor.save");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(save);
    }

    /** Escape, the backdrop and the close control, all through the same question. */
    @Override
    public void close() {
        unsaved.leave(this, () -> getUI().ifPresent(ui -> ui.navigate(OrderBoardView.ROUTE)));
    }

    /** The slowest product actually on the order right now decides the earliest pickup day. */
    static int leadTimeDays(OrderLineEditor editor, CatalogueService catalogue) {
        return editor.getLines().stream()
                .mapToInt(line -> catalogue.require(line.productId()).getLeadTimeDays())
                .max()
                .orElse(0);
    }

    /**
     * What stops this save, as a translation key, or {@code null} when nothing
     * does. This only catches what the view itself can know before asking:
     * whether there is anything to order, a way to reach the customer, and a
     * chosen slot. Everything {@code OrderService.place} can still refuse
     * (a product not baked that day, a lead time violation, a full slot) rolls
     * back cleanly on its own, because resolving the customer and placing the
     * order now happen in the one transaction that method runs.
     */
    static String refuse(OrderLineEditor editor, EmailField email, SlotPicker picker) {
        if (editor.getLines().isEmpty()) {
            return "board.editor.linesRequired";
        }
        if (email.getValue() == null || email.getValue().isBlank()) {
            return "board.editor.emailRequired";
        }
        if (picker.getDate() == null || picker.getTime() == null) {
            return "ordering.slot.notChosen";
        }
        return null;
    }
}
