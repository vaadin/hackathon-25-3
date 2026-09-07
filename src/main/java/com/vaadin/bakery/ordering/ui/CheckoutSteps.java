package com.vaadin.bakery.ordering.ui;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;

/**
 * The trail is derived from the route hierarchy declared with RouteParent, so
 * nobody writes a breadcrumb by hand and nobody forgets to update one.
 */
final class CheckoutSteps {

    private CheckoutSteps() {
    }

    static Breadcrumbs breadcrumbs() {
        var breadcrumbs = new Breadcrumbs(Breadcrumbs.Mode.ROUTER);
        breadcrumbs.addClassName("checkout-view__steps");
        return breadcrumbs;
    }
}
