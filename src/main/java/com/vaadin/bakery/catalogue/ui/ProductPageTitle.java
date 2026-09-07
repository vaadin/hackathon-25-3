package com.vaadin.bakery.catalogue.ui;

import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.PageTitleGenerator;
import com.vaadin.flow.server.VaadinService;
import java.util.Locale;
import java.util.Optional;

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
        return slug(context)
                .flatMap(catalogue::bySlug)
                .map(product -> product.getName())
                .orElseGet(this::noSuchProduct);
    }

    /**
     * The slug, from the context when there is one and from the open location
     * when there is not.
     *
     * The router calls this with the route parameters and everything works.
     * {@code MenuConfiguration.getPageHeader}, which is what the shell binds
     * its header to, calls the same generator with a context carrying none, so
     * the slug is absent, so the header reads "we cannot find that product"
     * over the product the page is showing. Recorded in
     * {@code specs/FEEDBACK-25.3.md}: delete this the day the context is
     * complete whoever asks.
     */
    private Optional<String> slug(PageTitleContext context) {
        var fromContext = context.routeParameters().get("slug");
        if (fromContext.isPresent()) {
            return fromContext;
        }
        return Optional.ofNullable(UI.getCurrent())
                .map(ui -> ui.getInternals().getActiveViewLocation())
                .map(location -> location.getSegments())
                .filter(segments -> segments.size() == 3 && "shop".equals(segments.get(0))
                        && ProductDetailView.SEGMENT.equals(segments.get(1)))
                .map(segments -> segments.get(2));
    }

    /** The tab of a slug nobody sells says the same as the page under it. */
    private String noSuchProduct() {
        var locale = UI.getCurrent() != null ? UI.getCurrent().getLocale() : Locale.ENGLISH;
        return VaadinService.getCurrent().getInstantiator().getI18NProvider()
                .getTranslation("catalogue.product.notFound", locale);
    }
}
