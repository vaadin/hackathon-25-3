package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.ordering.PickupClosure;
import com.vaadin.bakery.ordering.PickupClosureRepository;
import com.vaadin.bakery.ordering.PickupLocation;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.bakery.people.Role;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudI18n;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Closures. Adding one here is what makes a day disappear from the public date
 * picker, so this is also the fastest way to demonstrate the calendar.
 *
 * A list and an editor, so {@code Crud} owns the shape: the new item button,
 * the editor panel and the delete confirmation were all written by hand here
 * before. What is not Crud's is the rule that matters: a closure that would
 * strand existing orders warns and lists them, and it does not cancel anything,
 * because deciding what to do with those orders is a phone call and not a
 * database update.
 */
@Route("admin/closures")
@PageTitle("Closures")
@Menu(order = 22, title = "Closures", icon = "vaadin:calendar-clock")
@RolesAllowed(Role.ADMIN_NAME)
public class ClosureAdminView extends VerticalLayout {

    private final PickupClosureRepository closures;
    private final PickupLocationRepository locations;
    private final OrderRepository orders;
    private final Clock clock;
    private final Grid<PickupClosure> grid = new Grid<>(PickupClosure.class, false);
    private final Crud<PickupClosure> crud;

    public ClosureAdminView(PickupClosureRepository closures, PickupLocationRepository locations,
            OrderRepository orders, Clock clock) {
        this.closures = closures;
        this.locations = locations;
        this.orders = orders;
        this.clock = clock;
        addClassName("closure-admin");
        setSizeFull();

        // A column is not in the component tree, so the grid owns the binding.
        var date = grid.addColumn(PickupClosure::getDate).setSortable(true);
        Translations.bind(grid, date::setHeader, "admin.closure.date");
        var where = grid.addColumn(closure -> closure.getLocation() == null
                ? getTranslation("admin.closure.everywhere")
                : closure.getLocation().getName()).setSortable(true);
        Translations.bind(grid, where::setHeader, "ordering.slot.location");
        var reason = grid.addColumn(PickupClosure::getReason).setSortable(true).setFlexGrow(2);
        Translations.bind(grid, reason::setHeader, "admin.closure.reason");
        var kind = grid.addColumn(closure -> getTranslation(closure.getKind().translationKey()))
                .setSortable(true);
        Translations.bind(grid, kind::setHeader, "admin.closure.kind");

        // A Crud given its own grid does not add the edit column.
        Crud.addEditColumn(grid);

        crud = new Crud<>(PickupClosure.class, grid, editor());
        crud.setSizeFull();
        Translations.onLocale(this, locale -> crud.setI18n(texts(locale)));
        crud.addSaveListener(event -> saveUnlessItStrandsOrders(event.getItem()));
        crud.addDeleteListener(event -> {
            closures.delete(event.getItem());
            refresh();
        });

        add(Translations.bindText(new H2(), "admin.closure.title"), crud);
        // Two cells are translated by their value provider, and only a reload
        // makes the grid ask for them again.
        Translations.onLocale(this, locale -> refresh());
    }

    private BinderCrudEditor<PickupClosure> editor() {
        var binder = new BeanValidationBinder<>(PickupClosure.class);

        var date = new DatePicker();
        Translations.bind(date, date::setLabel, "admin.closure.date");
        date.setMin(LocalDate.now(clock));

        var location = new ComboBox<PickupLocation>();
        Translations.bind(location, location::setLabel, "ordering.slot.location");
        location.setItems(locations.findAll());
        location.setItemLabelGenerator(PickupLocation::getName);
        // Empty means everywhere, which is why it clears rather than requires.
        Translations.bind(location, location::setPlaceholder, "admin.closure.everywhere");
        location.setClearButtonVisible(true);

        var reason = new TextField();
        Translations.bind(reason, reason::setLabel, "admin.closure.reason");

        var kind = new ComboBox<PickupClosure.ClosureKind>();
        Translations.bind(kind, kind::setLabel, "admin.closure.kind");
        kind.setItems(PickupClosure.ClosureKind.values());
        Translations.onLocale(kind, locale ->
                kind.setItemLabelGenerator(value -> getTranslation(locale, value.translationKey())));

        binder.bind(date, "date");
        binder.bind(location, "location");
        binder.bind(reason, "reason");
        binder.bind(kind, "kind");

        return new BinderCrudEditor<>(binder, new FormLayout(date, location, reason, kind));
    }

    private CrudI18n texts(java.util.Locale locale) {
        var i18n = CrudI18n.createDefault();
        i18n.setNewItem(getTranslation(locale, "admin.closure.add"));
        i18n.setEditItem(getTranslation(locale, "admin.closure.edit"));
        i18n.setSaveItem(getTranslation(locale, "admin.save"));
        i18n.setDeleteItem(getTranslation(locale, "admin.delete"));
        i18n.setCancel(getTranslation(locale, "admin.cancel"));
        i18n.setEditLabel(getTranslation(locale, "admin.edit"));
        i18n.getConfirm().getDelete().setTitle(getTranslation(locale, "admin.closure.delete.title"));
        i18n.getConfirm().getDelete().setContent(getTranslation(locale, "admin.closure.delete.body"));
        i18n.getConfirm().getDelete().getButton().setConfirm(getTranslation(locale, "admin.delete"));
        i18n.getConfirm().getDelete().getButton().setDismiss(getTranslation(locale, "admin.cancel"));
        return i18n;
    }

    /**
     * The one rule Crud does not know about. Saving is not refused, it is
     * asked about: the administrator is shown which orders fall on that day and
     * decides, because the alternative is either cancelling somebody's cake
     * without telling them or refusing a closure that genuinely has to happen.
     */
    void saveUnlessItStrandsOrders(PickupClosure closure) {
        var affected = affectedOrders(closure.getDate(), closure.getLocation());
        if (affected.isEmpty()) {
            save(closure);
            return;
        }
        var dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("admin.closure.affected.title", affected.size()));
        dialog.setText(getTranslation("admin.closure.affected.body", String.join(", ", affected)));
        dialog.setCancelable(true);
        dialog.setConfirmText(getTranslation("admin.closure.add"));
        dialog.addConfirmListener(event -> save(closure));
        // Refused: the row never reaches the database, so the grid is put back
        // to what the database still says.
        dialog.addCancelListener(event -> refresh());
        dialog.open();
    }

    private List<String> affectedOrders(LocalDate date, PickupLocation location) {
        if (date == null) {
            return List.of();
        }
        return orders.findByPickupDateAndStateInOrderByPickupTimeAsc(date,
                        List.of(OrderState.NEW, OrderState.CONFIRMED, OrderState.IN_PREPARATION, OrderState.READY))
                .stream()
                .filter(order -> location == null || order.getPickupLocation().equals(location))
                .map(order -> order.getReference())
                .toList();
    }

    private void save(PickupClosure closure) {
        // Every closure made here is a whole day. Half days exist in the model
        // for the dataset, and nothing in this screen creates one.
        closure.setWholeDay(true);
        if (closure.getKind() == null) {
            closure.setKind(PickupClosure.ClosureKind.MAINTENANCE);
        }
        closures.save(closure);
        refresh();
    }

    private void refresh() {
        grid.setItems(closures.findAll().stream()
                .sorted((left, right) -> left.getDate().compareTo(right.getDate()))
                .toList());
    }

    Grid<PickupClosure> grid() {
        return grid;
    }

    Crud<PickupClosure> crud() {
        return crud;
    }
}
