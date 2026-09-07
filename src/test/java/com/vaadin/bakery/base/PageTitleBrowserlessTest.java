package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.ui.AboutView;
import com.vaadin.bakery.ordering.ui.CartView;
import com.vaadin.bakery.ordering.ui.CheckoutSlotView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Every route names itself.
 *
 * What this catches: a route that loses its title, or a title that stops being
 * resolved from the route rather than from a view instance.
 *
 * What it does not catch, and this is worth knowing before trusting it: the way
 * they all stopped. `ProductPageTitle` was annotated `@Component`, and a
 * `PageTitleGenerator` bean is the whole application's generator rather than
 * only the view that named it in `@DynamicPageTitle`. It knew how to name a
 * product and returned a constant for everything else, so every tab in the
 * bakery read "Bakery", and so did every label in every router driven breadcrumb
 * trail. This tier stayed green throughout: with the bean in place, both
 * assertions below still pass, and the browser still showed "Bakery" everywhere.
 * That check needs a real one, `PageTitleIT`, and it is not written.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class PageTitleBrowserlessTest extends SpringBrowserlessTest {

    private String title() {
        return UI.getCurrent().getInternals().getTitle();
    }

    @Test
    void eachRouteCarriesItsOwnTitle() {
        navigate(CartView.class);
        assertEquals("Cart", title());

        navigate(CheckoutSlotView.class);
        assertEquals("Pickup", title(), "not the application name, the step's own");

        navigate(AboutView.class);
        assertEquals("About", title());
    }
}
