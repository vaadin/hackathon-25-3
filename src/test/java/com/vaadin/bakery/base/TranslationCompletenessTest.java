package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * POL-03. A key that exists in one language and not the other is a screen that
 * shows a raw key to somebody. This compares the two bundles both ways.
 */
class TranslationCompletenessTest {

    private static final Path BUNDLES = Path.of("src/main/resources/vaadin-i18n");

    private Properties load(String name) throws IOException {
        var properties = new Properties();
        try (InputStream stream = Files.newInputStream(BUNDLES.resolve(name))) {
            properties.load(stream);
        }
        return properties;
    }

    @Test
    void bothLanguagesHaveTheSameKeys() throws IOException {
        var english = load("translations_en.properties");
        var spanish = load("translations_es.properties");

        var missingInSpanish = new TreeSet<>(english.stringPropertyNames());
        missingInSpanish.removeAll(spanish.stringPropertyNames());

        var missingInEnglish = new TreeSet<>(spanish.stringPropertyNames());
        missingInEnglish.removeAll(english.stringPropertyNames());

        List<String> problems = new ArrayList<>();
        if (!missingInSpanish.isEmpty()) {
            problems.add("Missing in Spanish: " + String.join(", ", missingInSpanish));
        }
        if (!missingInEnglish.isEmpty()) {
            problems.add("Missing in English: " + String.join(", ", missingInEnglish));
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void theFallbackBundleMatchesEnglish() throws IOException {
        var fallback = load("translations.properties");
        var english = load("translations_en.properties");

        var difference = new TreeSet<>(english.stringPropertyNames());
        difference.removeAll(fallback.stringPropertyNames());

        assertTrue(difference.isEmpty(),
                "The fallback bundle is what a browser with an unknown language gets, so it cannot be "
                        + "smaller than English. Missing: " + difference);
    }

    @Test
    void noTranslationIsEmpty() throws IOException {
        for (String bundle : List.of("translations_en.properties", "translations_es.properties")) {
            var properties = load(bundle);
            var empty = properties.stringPropertyNames().stream()
                    .filter(key -> properties.getProperty(key).isBlank())
                    .sorted()
                    .toList();
            assertTrue(empty.isEmpty(), bundle + " has empty values: " + empty);
        }
    }
}
