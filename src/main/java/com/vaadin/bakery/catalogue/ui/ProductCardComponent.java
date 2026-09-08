package com.vaadin.bakery.catalogue.ui;

import com.vaadin.bakery.catalogue.ProductCard;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableConsumer;
import java.util.Locale;

/**
 * One product on the storefront.
 *
 * Note the composition: in 25.3 {@code Image} is no longer an
 * {@code HtmlContainer}, so nothing nests inside it. The badge and the overlay
 * are siblings in a positioned wrapper instead of children of the image.
 */
public class ProductCardComponent extends Div {

    public ProductCardComponent(ProductCard card, SerializableConsumer<ProductCard> onAdd) {
        addClassName("product-card");

        var href = "shop/" + ProductDetailView.SEGMENT + "/" + card.slug();

        var media = new Div();
        media.addClassName("product-card__media");
        var image = new Image(card.imageUrl(), card.name());
        image.addClassName("product-card__image");
        image.getElement().setAttribute("loading", "lazy");
        // The photograph opens the product too. It is the largest thing on the
        // card and the thing somebody actually points at, and a card whose only
        // way in is its title reads as a card that does not open.
        //
        // A second link to the same route with the same name would be announced
        // twice and would be a second tab stop, so this one is out of the tab
        // order and hidden from assistive technology: the title next to it is
        // the accessible way in. The badge stays outside the link, because
        // "order two days ahead" is content and not decoration.
        var imageLink = new Anchor(href, image);
        imageLink.addClassName("product-card__image-link");
        imageLink.getElement().setAttribute("tabindex", "-1");
        imageLink.getElement().setAttribute("aria-hidden", "true");
        media.add(imageLink);
        if (card.needsLeadTime()) {
            var badge = Translations.bindText(new Span(), "catalogue.leadTime.badge", card.leadTimeDays());
            badge.addClassNames("product-card__badge");
            badge.getElement().getThemeList().add("badge contrast small");
            media.add(badge);
        }

        var title = new Anchor(href, card.name());
        title.addClassName("product-card__title");

        var price = Translations.bindText(new Span(), locale -> card.price().format(locale));
        price.addClassName("product-card__price");

        var allergens = new Div();
        allergens.addClassName("product-card__allergens");
        card.allergenKeys().forEach(key -> {
            var chip = Translations.bindText(new Span(), key);
            chip.getElement().getThemeList().add("badge small");
            allergens.add(chip);
        });

        var add = Translations.bindText(new Button("", event -> onAdd.accept(card)), "catalogue.addToCart");
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add.addClassName("product-card__add");
        Translations.bind(add, add::setAriaLabel, "catalogue.addToCart.aria", card.name());

        var footer = new Div(price, add);
        footer.addClassName("product-card__footer");

        add(media, title, allergens, footer);
    }
}
