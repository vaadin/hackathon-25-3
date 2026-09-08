package com.vaadin.bakery.base;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

    /**
     * Cleaning must not reformat. Jsoup pretty prints by default, which
     * normalises whitespace, and the whole structure of a markdown document is
     * whitespace: every product description arrived at the renderer as one long
     * line, so its heading swallowed its own body text and its bullet list, and
     * the product panel showed the lot as a single heading.
     */
    private static final Document.OutputSettings VERBATIM =
            new Document.OutputSettings().prettyPrint(false);

    private SafeHtml() {
    }

    /** Untrusted markup, cleaned, with the source's own line structure intact. */
    public static String clean(String html) {
        return html == null ? "" : Jsoup.clean(html, "", SAFELIST, VERBATIM);
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
