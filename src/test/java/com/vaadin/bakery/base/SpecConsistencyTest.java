package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * POL-01. Every feature document ends in a table of test cases, and the last
 * column names the class that proves each one. This walks those names and fails
 * when one does not exist.
 *
 * It is the single mechanism that keeps specifications and code from drifting
 * apart: a specification can promise anything, but it cannot promise a test
 * class that was never written.
 */
class SpecConsistencyTest {

    private static final Path FEATURES = Path.of("specs/features");
    private static final Path TESTS = Path.of("src/test/java");
    private static final Pattern VERIFIED_BY = Pattern.compile("`([A-Z][A-Za-z0-9]*(?:Test|IT))`");

    /** Browser tests. They need a licence and a display, so they are a separate run. */
    private static final Set<String> BROWSER_TIER = Set.of(
            "AuraDarkModeIT", "ClipboardPasteIT",             "DashboardLayoutIT", "DatePickerMetadataIT", "DiagnosticsLayoutIT", "GridProEditIT",
            "InvoicePrintIT", "KitchenBoardPushIT", "KitchenSummaryOverlayIT",
            "PageTitleIT", "PwaInstallIT", "SmokeIT", "UploadDropZoneIT", "UserAvatarIT");

    /**
     * Honest gaps. Each of these is a test the specifications ask for that has
     * not been written yet, listed here rather than quietly renamed to something
     * that happens to exist. Deleting a line from this set is how the gap gets
     * closed.
     */
    private static final Set<String> NOT_WRITTEN_YET = Set.of(
            "ConcurrentEditBrowserlessTest",
            "InvoiceLocaleBrowserlessTest",
            "LocaleSwitchDerivedTextBrowserlessTest",
            "LocaleSwitchTransientTextBrowserlessTest",
            "OrderDetailsBandIT",
            "PageTitleLocaleBrowserlessTest",
            "ResponsiveBoardBrowserlessTest",
            "TrackingLiveBrowserlessTest",
            "UserAvatarBrowserlessTest"
    );

    private Set<String> namedTestClasses() throws IOException {
        var names = new LinkedHashSet<String>();
        try (Stream<Path> documents = Files.list(FEATURES)) {
            for (Path document : documents.filter(path -> path.toString().endsWith(".md")).toList()) {
                var matcher = VERIFIED_BY.matcher(Files.readString(document));
                while (matcher.find()) {
                    names.add(matcher.group(1));
                }
            }
        }
        return names;
    }

    private Set<String> existingTestClasses() throws IOException {
        try (Stream<Path> files = Files.walk(TESTS)) {
            var names = new LinkedHashSet<String>();
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> names.add(path.getFileName().toString().replace(".java", "")));
            return names;
        }
    }

    @Test
    void theFeatureDocumentsNameRealTestClasses() throws IOException {
        var named = namedTestClasses();
        var existing = existingTestClasses();

        assertFalse(named.isEmpty(), "the feature documents should name their tests");

        List<String> missing = new ArrayList<>();
        for (String name : named) {
            if (!existing.contains(name) && !BROWSER_TIER.contains(name) && !NOT_WRITTEN_YET.contains(name)) {
                missing.add(name);
            }
        }
        assertTrue(missing.isEmpty(), """
                These test classes are promised by a feature document and do not exist:
                %s
                Either write them, or change the document. A specification that names a
                test nobody wrote is how specifications stop being true."""
                .formatted(String.join("\n", missing)));
    }

    @Test
    void theBrowserTierOnlyContainsBrowserTests() {
        var wrong = BROWSER_TIER.stream().filter(name -> !name.endsWith("IT")).toList();

        assertTrue(wrong.isEmpty(), """
                Only browser tests belong in the browser tier, because that is the run that
                needs a licence and a display. These do not: %s""".formatted(wrong));
    }

    @Test
    void theGapListIsStillHonest() {
        // If one of these has since been written, remove it from the set: a gap
        // list that lists things that exist is worse than no gap list.
        var stale = new ArrayList<String>();
        try {
            var existing = existingTestClasses();
            NOT_WRITTEN_YET.stream().filter(existing::contains).forEach(stale::add);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
        assertTrue(stale.isEmpty(), "These are listed as missing but exist: " + stale);
    }
}
