package com.vaadin.bakery.catalogue.ui;

import com.vaadin.bakery.base.signals.Children;
import com.vaadin.bakery.catalogue.Allergen;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.catalogue.ProductCard;
import com.vaadin.bakery.catalogue.ProductImageRepository;
import com.vaadin.bakery.ordering.CartSignals;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The public catalogue. Filters are signals, the card list is computed from
 * them, and the whole thing is mirrored into the URL so a filtered catalogue is
 * shareable.
 *
 * It is also the layout of the product page, which opens over it rather than
 * replacing it: a person browsing a catalogue looks at one thing, goes back and
 * looks at the next, and a full page navigation loses the filters, the scroll
 * position and the place they had reached. The overlay is forced rather than
 * left to the width, because the list wants the whole page underneath it and
 * not half of one.
 */
@ParentLayout(com.vaadin.bakery.base.ui.MainLayout.class)
@Route(value = "shop", layout = com.vaadin.bakery.base.ui.MainLayout.class)
@PageTitle("Shop")
@Menu(order = 1, title = "Shop", icon = "vaadin:shop")
@AnonymousAllowed
public class StorefrontView extends MasterDetailLayout implements HasUrlParameter<String>, BeforeEnterObserver {

    private final CatalogueService catalogue;
    private final CartSignals cart;
    private final CatalogueFilters filters = new CatalogueFilters();
    private final ListSignal<ProductCard> visible = new ListSignal<>();
    private final Div grid = new Div();
    private final Div emptyState = new Div();
    private final List<ProductCard> all;
    /** How many times the filters have been applied. Read by the tests that
     * prove one user action causes one refilter. */
    int refreshCount;

    /** Test seam: what the list is filtered by, which surviving a product
     * panel is the point of opening one over the list. */
    CatalogueFilters filtersForTest() {
        return filters;
    }

    public StorefrontView(CatalogueService catalogue, CartSignals cart, ProductImageRepository images) {
        this.catalogue = catalogue;
        this.cart = cart;
        addClassName("storefront-view");

        var uploaded = images.findAll().stream()
                .map(image -> image.getProduct().getId())
                .collect(Collectors.toSet());
        // Forty eight products fit in memory comfortably, so filtering happens
        // here rather than in the database. A catalogue of thousands would move
        // this into a Specification, which is why the filter state is a signal
        // and not a field.
        this.all = catalogue.availableProducts().stream()
                .map(product -> ProductCard.of(product, uploaded.contains(product.getId())))
                .toList();

        grid.addClassName("storefront-view__grid");
        emptyState.addClassName("storefront-view__empty");

        var list = new Div(Translations.bindText(new H2(), "catalogue.title"), filterBar(), grid, emptyState);
        list.addClassName("storefront-view__master");
        setSizeFull();
        setMaster(list);
        setForceOverlay(true);
        setOverlaySize("38rem");
        setOverlayContainment(MasterDetailLayout.OverlayContainment.LAYOUT);

        // Escape and a click outside are the two ways everybody already knows.
        addBackdropClickListener(event -> closeProduct());
        addDetailEscapePressListener(event -> closeProduct());

        Children.bind(this, grid, visible, cardSignal -> {
            var card = cardSignal.peek();
            return new ProductCardComponent(card, this::addToCart);
        });

        Signal.effect(this, this::refresh);
    }

    /** Back to the list, with the list exactly as it was left. */
    private void closeProduct() {
        getUI().ifPresent(ui -> ui.navigate(StorefrontView.class));
    }

