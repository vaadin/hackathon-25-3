package com.vaadin.bakery.billing.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.BrowserIT;
import com.vaadin.testbench.BrowserTest;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.chrome.ChromeDriver;

/**
 * INV-06. The print page, under print media.
 *
 * Print is the deliverable here: there is no PDF library in this project and
 * there does not need to be, so what has to be right is the stylesheet the
 * browser uses when somebody presses print. That is a media query, and a media
 * query is only true in a browser that is emulating the medium.
 */
class InvoicePrintIT extends BrowserIT {

    /**
     * Chrome's own switch for asking what the page looks like on paper.
     *
     * The driver has to be unwrapped first. TestBench hands back a Javassist
     * proxy, so casting it to {@code ChromeDriver} throws, and CDP is the only
     * way to emulate a medium: there is no Selenium API for it.
     */
    private void emulatePrint() {
        var driver = getDriver();
        while (driver instanceof WrapsDriver wrapper) {
            driver = wrapper.getWrappedDriver();
        }
        ((ChromeDriver) driver).executeCdpCommand("Emulation.setEmulatedMedia",
                java.util.Map.of("media", "print"));
    }

    /**
     * The print link as the list builds it, token and all. The route is open to
     * anybody who has the token, exactly like the tracking page, so the token
     * is not optional: without it the page says it cannot find the invoice.
     */
    private String printLink() {
        open("/admin/invoices");
        return (String) script("var a = [...document.querySelectorAll('a')]"
                + ".map(function (e) { return e.getAttribute('href'); })"
                + ".filter(function (h) { return h && h.indexOf('/print?t=') > 0; })[0];"
                + "return a || null;");
    }

    @BrowserTest
    void thePrintPageHidesTheApplicationAndKeepsTheTable() {
        signIn("admin@bakery.test", "admin");
        var link = printLink();
        assertTrue(link != null, "the list offers a print link at " + whereAmI());
        var number = link.substring(link.indexOf('/') + 1, link.indexOf("/print"));

        open("/" + link);
        emulatePrint();

        // No shell. The route says autoLayout = false, and print media must not
        // bring it back through some other stylesheet.
        assertTrue(count("vaadin-app-layout") == 0, "no application shell at " + whereAmI());
        assertTrue(count("vaadin-side-nav") == 0, "and no navigation");

        // Real table markup, which is what a screen reader and a printer both need.
        assertTrue(count("table") >= 1, "the document is a table");
        assertTrue(count("th") >= 3, "with header cells, found " + count("th"));

        var text = (String) script("return document.body.innerText;");
        assertTrue(text.contains(number), "it names the invoice: " + number);
        assertTrue(text.toLowerCase().contains("vat"), "and it summarises VAT");
    }

    /** Black on white, because a bakery prints this on a mono laser printer. */
    @BrowserTest
    void underPrintMediaTheDocumentIsBlackOnWhite() {
        signIn("admin@bakery.test", "admin");
        var link = printLink();
        open("/" + link);
        emulatePrint();

        // The document, not the body. The print rules paint the invoice itself
        // white and black, and leave the page around it alone, because the page
        // around it is not what comes out of the printer.
        var painted = (String) script(
                "var d = document.querySelector('.invoice-print');"
                        + "if (!d) { return 'no document'; }"
                        + "var s = getComputedStyle(d);"
                        + "return s.backgroundColor + ' on ' + s.color;");

        assertTrue(painted.equals("rgb(255, 255, 255) on rgb(0, 0, 0)"),
                "the document is black on white under print media, it was " + painted);
    }
}
