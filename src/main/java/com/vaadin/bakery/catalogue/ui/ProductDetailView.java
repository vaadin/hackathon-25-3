package com.vaadin.bakery.catalogue.ui;

import com.vaadin.bakery.base.SafeHtml;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.catalogue.Product;
import com.vaadin.bakery.catalogue.ProductImageRepository;
import com.vaadin.bakery.catalogue.ProductImages;
import com.vaadin.bakery.ordering.CartSignals;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.DynamicPageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * One product, in the panel over the catalogue. The description is markdown
 * bound to a signal, which is the same code path the administrator's live
 * preview uses, so what an admin sees while typing is exactly what a visitor
 * gets.
 */
@Route(value = "shop/" + ProductDetailView.SEGMENT + "/:slug", layout = StorefrontView.class)
@DynamicPageTitle(ProductPageTitle.class)
@AnonymousAllowed
public class ProductDetailView extends VerticalLayout implements BeforeEnterObserver {

    /** Under the catalogue, because it opens over the catalogue. */
    public static final String SEGMENT = "product";

    private final CatalogueService catalogue;
    private final ProductImageRepository images;
    private final CartSignals cart;
    private final ValueSignal<String> description = new ValueSignal<>("");

    public ProductDetailView(CatalogueService catalogue, ProductImageRepository images, CartSignals cart) {
        this.catalogue = catalogue;
        this.images = images;
        this.cart = cart;
        addClassName("product-view");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        var product = event.getRouteParameters().get("slug").flatMap(catalogue::bySlug).orElse(null);
        if (product == null) {
            add(close(), Translations.bindText(new H1(), "catalogue.product.unknown"));
            return;
        }
        render(product);
    }

    /**
     * The list is still there behind the panel, so this closes rather than
     * navigating away: the anchor it replaces reloaded the catalogue and threw
     * away the filters somebody had just set.
     */
    private Button close() {
        var button = new Button(new com.vaadin.flow.component.icon.Icon(
                com.vaadin.flow.component.icon.VaadinIcon.CLOSE),
                event -> getUI().ifPresent(ui -> ui.navigate(StorefrontView.class)));
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        // A cross with no words still has to say what it does out loud, and the
        // escape key and a click outside already do the same thing.
        Translations.bind(button, button::setAriaLabel, "catalogue.backToShop");
        Translations.bind(button, button::setTooltipText, "catalogue.backToShop");
        button.addClassName("product-view__close");
        return button;
    }

    private void render(Product product) {
        boolean uploaded = images.findByProduct(product).isPresent();
        var media = new Div(new Image(ProductImages.url(product, uploaded), product.getName()));
        media.addClassName("product-view__media");

        var price = Translations.bindText(new Span(), locale -> product.price().format(locale));
        price.addClassName("product-view__price");

        var allergens = new Div();
        allergens.addClassName("product-view__allergens");
        product.getAllergens().forEach(allergen -> {
            var chip = Translations.bindText(new Span(), allergen.translationKey());
            chip.getElement().getThemeList().add("badge small");
            allergens.add(chip);
        });

        // The description is authored by staff and still cleaned before it is
        // rendered. There is exactly one safelist in this application.
        description.set(SafeHtml.clean(product.getDescriptionMarkdown()));
        var markdown = new Markdown(description);
        markdown.addClassName("product-view__description");

        var quantity = new IntegerField();
        Translations.bind(quantity, quantity::setLabel, "catalogue.quantity");
        quantity.setValue(1);
        quantity.setMin(1);
        quantity.setMax(99);
        quantity.setStepButtonsVisible(true);

        var add = Translations.bindText(new Button("", event -> {
            cart.add(product.getId(), quantity.getValue() == null ? 1 : quantity.getValue(), null);
            Notification.show(getTranslation("catalogue.addedToCart", product.getName()));
        }), "catalogue.addToCart");
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Name, price and allergens sit beside the photograph; everything that
        // wants the full width of the panel goes under both of them.
        var header = new Div(new H1(product.getName()), price, allergens);
        header.addClassName("product-view__header");

        var body = new Div(markdown);
        body.addClassName("product-view__body");
        if (product.getLeadTimeDays() > 0) {
            body.add(Translations.bindText(new Paragraph(), "catalogue.leadTime.notice",
                    product.getLeadTimeDays()));
        }

        var layout = new Div(media, header, body);
        layout.addClassName("product-view__layout");
        if (!product.isAvailable()) {
            body.add(Translations.bindText(new Paragraph(), "catalogue.notAvailable"));
        } else {
            var actions = new Div(quantity, add);
            actions.addClassName("product-view__actions");
            layout.add(actions);
        }
        add(close(), layout);
    }
}
