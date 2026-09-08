package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.router.RouteParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** SHOP-05 and the dynamic page title. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class ProductPageBrowserlessTest extends SpringBrowserlessTest {

    // Built by hand, because it deliberately is not a bean: a PageTitleGenerator
    // bean names every page in the application, not just the one that registered
    // it with @DynamicPageTitle.
    private ProductPageTitle titleGenerator;

    @Autowired
    private CatalogueService catalogue;

    @Test
    void theProductPageShowsItsNameAndMarkdownDescription() {
        navigate("shop/product/carrot-cake", ProductDetailView.class);

        assertEquals("Carrot cake", find(H1.class).single().getText());
        // Against the product's own text rather than against a heading the
        // dataset happens to open with: this asserts that the raw markdown
        // reaches the component, which renders it client side, and it keeps
        // asserting that when the dataset is regenerated.
        var product = catalogue.bySlug("carrot-cake").orElseThrow();
        var opening = product.getDescriptionMarkdown().strip().lines().findFirst().orElseThrow();
        var markdown = find(Markdown.class).single();
        assertTrue(markdown.getContent().contains(opening),
                "the product's own words reach the component, got " + markdown.getContent());
        assertTrue(markdown.getContent().contains("- "), "and it is still markdown, not rendered html");
    }

    @Test
    void theTitleComesFromTheRouteWithoutAViewInstance() {
        var context = new com.vaadin.flow.router.PageTitleContext(ProductDetailView.class,
                new RouteParameters("slug", "cheesecake"), com.vaadin.flow.router.QueryParameters.empty(), null);

        titleGenerator = new ProductPageTitle(catalogue);
        assertEquals("Cheesecake", titleGenerator.generatePageTitle(context));
    }

    @Test
    void anUnknownSlugExplainsItselfInsteadOfFailing() {
        navigate("shop/product/there-is-no-such-thing", ProductDetailView.class);

        assertTrue(find(H1.class).single().getText().contains("could not find"));
    }
}
