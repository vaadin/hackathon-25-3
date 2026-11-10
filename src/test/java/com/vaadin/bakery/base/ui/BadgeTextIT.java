package com.vaadin.bakery.base.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.BrowserIT;
import com.vaadin.testbench.BrowserTest;
import java.util.List;

/**
 * A badge that says something has to look like it says something.
 *
 * This exists because of a bug no other tier can see. `vaadin-badge` keeps its
 * content hidden until it has `has-content`, and it decides that from slot
 * changes alone: Flow appends the text as an empty text node and fills it in
 * afterwards, which changes no slot assignment, so the attribute never arrives
 * and every badge in the application is drawn as a small empty pill. The text
 * is in the DOM the whole time, which is exactly why nothing caught it. A
 * browserless test reading `getText()` passes, and so does the contrast
 * measurement in {@link AuraDarkModeIT}, because a computed colour does not
 * care whether the box is displayed. Upstream has the same blind spot: the only
 * integration test for the Flow component asserts `BadgeElement.getText()`, and
 * that method reads the light DOM text nodes with a script rather than asking
 * the browser what is rendered, so it passes on a badge that shows nothing.
 *
 * Upstream this is web-components#12663, a regression from #12399 and fixed by
 * #12664 within hours of being reported. The fix is merged and unreleased: the
 * workaround in `styles/base/components.css` is here until this application is
 * past 25.3.0-beta2. What this asserts is the outcome rather than the
 * workaround, so it stays true on the day the component sets the attribute
 * itself, and it is what says the CSS rule can safely go.
 */
class BadgeTextIT extends BrowserIT {

    /**
     * The allergen chips on a product card, which is the densest place badges
     * appear, measured with the height of the same badge as the yardstick. An
     * empty pill is square, near enough: the component floors it at one line
     * box plus its borders in both directions. A chip carrying a word is
     * several times wider than it is tall, so the ratio separates the two
     * without depending on a font, a theme or a viewport.
     */
    private static final String WIDEST_CHIP = """
            var chips = [...document.querySelectorAll('.product-card__allergens vaadin-badge')]
              .filter(function (b) { return b.textContent.trim().length > 2; });
            if (!chips.length) { return null; }
            var box = chips.map(function (b) { return b.getBoundingClientRect(); })
              .sort(function (a, b) { return b.width - a.width; })[0];
            return [Math.round(box.width), Math.round(box.height)];
            """;

    @BrowserTest
    void aBadgeWithTextIsWiderThanAnEmptyPill() {
        open("/shop");
        waitUntil("return document.querySelectorAll('.product-card__allergens vaadin-badge').length > 0;");

        @SuppressWarnings("unchecked")
        var box = (List<Long>) settled(() -> script("return (function () {" + WIDEST_CHIP + "})();"));
        assertTrue(box != null && box.size() == 2,
                "the catalogue has allergen chips to measure, at " + whereAmI());

        long width = box.get(0);
        long height = box.get(1);
        assertTrue(width > height * 2,
                "an allergen chip carrying a word is wider than it is tall, it measured "
                        + width + " by " + height + ", at " + whereAmI());
    }

    /**
     * The same badge, asked the question the other way round. The width is what
     * a person sees; this is the mechanism behind it, and it names the attribute
     * so a failure says which half of the pair broke.
     */
    @BrowserTest
    void theContentOfABadgeWithTextIsDisplayed() {
        open("/shop");
        waitUntil("return document.querySelectorAll('.product-card__allergens vaadin-badge').length > 0;");

        var display = script("""
                var chip = [...document.querySelectorAll('.product-card__allergens vaadin-badge')]
                  .find(function (b) { return b.textContent.trim().length > 2; });
                return getComputedStyle(chip.shadowRoot.querySelector('[part="content"]')).display;
                """);
        assertTrue(!"none".equals(display),
                "the content part of a badge with text is displayed, it was " + display
                        + ", at " + whereAmI());
    }
}
