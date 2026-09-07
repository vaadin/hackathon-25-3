package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.base.security.CurrentUser;
import com.vaadin.bakery.base.ui.ColumnChooser;
import com.vaadin.bakery.base.ui.MainLayout;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderQueryCounter;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.OrderSpecifications;
import com.vaadin.bakery.ordering.OrderState;
import com.vaadin.bakery.ordering.SlotService;
import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridI18n;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import jakarta.annotation.security.RolesAllowed;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The staff board. This is the view that replaces the old storefront: no Lit
 * template, no page observer computing group headers, no renderer written in
 * JavaScript.
 *
 * Three 25.3 details earn their place here. Hidden columns cost nothing, and
 * the expensive summary column proves it. Row details are independent of
 * selection, so a baker can read one order while another is selected. GridI18n
 * gives the selection checkboxes and the sorters real names.
 */
@ParentLayout(MainLayout.class)
@Route(value = OrderBoardView.ROUTE, layout = MainLayout.class)
@PageTitle("Orders")
@Menu(order = 10, title = "Orders", icon = "vaadin:clipboard-text")
@RolesAllowed({ Role.ADMIN_NAME, Role.BAKER_NAME, Role.BARISTA_NAME })
public class OrderBoardView extends MasterDetailLayout {

    /** The one address of the board, and the prefix of everything it hosts. */
    public static final String ROUTE = "orders";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final OrderRepository orders;
    private final OrderService orderService;
    private final OrderQueryCounter counter;
    private final CurrentUser currentUser;
    private final SlotService slots;

    private final Grid<Order> grid = new Grid<>();
    private final ValueSignal<String> search = new ValueSignal<>("");
    private final ValueSignal<Boolean> includePast = new ValueSignal<>(false);
    private final Grid.Column<Order> referenceColumn;
    private final Grid.Column<Order> customerColumn;
    private final Grid.Column<Order> slotColumn;
    private final Grid.Column<Order> stateColumn;
    private final Grid.Column<Order> summaryColumn;
    private final Grid.Column<Order> channelColumn;
    private final Grid.Column<Order> totalColumn;

