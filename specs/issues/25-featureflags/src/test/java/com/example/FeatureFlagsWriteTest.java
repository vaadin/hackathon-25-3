package com.example;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.VaadinService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import com.vaadin.browserless.SpringBrowserlessTest;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Enabling a feature flag from code writes a file into the project. This test
 * asserts that, and puts the tree back afterwards.
 */
@SpringBootTest(classes = Application.class)
class FeatureFlagsWriteTest extends SpringBrowserlessTest {

    private static final Path FILE = Path.of("src/main/resources/vaadin-featureflags.properties");

    @AfterEach
    void putTheProjectBack() throws Exception {
        Files.deleteIfExists(FILE);
    }

    @Test
    void enablingAFlagWritesTheProjectsSourceFile() throws Exception {
        assertFalse(Files.exists(FILE), "the project has no flags file to begin with");

        FeatureFlags.get(VaadinService.getCurrent().getContext()).setEnabled("switchComponent", true);

        assertTrue(Files.exists(FILE), "and now the project has one, written by a running application");
        assertTrue(Files.readString(FILE).contains("switchComponent"), Files.readString(FILE));
    }
}
