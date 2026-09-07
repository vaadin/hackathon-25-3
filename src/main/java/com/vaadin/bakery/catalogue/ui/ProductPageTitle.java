package com.vaadin.bakery.catalogue.ui;

import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.PageTitleGenerator;
import com.vaadin.flow.server.VaadinService;
import java.util.Locale;

/**
 * The 25.3 way of naming a page whose title depends on the route: the generator
 * gets the route parameters and never needs a view instance, which is what lets
 * breadcrumbs label a parent route that has not been created.
 *
 * Deliberately not a Spring bean. A `PageTitleGenerator` bean is the whole
 * application's generator, so this one, which only knows how to name a product,
 * was naming every page in the bakery. `@DynamicPageTitle` on the view is the
 * registration that was wanted, and it instantiates this class through the
 * Spring aware instantiator, constructor injection included.
 */
public class ProductPageTitle implements PageTitleGenerator {

    private final transient CatalogueService catalogue;

    public ProductPageTitle(CatalogueService catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public String generatePageTitle(PageTitleContext context) {
        return context.routeParameters().get("slug")
                .flatMap(catalogue::bySlug)
                .map(product -> product.getName())
                .orElseGet(this::noSuchProduct);
    }

    /** The tab of a slug nobody sells says the same as the page under it. */
    private String noSuchProduct() {
        var locale = UI.getCurrent() != null ? UI.getCurrent().getLocale() : Locale.ENGLISH;
        return VaadinService.getCurrent().getInstantiator().getI18NProvider()
                .getTranslation("catalogue.product.notFound", locale);
    }
}
