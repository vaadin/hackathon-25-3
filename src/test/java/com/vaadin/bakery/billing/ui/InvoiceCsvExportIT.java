package com.vaadin.bakery.billing.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.BrowserIT;
import com.vaadin.testbench.BrowserTest;
import java.util.Map;
import org.openqa.selenium.By;

/**
 * FIX-02. Exporting really produces a file.
 *
 * The old version built the whole CSV and then showed a notification with its
 * byte count, which from the server looks identical to working. Only the
 * response tells the difference, so this asks for it: the anchor's own URL is
 * fetched from the page and the status, the content type and the first line are
 * read off the answer.
 *
 * Fetching rather than downloading is deliberate. A real download needs a
 * configured directory and a wait on the file system, and it would prove less:
 * what matters is that the server answers with the file.
 */
class InvoiceCsvExportIT extends BrowserIT {

    @BrowserTest
    @SuppressWarnings("unchecked")
    void exportingAsksTheServerForAFileAndGetsOne() {
        signIn("admin@bakery.test", "admin");
        open("/admin/invoices");
        var link = waitFor(By.cssSelector(".invoice-list__export"));

        var href = link.getAttribute("href");
        assertTrue(href != null && !href.isBlank(), "the export is a link with a target, not a click handler");

        var answer = (Map<String, Object>) ((org.openqa.selenium.JavascriptExecutor) getDriver())
                .executeAsyncScript(
                        "var done = arguments[arguments.length - 1];"
                                + "fetch(arguments[0]).then(function (r) {"
                                + "  return r.text().then(function (body) {"
                                + "    done({ status: r.status, type: r.headers.get('content-type') || '',"
                                + "           first: body.split('\\n')[0], lines: body.trim().split('\\n').length });"
                                + "  });"
                                + "}).catch(function (e) { done({ status: -1, type: String(e), first: '', lines: 0 }); });",
                        href);

        assertTrue(Long.valueOf(200L).equals(answer.get("status")),
                "the server answered, got " + answer);
        assertTrue(String.valueOf(answer.get("type")).contains("csv"),
                "with a CSV content type, got " + answer.get("type"));
        assertTrue(String.valueOf(answer.get("first")).startsWith("number,issued,customer"),
                "and the header row the grid promises, got " + answer.get("first"));
        assertTrue(((Long) answer.get("lines")) > 1, "and at least one invoice, got " + answer.get("lines"));
    }
}
