package com.vaadin.bakery.billing.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.textfield.TextField;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The export, from where it really runs.
 *
 * A download arrives as its own request, without the session lock, so this
 * calls the export from a thread that holds nothing: that is the condition the
 * code has to survive, and running it on the test thread proves nothing about
 * it. What it must not do from there is read UI state, which is why the filter
 * is copied into a snapshot when it changes and the export reads that.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class InvoiceExportBrowserlessTest extends SpringBrowserlessTest {

    @BeforeEach
    void signIn() {
        TestLogin.asAdmin();
    }

    private String exportFromAnUnlockedThread(InvoiceListView view) throws Exception {
        return CompletableFuture.supplyAsync(view::csv).get();
    }

    @Test
    void theExportRunsWithoutTheSessionLock() throws Exception {
        navigate(InvoiceListView.class);
        var view = find(InvoiceListView.class).single();

        var csv = exportFromAnUnlockedThread(view);

        assertTrue(csv.startsWith("number,issued,customer,net,vat,gross,status"), "the header is there");
        assertTrue(csv.lines().count() > 1, "and so are the rows: " + csv.lines().count());
    }

    @Test
    void theExportCarriesWhateverTheFilterShows() throws Exception {
        navigate(InvoiceListView.class);
        var view = find(InvoiceListView.class).single();
        long everything = exportFromAnUnlockedThread(view).lines().count();

        // Typing in the filter runs under the lock, which is where the snapshot
        // the export reads is written.
        // A number the dataset really carries, from the first seeded invoice:
        // the numbering is year and sequence, with no prefix.
        var aNumber = view.csv().lines().skip(1).findFirst().orElseThrow().split(",")[0];
        test(find(TextField.class).single()).setValue(aNumber);

        long narrowed = exportFromAnUnlockedThread(view).lines().count();

        assertTrue(narrowed < everything,
                "the filter narrows the export too, got " + narrowed + " of " + everything);
        assertTrue(narrowed > 1, "and it still found something: " + narrowed);
        assertEquals(view.csv(), exportFromAnUnlockedThread(view),
                "and the same bytes whether the lock is held or not");
    }
}
