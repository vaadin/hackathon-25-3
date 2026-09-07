package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.html.Table;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * FND-16 and FND-17. The application says what it can do before anybody asks.
 *
 * A preview flag that is off does not break the build and does not throw. It
 * removes a component, and the first sign is a screen missing half of itself in
 * front of somebody. So the flags are checked at startup, named with the file
 * that sets them, and the about page paints the same list from the same source.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class StartupReportTest extends SpringBrowserlessTest {

    private FeatureFlags flags() {
        return FeatureFlags.get(VaadinService.getCurrent().getContext());
    }

    /**
     * {@code FeatureFlags.setEnabled} does not only flip a boolean: it writes
     * {@code vaadin-featureflags.properties} in the source tree, comments and
     * all. A test that toggles a flag therefore edits the repository, and the
     * first run of this class committed a rewritten file without anybody
     * noticing. So the file is put back byte for byte, not merely the flags.
     * The row is in {@code specs/FEEDBACK-25.3.md}.
     */
    private static final Path FLAG_FILE = Path.of("src/main/resources/vaadin-featureflags.properties");
    private static String flagFileBefore;

    @BeforeEach
    void rememberTheFlagFile() throws IOException {
        flagFileBefore = Files.readString(FLAG_FILE);
    }

    @AfterEach
    void putTheFlagsBack() throws IOException {
        RequiredFeatures.ALL.forEach(feature -> flags().setEnabled(feature.id(), true));
        Files.writeString(FLAG_FILE, flagFileBefore);
    }

    @Test
    void everyFlagThisApplicationDependsOnIsOnInThisBuild() {
        var missing = StartupReport.missing(flags());

        assertTrue(missing.isEmpty(), "nothing this application needs is switched off: " + missing);
    }

    @Test
    void aFlagThatIsOffIsReportedByName() {
        flags().setEnabled("switchComponent", false);

        var missing = StartupReport.missing(flags());

        assertEquals(1, missing.size(), "exactly the one that was turned off, got " + missing);
        assertEquals("switchComponent", missing.getFirst().id());
        assertFalse(missing.getFirst().usedFor().isBlank(), "and it says what it was needed for");
    }

    /**
     * The other half of the same claim: the page and the log read one list, so
     * they cannot drift apart, and turning a flag off changes what the page
     * says rather than only what the log said once at boot.
     */
    @Test
    void turningAFlagOffChangesWhatTheAboutPageReports() {
        signIn();
        navigate("about", com.vaadin.bakery.base.ui.AboutView.class);
        assertFalse(flagTableText().toLowerCase().contains("off"), "everything is on to begin with");

        flags().setEnabled("aiComponents", false);
        // Away and back: navigating to the route you are already on is a no
        // operation, so the view is not rebuilt and reads its old answer. The
        // row is in specs/FEEDBACK-PLATFORM.md.
        navigate("hours", com.vaadin.bakery.base.ui.OpeningHoursView.class);
        navigate("about", com.vaadin.bakery.base.ui.AboutView.class);

        assertTrue(flagTableText().toLowerCase().contains("off"), "and the page says so: " + flagTableText());
    }

    /** The flag table, as the words a reader would see in it. */
    private String flagTableText() {
        return find(Table.class).all().stream()
                .map(table -> table.getElement().getTextRecursively())
                .filter(text -> text.contains("breadcrumbsComponent"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the about page has no flag table"));
    }

    private void signIn() {
        com.vaadin.bakery.TestLogin.asAdmin();
    }
}
