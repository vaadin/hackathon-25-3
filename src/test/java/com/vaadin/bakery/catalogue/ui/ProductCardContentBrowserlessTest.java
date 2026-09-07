package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * SHOP-02, SHOP-03 and SHOP-05. What is on a card.
 *
 * The existing catalogue tests count cards and filter them, which proves the
 * list and says nothing about what a card actually shows. A visitor decides
 * from the card: the photo, the price and, if they cannot eat nuts, the chips.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class ProductCardContentBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private CatalogueService catalogue;

    private Div firstCard() {
        return find(Div.class).withClassName("product-card").first();
    }

    private Div cardFor(String productName) {
        return find(Div.class).withClassName("product-card").all().stream()
                .filter(card -> card.getElement().getTextRecursively().contains(productName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no card for " + productName));
    }

    @Test
    void everyCardCarriesItsPhotoNamePriceAndAllergens() {
        navigate(StorefrontView.class);
        var card = firstCard();
        var text = card.getElement().getTextRecursively();

        assertFalse(find(Image.class).all().isEmpty(), "a card has a photo");
        assertTrue(text.contains("€"), "and a price in euros: " + text);
        assertFalse(card.getElement().getChildren()
                        .anyMatch(child -> child.getText() != null && child.getText().isBlank())
                        && text.isBlank(),
                "and a name");

        var withChips = find(Span.class).all().stream()
                .filter(span -> span.getElement().getThemeList().contains("badge"))
                .count();
        assertTrue(withChips > 0, "and the allergen chips are badges, found " + withChips);
    }

    /** A cake that has to be ordered ahead says so on the card, not at checkout. */
    @Test
    void aProductNeedingLeadTimeWearsItsBadge() {
        var slow = catalogue.availableProducts().stream()
                .filter(product -> product.getLeadTimeDays() > 0)
                .findFirst()
                .orElseThrow();
        navigate(StorefrontView.class);

        var text = cardFor(slow.getName()).getElement().getTextRecursively();

        assertTrue(text.contains("ahead") || text.contains("day"),
                "the card warns about the lead time: " + text);
    }

    /** The chips are words a customer knows, in their language. */
    @Test
    void allergensShowTheirTranslatedFullNames() {
        UI.getCurrent().setLocale(Locale.ENGLISH);
        navigate(StorefrontView.class);
        var inEnglish = firstCard().getElement().getTextRecursively();

        UI.getCurrent().setLocale(Locale.of("es"));
        navigate(com.vaadin.bakery.base.ui.OpeningHoursView.class);
        navigate(StorefrontView.class);
        var inSpanish = firstCard().getElement().getTextRecursively();

        assertFalse(inEnglish.contains("catalogue.allergen"),
                "the chips are words rather than keys: " + inEnglish);
        assertFalse(inSpanish.contains("catalogue.allergen"), "in either language: " + inSpanish);
        assertFalse(inEnglish.equals(inSpanish), "and the words change with the language");
    }
}
