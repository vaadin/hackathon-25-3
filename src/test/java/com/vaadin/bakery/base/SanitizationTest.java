package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * SHOP-06 and SHOP-07. One safelist, and it is the only thing standing between
 * a customer's note and everybody else's browser.
 */
class SanitizationTest {

    @Test
    void scriptsNeverSurvive() {
        var cleaned = SafeHtml.clean("<p>Hello</p><script>alert('x')</script>");
        assertTrue(cleaned.contains("Hello"));
        assertFalse(cleaned.toLowerCase().contains("script"));
    }

    @Test
    void javascriptUrlsAreNotLinks() {
        var cleaned = SafeHtml.clean("<a href=\"javascript:alert('x')\">click</a>");
        assertFalse(cleaned.contains("javascript:"), cleaned);
        // jsoup keeps the element and drops the attribute it cannot trust, so
        // what is left is text in an anchor with nowhere to go.
        assertFalse(cleaned.contains("href"), "the unsafe href is gone: " + cleaned);
    }

    @Test
    void ordinaryLinksSurvive() {
        var cleaned = SafeHtml.clean("<a href=\"https://vaadin.com\">Vaadin</a>");
        assertTrue(cleaned.contains("https://vaadin.com"));
    }

    @Test
    void customerTextNeverBecomesMarkup() {
        assertEquals("Please write &lt;b&gt;Ana&lt;/b&gt; on the cake",
                SafeHtml.text("Please write <b>Ana</b> on the cake"));
    }

    @Test
    void onEventAttributesAreStripped() {
        var cleaned = SafeHtml.clean("<p onclick=\"steal()\">text</p>");
        assertFalse(cleaned.contains("onclick"));
    }
}
