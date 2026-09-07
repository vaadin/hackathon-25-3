package com.vaadin.bakery.base;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Safelist;

/**
 * One safelist for the whole application. Anything a customer wrote, and
 * anything an administrator wrote that ends up as markup, goes through here
 * before it reaches a browser.
 */
public final class SafeHtml {

    private static final Safelist SAFELIST = Safelist.basic()
            .addTags("h1", "h2", "h3", "h4", "p", "ul", "ol", "li", "strong", "em", "code", "pre", "blockquote")
            .addAttributes("a", "href", "title")
            .addProtocols("a", "href", "http", "https", "mailto", "tel");

    private SafeHtml() {
    }

    /** Untrusted markup, cleaned. */
    public static String clean(String html) {
        return html == null ? "" : Jsoup.clean(html, SAFELIST);
    }

    /**
     * Untrusted text that must never become markup. It is escaped rather than
     * stripped: a customer who types "write &lt;b&gt;Ana&lt;/b&gt; on the cake"
     * meant those characters, and silently deleting them loses the order.
     */
    public static String text(String value) {
        return value == null ? "" : Entities.escape(value);
    }
}
