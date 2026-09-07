package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.signals.Children;
import com.vaadin.bakery.catalogue.Product;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.bakery.ordering.CartSignals;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * The basket. Every number on this page is a computed signal over the same
 * session bean the header badge reads, so they cannot disagree.
 */
@Route("cart")
@PageTitle("Cart")
@AnonymousAllowed
public class CartView extends VerticalLayout {

    private final CartSignals cart;
    private final ProductRepository products;

    public CartView(CartSignals cart, ProductRepository products) {
        this.cart = cart;
        this.products = products;
        addClassName("cart-view");

        var lines = new Div();
        lines.addClassName("cart-view__lines");
        Children.bind(this, lines, cart.lines(), this::row);

        var empty = Translations.bindText(new Paragraph(), "cart.empty");
        empty.bindVisible(Signal.computed(() -> cart.itemCount().get() == 0));
        lines.bindVisible(Signal.computed(() -> cart.itemCount().get() > 0));

        add(Translations.bindText(new H2(), "cart.title"), empty, lines, totals(), actions());
    }

    private Div row(ValueSignal<CartLine> lineSignal) {
        var line = lineSignal.peek();
        var product = products.findById(line.productId()).orElse(null);
        var row = new Div();
        row.addClassName("cart-view__row");
        if (product == null) {
            row.add(Translations.bindText(new Span(), "cart.line.gone"));
            return row;
        }

        var image = new Image(com.vaadin.bakery.catalogue.ProductImages.url(product, false), product.getName());
        image.addClassName("cart-view__image");

        var name = new Anchor("shop/" + com.vaadin.bakery.catalogue.ui.ProductDetailView.SEGMENT + "/"
                + product.getSlug(), product.getName());
        name.addClassName("cart-view__name");

        var quantity = new IntegerField();
        quantity.setValue(line.quantity());
        quantity.setMin(0);
        quantity.setMax(99);
        quantity.setStepButtonsVisible(true);
        quantity.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        Translations.bind(quantity, quantity::setAriaLabel, "cart.quantity.aria", product.getName());
        // Zero removes the line, which is what everybody tries first.
        quantity.addValueChangeListener(event ->
                cart.setQuantity(product.getId(), event.getValue() == null ? 0 : event.getValue()));

        var comment = new TextField();
        Translations.bind(comment, comment::setPlaceholder, "cart.comment.placeholder");
        comment.setValue(line.comment() == null ? "" : line.comment());
        comment.setValueChangeMode(ValueChangeMode.LAZY);
        comment.addValueChangeListener(event -> lineSignal.update(current -> current.withComment(event.getValue())));

        var price = Translations.bindText(new Span(),
                locale -> product.price().times(line.quantity()).format(locale));
        price.addClassName("cart-view__price");

        var remove = new Button(new Icon(VaadinIcon.TRASH), event -> cart.remove(product.getId()));
        remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        Translations.bind(remove, remove::setAriaLabel, "cart.remove.aria", product.getName());

        if (!product.isAvailable()) {
            var warning = Translations.bindText(new Span(), "cart.line.unavailable", product.getName());
            warning.getElement().getThemeList().add("badge error small");
            row.add(warning);
        }

        row.add(image, name, comment, quantity, price, remove);
        return row;
    }

    private Div totals() {
        var net = Translations.bindText(new Span(),
                locale -> getTranslation(locale, "cart.net", cart.net().get().format(locale)));
        var vat = Translations.bindText(new Span(),
                locale -> getTranslation(locale, "cart.vat", cart.vat().get().format(locale)));
        var gross = Translations.bindText(new Span(),
                locale -> getTranslation(locale, "cart.gross", cart.gross().get().format(locale)));
        gross.addClassName("cart-view__total");

        var totals = new Div(net, vat, gross);
        totals.addClassName("cart-view__totals");
        totals.bindVisible(Signal.computed(() -> cart.itemCount().get() > 0));
        return totals;
    }

    private Div actions() {
        var keepShopping = Translations.bindText(new Anchor("shop", ""), "cart.keepShopping");

        var checkout = Translations.bindText(new Button("",
                event -> getUI().ifPresent(ui -> ui.navigate("checkout/contact"))), "cart.checkout");
        checkout.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        // Nothing to check out with, and nothing to check out that we cannot sell.
        checkout.bindEnabled(Signal.computed(() -> cart.itemCount().get() > 0 && !hasUnavailableLine()));

        var actions = new Div(keepShopping, checkout);
        actions.addClassName("cart-view__actions");
        return actions;
    }

    private boolean hasUnavailableLine() {
        return cart.snapshot().stream()
                .map(line -> products.findById(line.productId()).orElse(null))
                .anyMatch(product -> product == null || !product.isAvailable());
    }
}
