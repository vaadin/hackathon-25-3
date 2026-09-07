package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.ordering.CartSignals;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.textfield.IntegerField;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * CART-03, the half nobody covered. Removal is tested and adding is tested; the
 * case in between, changing a quantity to something other than zero, is the one
 * a customer actually does and it was never asserted.
 *
 * The badge and the totals are separate signals over the same lines, so they
 * can disagree, and a customer who has just changed a three to a two and sees
 * the old total has no reason to trust the next number either.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class CartQuantityBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private CatalogueService catalogue;

    @Autowired
    private org.springframework.context.ApplicationContext context;

    /**
     * The cart is session scoped, and a session scoped bean cannot be a field
     * here: it is resolved before the session exists. Every test navigates
     * first and asks for the bean after. The row is in
     * {@code specs/FEEDBACK-25.3.md}.
     */
    private CartSignals cart() {
        return context.getBean(CartSignals.class);
    }

    @Test
    void changingAQuantityMovesTheBadgeAndTheTotals() {
        var product = catalogue.availableProducts().getFirst();
        navigate(com.vaadin.bakery.catalogue.ui.StorefrontView.class);
        var cart = cart();
        cart.add(product.getId(), 3, null);
        navigate(CartView.class);

        assertEquals(3, cart.count(), "three to begin with");
        var totalOfThree = cart.grossAmount().cents();

        var quantity = find(IntegerField.class).first();
        test(quantity).setValue(2);

        assertEquals(2, cart.count(), "the badge follows the line");
        var totalOfTwo = cart.grossAmount().cents();
        assertTrue(totalOfTwo < totalOfThree,
                "and so does the total: " + totalOfThree + " became " + totalOfTwo);
        assertEquals(totalOfThree / 3 * 2, totalOfTwo,
                "by exactly one unit's worth");
    }

    /** Upwards too, which is a different code path in a stepper. */
    @Test
    void raisingAQuantityMovesThemAsWell() {
        var product = catalogue.availableProducts().getFirst();
        navigate(com.vaadin.bakery.catalogue.ui.StorefrontView.class);
        var cart = cart();
        cart.add(product.getId(), 1, null);
        navigate(CartView.class);
        var totalOfOne = cart.grossAmount().cents();

        test(find(IntegerField.class).first()).setValue(4);

        assertEquals(4, cart.count(), "four now");
        assertEquals(totalOfOne * 4, cart.grossAmount().cents(), "and four times the money");
    }
}
