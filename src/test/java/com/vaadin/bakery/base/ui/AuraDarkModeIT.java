package com.vaadin.bakery.base.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.BrowserIT;
import com.vaadin.testbench.BrowserTest;
import java.util.List;

/**
 * POL-06. Both colour schemes render, and text stays readable in both.
 *
 * The reason this is a browser test and not a browserless one is written into
 * the feedback file: dark mode has two mechanisms and the older one, setting
 * `theme="dark"`, does nothing under Aura. A test asserting the attribute
 * passed while the feature was dead. What cannot be faked is the computed
 * colour of real text on a real background.
 */
class AuraDarkModeIT extends BrowserIT {

    /**
     * Relative luminance and contrast, the WCAG definitions, plus the part
     * everybody forgets: a translucent background has to be composited over
     * what is behind it. The allergen chips are a ten percent wash of their own
     * text colour, so taking the first non transparent background finds a
     * colour identical to the text and reports a ratio of one, on a chip that
     * is perfectly readable. Every layer up to the first opaque one is blended.
     */
    private static final String CONTRAST = """
            function parse(c) {
              var p = (c || '').match(/[\\d.]+/g);
              if (!p) { return null; }
              return [ +p[0], +p[1], +p[2], p.length > 3 ? +p[3] : 1 ];
            }
            function over(top, bottom) {
              var a = top[3];
              return [ top[0] * a + bottom[0] * (1 - a),
                       top[1] * a + bottom[1] * (1 - a),
                       top[2] * a + bottom[2] * (1 - a), 1 ];
            }
            function behind(e) {
              var layers = [];
              for (var n = e; n && n !== document.documentElement; n = n.parentElement) {
                var c = parse(getComputedStyle(n).backgroundColor);
                if (c && c[3] > 0) { layers.push(c); if (c[3] === 1) { break; } }
              }
              var page = parse(getComputedStyle(document.documentElement).backgroundColor)
                      || [255, 255, 255, 1];
              if (page[3] < 1) { page = [255, 255, 255, 1]; }
              var result = page;
              for (var i = layers.length - 1; i >= 0; i--) { result = over(layers[i], result); }
              return result;
            }
            function lum(rgb) {
              var p = rgb.slice(0, 3).map(function (v) {
                v = v / 255;
                return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
              });
              return 0.2126 * p[0] + 0.7152 * p[1] + 0.0722 * p[2];
            }
            function ratio(e) {
              var fg = parse(getComputedStyle(e).color);
              var bg = behind(e);
              if (!fg) { return 21; }
              if (fg[3] < 1) { fg = over(fg, bg); }
              var a = lum(fg), b = lum(bg);
              return Math.round(((Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05)) * 10) / 10;
            }
            """;

    /**
     * A whole number comes back from JavaScript as a Long and a fraction as a
     * Double, in the same list, so both are read as numbers here.
     */
    @SuppressWarnings("unchecked")
    private List<Double> contrasts() {
        List<Number> raw = (List<Number>) script(CONTRAST
                + "return [...document.querySelectorAll('h1, h2, h3, p, span, td')]"
                + ".filter(function (e) { return e.innerText && e.innerText.trim().length > 2"
                + " && e.offsetParent !== null && e.children.length === 0; })"
                + ".slice(0, 40).map(ratio);");
        return raw.stream().map(Number::doubleValue).toList();
    }

    private void setScheme(String scheme) {
        script("document.documentElement.style.colorScheme = '" + scheme + "';");
    }

    private void assertReadable(String where) {
        var measured = contrasts();
        assertTrue(measured.size() >= 5, "there is text to measure on " + where + ", found " + measured.size());
        var worst = measured.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        // 4.5 is the AA threshold for body text. Large headings are allowed 3,
        // and this measures them together, so 3 is the honest floor for a
        // mixed sample and anything under it is a real readability problem.
        assertTrue(worst >= 3.0, "the least readable text on " + where + " was " + worst + " to one");
    }

    @BrowserTest
    void theMainViewsAreReadableInBothColourSchemes() {
        signIn("admin@bakery.test", "admin");

        for (String route : List.of("/shop", "/orders", "/admin/dashboard", "/about")) {
            open(route);
            setScheme("light");
            assertReadable(route + " in light");
            setScheme("dark");
            assertReadable(route + " in dark");
        }
    }

    /** And the scheme really is what the page is painting, not just an attribute. */
    @BrowserTest
    void darkModeChangesWhatIsPainted() {
        signIn("admin@bakery.test", "admin");
        open("/about");

        setScheme("light");
        var light = (String) script("return getComputedStyle(document.body).backgroundColor;");
        setScheme("dark");
        var dark = (String) script("return getComputedStyle(document.body).backgroundColor;");

        assertTrue(!light.equals(dark),
                "the page is painted differently: light was " + light + " and dark was " + dark);
    }
}