    /**
     * The four filters as a form rather than a row of fields.
     *
     * Auto responsive rather than responsive steps: the column count is derived
     * from the bar's own width against {@code columnWidth}, and the labels move
     * beside their fields when there is room and back above them when there is
     * not. That last part is the reason for the mode. It is a measurement the
     * component makes of itself, and a breakpoint cannot express it, because the
     * bar's width depends on the drawer and not on the window.
     */
    private Component filterBar() {
        var search = new TextField();
        Translations.bind(search, search::setPlaceholder, "catalogue.search.placeholder");
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.setClearButtonVisible(true);
        search.addValueChangeListener(event -> filters.search().set(event.getValue()));

        var category = new Select<String>();
        var categories = catalogue.categories().stream().map(item -> item.getName()).toList();
        category.setItems(categories);
        category.setEmptySelectionAllowed(true);
        Translations.bind(category, category::setEmptySelectionCaption, "catalogue.category.all");
        category.addValueChangeListener(event ->
                filters.category().set(event.getValue() == null ? "" : event.getValue()));

        // The 25.3 change event semantics matter here: the value arrives once
        // per user action, so the catalogue refilters once and not once per chip.
        var allergens = new MultiSelectComboBox<Allergen>();
        // Empty, it was the only control on the bar with nothing written in it,
        // which reads as a field that failed to load rather than as a filter
        // nobody has used yet.
        Translations.bind(allergens, allergens::setPlaceholder,
                "catalogue.allergens.exclude.placeholder");
        allergens.setItems(catalogue.allergens());
        // Re-setting the generator is what makes the rendered chips and the
        // dropdown redraw; refreshing the data provider alone leaves the
        // selected chips in the old language.
        Translations.onLocale(allergens, locale ->
                allergens.setItemLabelGenerator(item -> getTranslation(locale, item.translationKey())));
        allergens.addValueChangeListener(event -> filters.excludedAllergens().set(
                event.getValue().stream().map(Allergen::translationKey).collect(Collectors.toSet())));

        var sort = new Select<CatalogueFilters.Sort>();
        sort.setItems(CatalogueFilters.Sort.values());
        Translations.onLocale(sort, locale ->
                sort.setItemLabelGenerator(value -> getTranslation(locale, value.translationKey())));
        sort.setValue(CatalogueFilters.Sort.RELEVANCE);
        sort.addValueChangeListener(event -> filters.sort().set(event.getValue()));

        var bar = new FormLayout();
        bar.addClassName("storefront-view__filters");
        // Four columns, two, then one, and the labels move beside their fields
        // only in that last state. Four filters divide evenly into all three
        // counts, so no state ever strands a field on a row of its own: that is
        // what a three column state could not do, and it is why none of these
        // needs a colspan. A field's colspan is one static number clamped to the
        // active step's column count, so it cannot be one thing at four columns
        // and another at two anyway.
        //
        // The width these steps measure is the bar's own and not the window's,
        // so they hold whether or not the drawer is open.
        bar.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE),
                new FormLayout.ResponsiveStep("34em", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("52em", 4, FormLayout.ResponsiveStep.LabelsPosition.TOP));
        bar.setLabelWidth("6em");
        filter(bar, search, "catalogue.search");
        filter(bar, category, "catalogue.category");
        filter(bar, allergens, "catalogue.allergens.exclude");
        filter(bar, sort, "catalogue.sort");
        return bar;
    }

    /**
     * One labelled filter. The label belongs to the form item and not to the
     * field, because that is what the layout moves to the side of the field: a
     * label set on the field itself stays above it at every width.
     */
    private static FormLayout.FormItem filter(FormLayout bar, Component field, String key) {
        return bar.addFormItem(field, Translations.bindText(new Span(), key));
    }

    private void addToCart(ProductCard card) {
        cart.add(card.id(), 1, null);
        // Say what is in the basket now, not just what was added: the number is
        // the thing somebody is actually keeping track of.
        Notification.show(getTranslation("catalogue.addedToCart.count", card.name(), cart.count()))
                .setPosition(Notification.Position.BOTTOM_END);
    }

    /** One effect keeps the visible list, the empty state and the URL in step. */
    private void refresh() {
        refreshCount++;
        var matching = filters.apply(all);
        var current = visible.peek().stream().map(signal -> signal.peek()).toList();
        if (!current.equals(matching)) {
            visible.peek().forEach(visible::remove);
            visible.insertAllLast(matching);
        }
        boolean empty = matching.isEmpty();
        grid.setVisible(!empty);
        emptyState.setVisible(empty);
        if (empty) {
            emptyState.removeAll();
            emptyState.add(Translations.bindText(new Paragraph(), "catalogue.empty"));
            var clear = Translations.bindText(new Button("", event -> filters.clear()),
                    "catalogue.empty.clear");
            clear.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            emptyState.add(clear);
        }
        updateQueryParameters();
    }

    private void updateQueryParameters() {
        var parameters = new java.util.LinkedHashMap<String, List<String>>();
        if (!filters.search().peek().isBlank()) {
            parameters.put("q", List.of(filters.search().peek()));
        }
        if (!filters.category().peek().isBlank()) {
            parameters.put("category", List.of(filters.category().peek()));
        }
        if (!filters.excludedAllergens().peek().isEmpty()) {
            parameters.put("without", List.copyOf(filters.excludedAllergens().peek()));
        }
        getUI().ifPresent(ui -> ui.getPage().getHistory().replaceState(null,
                new com.vaadin.flow.router.Location("shop",
                        new com.vaadin.flow.router.QueryParameters(parameters))));
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter != null && !parameter.isBlank()) {
            catalogue.categories().stream()
                    .filter(category -> category.getSlug().equals(parameter))
                    .findFirst()
                    .ifPresent(category -> filters.category().set(category.getName()));
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Map<String, List<String>> parameters = event.getLocation().getQueryParameters().getParameters();
        if (parameters.containsKey("q")) {
            filters.search().set(parameters.get("q").getFirst());
        }
        if (parameters.containsKey("category")) {
            filters.category().set(parameters.get("category").getFirst());
        }
        if (parameters.containsKey("without")) {
            filters.excludedAllergens().set(Set.copyOf(parameters.get("without")));
        }
    }
}
