package com.example;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.DynamicPageTitle;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Two routes. One asks for the dynamic generator, the other declares a plain
 * title of its own.
 *
 * Open http://localhost:8098 and read the browser tab. It says "We cannot find
 * that product", which is this generator's fallback, on a route that declared
 * `@PageTitle("About")` and never asked for a generator at all.
 *
 * Then delete the `@Component` from `ProductTitle`, restart, and the tab says
 * "About". `@DynamicPageTitle` still works on the product route, because it
 * instantiates the class through the Spring aware instantiator.
 *
 * Nothing warns. The two registrations look independent, and a server side test
 * suite cannot see it: `Router.resolvePageTitle` answers correctly throughout.
 */
public final class Views {

    private Views() {
    }

    @Route("")
    @PageTitle("About")
    public static class AboutView extends VerticalLayout {
        public AboutView() {
            add(new H2("About"));
            add(new Paragraph("This route declares @PageTitle(\"About\"). Read the browser tab."));
            add(new Anchor("product/sourdough", "A product page, which does want the generator"));
        }
    }

    @Route("product/:slug")
    @DynamicPageTitle(ProductTitle.class)
    public static class ProductView extends VerticalLayout {
        public ProductView() {
            add(new H2("A product"));
            add(new Anchor("", "Back to the route that has its own title"));
        }
    }
}
