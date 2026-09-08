package com.vaadin.bakery.base.ui;

import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.catalogue.ProductCard;
import com.vaadin.bakery.catalogue.ProductImageRepository;
import com.vaadin.bakery.catalogue.ui.ProductCardComponent;
import com.vaadin.bakery.ordering.CartSignals;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.local.ListSignal;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/** The front door: what is open today, and what is worth buying. */
@Route("")
@PageTitle("Bakery")
@AnonymousAllowed
public class HomeView extends VerticalLayout {

    private final CartSignals cart;

    public HomeView(CatalogueService catalogue, CartSignals cart, ProductImageRepository images,
            PickupLocationRepository locations) {
        this.cart = cart;
        addClassName("home-view");

        var hero = new Div();
        hero.addClassNames("home-view__hero", "page-block");
        hero.add(Translations.bindText(new H1(), "app.name"));
        hero.add(Translations.bindText(new Paragraph(), "home.tagline"));

        var hours = new Div();
        hours.addClassName("home-view__hours");
        var formatter = DateTimeFormatter.ofPattern("HH:mm");
        locations.findByActiveTrueOrderByNameAsc().forEach(location -> {
            var line = Translations.bindText(new Span(), "home.hours.line", location.getName(),
                    location.getOpensAt().format(formatter), location.getClosesAt().format(formatter));
            line.addClassName("home-view__hours-line");
            hours.add(line);
        });
        hero.add(hours);
        hero.add(Translations.bindText(new Anchor("shop", ""), "home.browse"));

        var uploaded = images.findAll().stream()
                .map(image -> image.getProduct().getId())
                .collect(Collectors.toSet());
        var featured = new ListSignal<ProductCard>();
        featured.insertAllLast(catalogue.featured().stream()
                .map(product -> ProductCard.of(product, uploaded.contains(product.getId())))
                .toList());

        var grid = new Div();
        grid.addClassName("home-view__featured");
        grid.bindChildren(featured, cardSignal ->
                new ProductCardComponent(cardSignal.peek(), this::addToCart));

        var featuredTitle = Translations.bindText(new H2(), "home.featured");
        featuredTitle.addClassName("page-block");
        add(hero, featuredTitle, grid);
    }

    private void addToCart(ProductCard card) {
        cart.add(card.id(), 1, null);
        Notification.show(getTranslation("catalogue.addedToCart", card.name()));
    }
}
