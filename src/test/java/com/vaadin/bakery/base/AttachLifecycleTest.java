package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * FND-08. In 25.3 attach scoped setup belongs in whenAttached, which returns the
 * registration to release on detach. Overriding onAttach or onDetach is how
 * subscriptions leak, so the codebase simply does not do it.
 *
 * Sources that Copilot generates and regenerates are out of scope, see
 * {@link GeneratedSources}.
 */
class AttachLifecycleTest {

    @Test
    void noOnAttachOrOnDetachOverrides() throws IOException {
        var root = Path.of("src/main/java/com/vaadin/bakery");
        List<String> offenders = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (GeneratedSources.isCopilotTool(file)) {
                    skipped.add(file.toString());
                    continue;
                }
                var lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    var line = lines.get(i);
                    if (line.contains("void onAttach(") || line.contains("void onDetach(")) {
                        offenders.add(file + ":" + (i + 1));
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "Use whenAttached instead of overriding onAttach or onDetach: "
                + offenders);
        skipped.forEach(file -> System.out.println("Copilot generated, not scanned: " + file));
    }
}
