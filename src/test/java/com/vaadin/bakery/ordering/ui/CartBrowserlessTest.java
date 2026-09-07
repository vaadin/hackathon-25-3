package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.bakery.catalogue.ui.StorefrontView;
import com.vaadin.bakery.ordering.CartSignals;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.IntegerField;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** CART-01, CART-02, CART-03 and CART-10. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class CartBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private org.springframework.context.ApplicationContext context;

    @Autowired
    private ProductRepository products;

    /**
     * The cart is session scoped, so it can only be resolved once a Vaadin
     * session exists. Every test navigates first and asks for the bean after.
     */
    private CartSignals cart() {
        return context.getBean(CartSignals.class);
    }

    @Test
    void addingFromACardUpdatesTheCount() {
        navigate(StorefrontView.class);
        var cart = cart();
        assertEquals(0, cart.count());

        var add = find(Button.class).withText("Add").first();
        test(add).click();

        assertEquals(1, cart.count(), "the badge reads this same signal");
    }

    @Test
    void theCartPageShowsEveryLineAndTheTotals() {
        var croissant = products.findBySlug("butter-croissant").orElseThrow();
        var bread = products.findBySlug("sourdough-loaf").orElseThrow();
        navigate(CartView.class);
        var cart = cart();
        cart.add(croissant.getId(), 2, null);
        cart.add(bread.getId(), 1, null);
        navigate(CartView.class);

        assertEquals(2, find(IntegerField.class).all().size(), "one quantity field per line");
        int expectedNet = croissant.getPriceCents() * 2 + bread.getPriceCents();
        assertEquals(expectedNet, cart.netAmount().cents());
    }

    @Test
    void settingAQuantityToZeroRemovesTheLine() {
        var croissant = products.findBySlug("butter-croissant").orElseThrow();
        navigate(CartView.class);
        var cart = cart();
        cart.add(croissant.getId(), 2, null);
        navigate(CartView.class);

        var quantity = find(IntegerField.class).single();
        test(quantity).setValue(0);

        assertEquals(0, cart.count());
        assertTrue(cart.snapshot().isEmpty());
    }

    @Test
    void addingTheSameProductTwiceRaisesTheQuantity() {
        var croissant = products.findBySlug("butter-croissant").orElseThrow();
        navigate(CartView.class);
        var cart = cart();
        cart.add(croissant.getId(), 1, null);
        cart.add(croissant.getId(), 2, null);

        assertEquals(1, cart.snapshot().size(), "one line, not two");
        assertEquals(3, cart.count());
    }

    @Test
    void anEmptyCartSaysSoAndCannotCheckOut() {
        navigate(CartView.class);

        assertTrue(find(Paragraph.class).all().stream()
                .anyMatch(paragraph -> paragraph.getText().contains("empty")));
        var checkout = find(Button.class).withText("Checkout").single();
        assertFalse(checkout.isEnabled(), "there is nothing to check out");
    }

    @Test
    void aLineWhoseProductWentAwayBlocksCheckout() {
        var product = products.findBySlug("empanada").orElseThrow();
        navigate(CartView.class);
        cart().add(product.getId(), 1, null);
        product.setAvailable(false);
        products.saveAndFlush(product);
        try {
            // Navigating to the route you are already on does nothing, so go
            // away and come back to get a freshly built cart page.
            navigate(StorefrontView.class);
            navigate(CartView.class);
            var checkout = find(Button.class).withText("Checkout").single();
            assertFalse(checkout.isEnabled(), "an unavailable line blocks the checkout");
        } finally {
            var restored = products.findBySlug("empanada").orElseThrow();
            restored.setAvailable(true);
            products.saveAndFlush(restored);
        }
    }
}
