package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * FND-06. Every user visible string comes from a translation bundle. This scan
 * is a heuristic, not a proof, but it catches the common way the rule is broken:
 * a literal handed to a component that renders it.
 *
 * Sources that Copilot generates and regenerates are out of scope, see
 * {@link GeneratedSources}.
 */
class NoHardcodedStringsTest {

    private static final List<Pattern> SUSPECTS = List.of(
            Pattern.compile("new (Span|H1|H2|H3|H4|Paragraph|Button|NativeLabel|Text)\\(\\s*\"([^\"]{2,})\""),
            Pattern.compile("\\.(setText|setLabel|setPlaceholder|setHelperText|setTitle|setAriaLabel|setErrorMessage|setTooltipText)\\(\\s*\"([^\"]{2,})\""),
            Pattern.compile("Notification\\.show\\(\\s*\"([^\"]{2,})\""));

    @Test
    void noUserVisibleLiteralsInSources() throws IOException {
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
                    if (line.trim().startsWith("//") || line.trim().startsWith("*")) {
                        continue;
                    }
                    for (Pattern suspect : SUSPECTS) {
                        var matcher = suspect.matcher(line);
                        if (matcher.find()) {
                            offenders.add(file + ":" + (i + 1) + " " + line.trim());
                        }
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "User visible string literals outside the bundles:\n"
                + String.join("\n", offenders));
        // Printed rather than asserted away: an exemption nobody can see is an
        // exemption that grows.
        skipped.forEach(file -> System.out.println("Copilot generated, not scanned: " + file));
    }
}