    public OrderBoardView(OrderRepository orders, OrderService orderService, OrderQueryCounter counter,
            CurrentUser currentUser, SlotService slots) {
        this.orders = orders;
        this.orderService = orderService;
        this.counter = counter;
        this.currentUser = currentUser;
        this.slots = slots;
        addClassName("order-board");

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.setSizeFull();

        // Accessible names for the checkboxes and the sorters. The grid takes
        // one i18n object rather than a setter per string, so following the
        // locale means handing it a new one.
        Translations.onLocale(grid, locale -> {
            var i18n = new GridI18n();
            i18n.setSelectAll(getTranslation(locale, "board.i18n.selectAll"));
            i18n.setSelectRow(getTranslation(locale, "board.i18n.selectRow"));
            i18n.setSorter(getTranslation(locale, "board.i18n.sorter"));
            grid.setI18n(i18n);
        });

        // A column is not in the component tree, so the grid owns the binding.
        referenceColumn = grid.addColumn(Order::getReference)
                .setKey("reference").setSortProperty("reference").setAutoWidth(true);
        Translations.bind(grid, referenceColumn::setHeader, "board.column.reference");
        customerColumn = grid.addColumn(order -> order.getCustomer().getFullName())
                .setKey("customer").setFlexGrow(2);
        Translations.bind(grid, customerColumn::setHeader, "board.column.customer");
        slotColumn = grid.addColumn(order -> order.getPickupDate().format(DAY.withLocale(getLocale()))
                        + " " + order.getPickupTime().format(TIME))
                .setKey("slot").setSortProperty("pickupDate").setAutoWidth(true);
        Translations.bind(grid, slotColumn::setHeader, "board.column.slot");
        stateColumn = grid.addComponentColumn(this::stateBadge)
                .setKey("state").setSortProperty("state").setAutoWidth(true);
        Translations.bind(grid, stateColumn::setHeader, "board.column.state");

        // Deliberately expensive: it walks the lazy item collection. Hidden by
        // default, and the counter next to it is what the test reads.
        summaryColumn = grid.addColumn(this::itemsSummary).setKey("items").setFlexGrow(3);
        Translations.bind(grid, summaryColumn::setHeader, "board.column.items");
        summaryColumn.setVisible(false);

        channelColumn = grid.addColumn(order -> getTranslation(order.getChannel().translationKey()))
                .setKey("channel").setAutoWidth(true);
        Translations.bind(grid, channelColumn::setHeader, "board.column.channel");
        channelColumn.setVisible(false);

        totalColumn = grid.addColumn(order -> order.gross().format(getLocale())).setKey("total").setAutoWidth(true);
        Translations.bind(grid, totalColumn::setHeader, "board.column.total");
        totalColumn.setVisible(false);

        // Details are independent of selection in 25.3, so expanding a row to
        // read its comments does not change what the bulk actions will act on.
        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::details));
        grid.setDetailsVisibleOnClick(false);

        grid.addItemClickListener(event -> grid.setDetailsVisible(event.getItem(),
                !grid.isDetailsVisible(event.getItem())));

        grid.addItemDoubleClickListener(event -> getUI().ifPresent(ui ->
                ui.navigate(ROUTE + "/" + event.getItem().getReference())));

        setSizeFull();
        var master = new Div(toolbar(), columnChooser(), grid);
        master.addClassName("order-board__master");
        setMaster(master);
        setDetailSize("32rem");
        setOverlayContainment(MasterDetailLayout.OverlayContainment.LAYOUT);

        // Escape and a click outside are the two ways everybody already knows.
        addBackdropClickListener(event -> closePanel());
        addDetailEscapePressListener(event -> closePanel());

        // The effect reads the signals, so it tracks them. Everything else calls
        // applyFilter with values it already has: a method shared between an
        // effect and a click listener cannot use get() in both.
        // Three cells are translated or formatted by their value provider, and
        // only a reload makes the grid ask for them again.
        Translations.onLocale(this, locale -> applyFilter(search.get(), includePast.get()));
    }

    /**
     * Escape and the backdrop go through whatever is in the panel rather than
     * navigating over its head, so a panel holding unsaved work gets to ask
     * before it is thrown away.
     */
    private void closePanel() {
        getUI().ifPresent(ui -> ui.getInternals().getActiveRouterTargetsChain().stream()
                .filter(BoardPanel.class::isInstance)
                .map(BoardPanel.class::cast)
                .findFirst()
                .ifPresentOrElse(BoardPanel::close, () -> ui.navigate(ROUTE)));
    }

    private Div toolbar() {
        var field = new TextField();
        Translations.bind(field, field::setPlaceholder, "board.search.placeholder");
        field.setClearButtonVisible(true);
        field.setValueChangeMode(ValueChangeMode.LAZY);
        field.addValueChangeListener(event -> search.set(event.getValue()));

        var past = new Checkbox();
        Translations.bind(past, past::setLabel, "board.showPast");
        past.addValueChangeListener(event -> includePast.set(event.getValue()));

        var confirm = Translations.bindText(new Button("",
                event -> bulk(OrderState.CONFIRMED, "ordering.history.confirmed")), "board.bulk.confirm");
        var cancel = Translations.bindText(new Button("",
                event -> bulk(OrderState.CANCELLED, "ordering.history.cancelled")), "board.bulk.cancel");
        cancel.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        var take = Translations.bindText(new Button("",
                event -> getUI().ifPresent(ui -> ui.navigate(ROUTE + "/" + NewOrderView.SEGMENT))), "board.new");
        take.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        // NewOrderView is closed to a baker, so the board does not offer them
        // a button whose only outcome is being refused.
        take.setVisible(currentUser.get().map(user -> user.getRole() != Role.BAKER).orElse(false));

        var toolbar = new Div(take, field, past, confirm, cancel);
        toolbar.addClassName("order-board__toolbar");
        return toolbar;
    }

    private Div columnChooser() {
        var menu = new MenuBar();
        ColumnChooser.of(menu, "board.columns", List.of(
                new ColumnChooser.Entry<>(referenceColumn, "board.column.reference"),
                new ColumnChooser.Entry<>(customerColumn, "board.column.customer"),
                new ColumnChooser.Entry<>(slotColumn, "board.column.slot"),
                new ColumnChooser.Entry<>(stateColumn, "board.column.state"),
                new ColumnChooser.Entry<>(summaryColumn, "board.column.items"),
                new ColumnChooser.Entry<>(channelColumn, "board.column.channel"),
                new ColumnChooser.Entry<>(totalColumn, "board.column.total")));
        var chooser = new Div(menu);
        chooser.addClassName("order-board__columns");
        return chooser;
    }

    private Span stateBadge(Order order) {
        var badge = Translations.bindText(new Span(), order.getState().translationKey());
        badge.getElement().getThemeList().add("badge " + switch (order.getState()) {
            case READY, PICKED_UP -> "success";
            case PROBLEM, CANCELLED -> "error";
            default -> "contrast";
        });
        return badge;
    }

    /** The expensive one. Every call is counted. */
    private String itemsSummary(Order order) {
        counter.countExpensiveColumn();
        return orderService.itemsSummary(order.getReference());
    }

    private Div details(Order order) {
        return new OrderDetailsBand(orderService, order.getReference());
    }

    private void bulk(OrderState target, String message) {
        var selected = new ArrayList<>(grid.getSelectedItems());
        if (selected.isEmpty()) {
            Notification.show(getTranslation("board.bulk.nothingSelected"));
            return;
        }
        var actor = currentUser.get().orElse(null);
        int done = 0;
        var refused = new ArrayList<String>();
        for (Order order : selected) {
            try {
                orderService.changeState(order, target, message, actor);
                done++;
            } catch (DomainException failure) {
                refused.add(order.getReference());
            }
        }
        grid.deselectAll();
        reload();
        // Say how many worked and name the ones that did not, with the reason.
        Notification.show(refused.isEmpty()
                ? getTranslation("board.bulk.done", done)
                : getTranslation("board.bulk.partial", done, String.join(", ", refused)));
    }

    void reload() {
        applyFilter(search.peek(), includePast.peek());
    }

    /**
     * Tells the board behind a panel that what it lists has changed. Flow
     * reuses the layout instance, so a panel that saves and navigates back
     * lands on the very same board, still holding the data provider it was
     * given: nothing re-invokes setItemsPageable unless something asks.
     *
     * The board is reached the way the tests reach it, as the panel's layout
     * and therefore one of the active router targets, rather than as something
     * a panel could be handed at construction time.
     */
    static void refreshBehind(Component panel) {
        panel.getUI().ifPresent(ui -> ui.getInternals().getActiveRouterTargetsChain().stream()
                .filter(OrderBoardView.class::isInstance)
                .map(OrderBoardView.class::cast)
                .findFirst()
                .ifPresent(OrderBoardView::reload));
    }

    private void applyFilter(String term, boolean withPast) {
        // A search is a search: when somebody types a reference, they mean any
        // order, not only the ones still to come.
        var searching = term != null && !term.isBlank();
        var from = withPast || searching ? null : slots.today().minusDays(1);
        // Arrays.asList, not List.of: an inactive filter is null, and List.of
        // rejects nulls with an NPE that surfaces only on stderr.
        var specification = OrderSpecifications.all(java.util.Arrays.asList(
                OrderSpecifications.matching(term),
                OrderSpecifications.fromDate(from)));

        // Lazy, page by page, with the sort the board is read in. The counter
        // makes the cost of a page visible to the diagnostics view and to the
        // test that proves hidden columns are free.
        grid.setItemsPageable(pageable -> {
            counter.countQuery();
            var sorted = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("pickupDate").ascending()
                            .and(org.springframework.data.domain.Sort.by("pickupTime").ascending()));
            return specification == null ? orders.findAll(sorted).getContent()
                    : orders.findAll(specification, sorted).getContent();
        }, pageable -> Math.toIntExact(specification == null ? orders.count() : orders.count(specification)));
    }

    /** Used by the tests that prove a hidden column costs nothing. */
    Grid.Column<Order> expensiveColumn() {
        return summaryColumn;
    }

    Grid<Order> grid() {
        return grid;
    }
}
