package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.base.i18n.Translations;
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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridI18n;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

    /** The chooser in the table's own header, so a test can reach the toggles. */
    private com.vaadin.flow.component.grid.contextmenu.GridContextMenu<Order> columnChooser;
    private final ValueSignal<String> search = new ValueSignal<>("");
    private final ValueSignal<Boolean> includePast = new ValueSignal<>(false);
    private final Grid.Column<Order> editColumn;
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
        var selection = (GridMultiSelectionModel<Order>) grid.setSelectionMode(Grid.SelectionMode.MULTI);
        // Visible, and that is not a preference either: HIDDEN is not
        // honoured. The board ran with HIDDEN for a while and the checkbox was
        // in the header the whole time, 26 by 26 pixels, ticking when clicked
        // and selecting nothing. Measured on this screen and reduced in
        // specs/issues/20-grid-select-all-lazy.md.
        //
        // VISIBLE is what makes the control tell the truth: it works on a lazy
        // grid, including through setItemsPageable, and selects every row the
        // count callback reports. Which is also what it costs, and the reason
        // this line deserves a second thought before it is copied: select all
        // here means every order the current filter matches, fetched into the
        // session in one go.
        selection.setSelectAllCheckboxVisibility(
                GridMultiSelectionModel.SelectAllCheckboxVisibility.VISIBLE);
        grid.setSizeFull();

        // Accessible names for the checkboxes and the sorters. The grid takes
        // one i18n object rather than a setter per string, so following the
        // locale means handing it a new one.
        Translations.onLocale(grid, locale -> {
            var i18n = new GridI18n();
            i18n.setSelectAll(getTranslation(locale, "board.i18n.selectAll"));
            // Translated, and never shown while select all is offered: the
            // grid writes this sentence into the header instead of the
            // checkbox, and there is a checkbox. It was empty for a while, for
            // a reason worth remembering: the sentence lives in a span marked
            // sr-only, a declared theme makes that span one pixel square, and
            // a theme added with addStyleSheet never reaches a component's
            // shadow root, so the span came out 140 pixels wide and took the
            // column with it. That is specs/issues/01-runtime-theme-shadow-dom.md
            // and it is still true. Emptying the string is no longer the price
            // of a narrow column, so the string is a string again.
            i18n.setSelectAllUnavailable(getTranslation(locale, "board.i18n.selectAllUnavailable"));
            i18n.setSelectRow(getTranslation(locale, "board.i18n.selectRow"));
            i18n.setSorter(getTranslation(locale, "board.i18n.sorter"));
            grid.setI18n(i18n);
        });

        // A column is not in the component tree, so the grid owns the binding.
        // The panel opens from a column of its own rather than from a double
        // click: a double click is invisible, and on a board where a single
        // click expands the row it is also a gesture nobody discovers. The
        // column is as narrow as an icon and frozen, so it stays reachable when
        // the columns to its right are scrolled.
        editColumn = grid.addComponentColumn(this::editButton).setKey("edit")
                .setWidth("2.75rem").setFlexGrow(0).setFrozen(true)
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);

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

        // The columns menu opens from the table's own header, over any header
        // cell including the select all one, which is the nearest thing the API
        // has to a control in that cell. GridSelectionColumn extends Component
        // rather than AbstractColumn, so it has no header API, and neither the
        // default header row nor a prepended one has a cell for it: measured,
        // and written up in the select all section of FEEDBACK-25.3.md.
        //
        // Built here, after every column it lists exists, because it reads
        // their visibility.
        columnChooser();

        // Details are independent of selection in 25.3, so expanding a row to
        // read its comments does not change what the bulk actions will act on.
        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::details));
        grid.setDetailsVisibleOnClick(false);

        // One row expanded at a time. Several open bands turn the board into a
        // wall of tiles with the queue lost between them, and the band is a
        // glance at one order rather than a comparison of many.
        grid.addItemClickListener(event -> {
            var wasOpen = grid.isDetailsVisible(event.getItem());
            collapseExpanded();
            if (!wasOpen) {
                expanded = event.getItem();
                grid.setDetailsVisible(expanded, true);
            }
        });

        // Escape collapses the band. The layout already gives Escape to the
        // panel when one is open, and that one asks about unsaved work, so this
        // only acts when there is nothing open over the board.
        com.vaadin.flow.component.Shortcuts.addShortcutListener(this, () -> {
            if (!isPanelOpen()) {
                collapseExpanded();
            }
        }, com.vaadin.flow.component.Key.ESCAPE);

        setSizeFull();
        var master = new Div(toolbar(), grid);
        master.addClassName("order-board__master");
        setMaster(master);
        // A phone's width, fixed, so the panel looks the same on every screen
        // and the editor inside it has one shape to be right in rather than a
        // range. The master takes the slack: with neither side expanding, the
        // layout leaves the difference belonging to nobody, which on a 1428px
        // window was 180 pixels of blank panel to the right of the scrollbar.
        setDetailSize(DEFAULT_DETAIL_SIZE);
        setExpandMaster(true);
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

    /** The panel width the board hands out unless a panel asks for more. */
    private static final String DEFAULT_DETAIL_SIZE = "26rem";

    /**
     * Widens the panel area for a panel that needs it, and
     * {@link #resetPanelWidth} puts it back. The board owns the number so that
     * a panel which widens and then closes cannot leave every later panel wide.
     */
    public static void widenPanel(Component panel, String size) {
        board(panel).ifPresent(board -> board.setDetailSize(size));
    }

    public static void resetPanelWidth(Component panel) {
        board(panel).ifPresent(board -> board.setDetailSize(DEFAULT_DETAIL_SIZE));
    }

    private static java.util.Optional<OrderBoardView> board(Component panel) {
        return panel.getUI().flatMap(ui -> ui.getInternals().getActiveRouterTargetsChain().stream()
                .filter(OrderBoardView.class::isInstance)
                .map(OrderBoardView.class::cast)
                .findFirst());
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

    /**
     * Two groups, because the buttons do two different things. Everything on
     * the left acts on the board: start an order, ask a question, narrow the
     * list. The two on the right act on whatever is ticked, and they are dark
     * until something is, which is the only honest way to say what they need.
     */
    /** The one row whose details band is open, so opening another closes it. */
    private Order expanded;

    private Div toolbar() {
        var field = new TextField();
        Translations.bind(field, field::setPlaceholder, "board.search.placeholder");
        field.setClearButtonVisible(true);
        field.setValueChangeMode(ValueChangeMode.LAZY);
        field.addValueChangeListener(event -> search.set(event.getValue()));

        var past = new Checkbox();
        Translations.bind(past, past::setLabel, "board.showPast");
        past.addValueChangeListener(event -> includePast.set(event.getValue()));

        var take = iconAction(VaadinIcon.PLUS, "board.new",
                () -> getUI().ifPresent(ui -> ui.navigate(ROUTE + "/" + NewOrderView.SEGMENT)));
        // NewOrderView is closed to a baker, so the board does not offer them
        // a button whose only outcome is being refused.
        take.setVisible(currentUser.get().map(user -> user.getRole() != Role.BAKER).orElse(false));

        // Open to a baker too: asking a question reads, it does not change an
        // order, and "which of these is at risk" is a kitchen question.
        var question = iconAction(VaadinIcon.MAGIC, "board.ask",
                () -> getUI().ifPresent(ui -> ui.navigate(
                        ROUTE + "/" + com.vaadin.bakery.assistant.ui.BoardAskView.SEGMENT)));

        // The two actions go last and are pushed to the end of the row, so the
        // row reads as what you are looking at first and what you can do second.
        var actions = new Div(question, take);
        actions.addClassName("order-board__actions");

        var browse = new Div(field, past, actions);
        browse.addClassName("order-board__browse");

        var confirm = bulkAction(VaadinIcon.CHECK, "board.bulk.confirm",
                () -> bulk(OrderState.CONFIRMED, "ordering.history.confirmed"));
        var cancel = bulkAction(VaadinIcon.CLOSE_SMALL, "board.bulk.cancel",
                () -> bulk(OrderState.CANCELLED, "ordering.history.cancelled"));
        cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);

        var selected = new Div(confirm, cancel);
        selected.addClassName("order-board__selected");
        grid.addSelectionListener(event -> {
            var any = !event.getAllSelectedItems().isEmpty();
            confirm.setEnabled(any);
            cancel.setEnabled(any);
        });

        var toolbar = new Div(browse, selected);
        toolbar.addClassName("order-board__toolbar");
        return toolbar;
    }

    /**
     * An icon with a name. The two words fitted while the toolbar was one row
     * of six controls and stopped fitting when it became two groups, and a
     * verb on a button is worth less than the room the search field gets back.
     */
    private Button bulkAction(VaadinIcon icon, String labelKey, Runnable action) {
        var button = iconAction(icon, labelKey, action);
        button.setEnabled(false);
        return button;
    }

    /**
     * An icon button whose words live in its accessible name and its tooltip.
     * A toolbar of six controls does not have room for a verb on every button,
     * and the search field is worth more than the words are.
     */
    private Button iconAction(VaadinIcon icon, String labelKey, Runnable action) {
        var button = new Button(new Icon(icon), event -> action.run());
        Translations.bind(button, text -> {
            button.setAriaLabel(text);
            button.setTooltipText(text);
        }, labelKey);
        return button;
    }

    private void columnChooser() {
        columnChooser = ColumnChooser.onHeaderOf(grid, List.of(
                new ColumnChooser.Entry<>(referenceColumn, "board.column.reference"),
                new ColumnChooser.Entry<>(customerColumn, "board.column.customer"),
                new ColumnChooser.Entry<>(slotColumn, "board.column.slot"),
                new ColumnChooser.Entry<>(stateColumn, "board.column.state"),
                new ColumnChooser.Entry<>(summaryColumn, "board.column.items"),
                new ColumnChooser.Entry<>(channelColumn, "board.column.channel"),
                new ColumnChooser.Entry<>(totalColumn, "board.column.total")));
    }

    /**
     * Opens this order's panel, or closes it if this order's panel is the one
     * already open. Closing goes through the panel rather than navigating over
     * its head, so unsaved work still gets to ask.
     */
    private Button editButton(Order order) {
        var button = new Button(new Icon(VaadinIcon.EDIT), event -> {
            if (isPanelOpenFor(order)) {
                closePanel();
                return;
            }
            // The panel says everything the band says and more, so the band
            // closes: two views of one order at once is one of them wasting
            // half the board.
            collapseExpanded();
            getUI().ifPresent(ui -> ui.navigate(ROUTE + "/" + order.getReference()));
        });
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        button.addClassName("order-board__edit");
        Translations.bind(grid, text -> {
            button.setAriaLabel(text);
            button.setTooltipText(text);
        }, "board.editor.open");
        return button;
    }

    /** Closes whichever row is expanded, if any. */
    private void collapseExpanded() {
        if (expanded != null) {
            grid.setDetailsVisible(expanded, false);
            expanded = null;
        }
    }

    private boolean isPanelOpen() {
        return getUI().map(ui -> ui.getInternals().getActiveRouterTargetsChain().stream()
                .anyMatch(BoardPanel.class::isInstance)).orElse(false);
    }

    private boolean isPanelOpenFor(Order order) {
        return getUI().map(ui -> ui.getInternals().getActiveViewLocation())
                .map(location -> location.getPath().equals(ROUTE + "/" + order.getReference()))
                .orElse(false);
    }

    /**
     * The state's own colour, from the one palette that also colours the state
     * field in the panel: `data-state` picks the pair and the stylesheet holds
     * them, so the board and the editor cannot drift apart.
     */
    private Span stateBadge(Order order) {
        var badge = Translations.bindText(new Span(), order.getState().translationKey());
        badge.addClassName("state-chip");
        badge.getElement().setAttribute("data-state", order.getState().name());
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

    /** How many rows a bulk action changes before it asks first. */
    private static final int ASK_ABOVE = 25;

    private void bulk(OrderState target, String message) {
        var selected = new ArrayList<>(grid.getSelectedItems());
        if (selected.isEmpty()) {
            Notification.show(getTranslation("board.bulk.nothingSelected"));
            return;
        }
        // Select all reaches every order the filter matches, which on this
        // dataset is a four figure number, and each one is a transaction and a
        // history line. A handful goes straight through, the whole board says
        // how many first.
        if (selected.size() > ASK_ABOVE) {
            var dialog = new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
            dialog.setHeader(getTranslation("board.bulk.many.title"));
            dialog.setText(getTranslation("board.bulk.many.body", selected.size()));
            dialog.setCancelable(true);
            dialog.setCancelText(getTranslation("board.bulk.many.cancel"));
            dialog.setConfirmText(getTranslation("board.bulk.many.confirm"));
            dialog.addConfirmListener(event -> apply(target, message, selected));
            dialog.open();
            return;
        }
        apply(target, message, selected);
    }

    private void apply(OrderState target, String message, java.util.List<Order> selected) {
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

    /**
     * The board's current filter, in one place, because the select all control
     * has to mean exactly what the grid is showing and not something close to
     * it.
     */
    private org.springframework.data.jpa.domain.Specification<Order> specification(
            String term, boolean withPast) {
        // A search is a search: when somebody types a reference, they mean any
        // order, not only the ones still to come.
        var searching = term != null && !term.isBlank();
        var from = withPast || searching ? null : slots.today().minusDays(1);
        // Arrays.asList, not List.of: an inactive filter is null, and List.of
        // rejects nulls with an NPE that surfaces only on stderr.
        return OrderSpecifications.all(java.util.Arrays.asList(
                OrderSpecifications.matching(term),
                OrderSpecifications.fromDate(from)));
    }

    private static org.springframework.data.domain.Sort boardOrder() {
        return org.springframework.data.domain.Sort.by("pickupDate").ascending()
                .and(org.springframework.data.domain.Sort.by("pickupTime").ascending());
    }

    private void applyFilter(String term, boolean withPast) {
        var specification = specification(term, withPast);

        // Lazy, page by page, with the sort the board is read in. The counter
        // makes the cost of a page visible to the diagnostics view and to the
        // test that proves hidden columns are free.
        grid.setItemsPageable(pageable -> {
            counter.countQuery();
            var sorted = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(), boardOrder());
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

    /** The column chooser, for the tests that assert on what it offers. */
    com.vaadin.flow.component.grid.contextmenu.GridContextMenu<Order> chooserMenu() {
        return columnChooser;
    }
}
