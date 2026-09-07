package com.vaadin.bakery.base;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Which sources the codebase rules do not apply to.
 *
 * Copilot's All Components tool writes a view into the source tree, and writes
 * it again every time it is regenerated, saying so in its own text: keep your
 * customizations somewhere else, because this file is replaced. Translating its
 * strings or rewriting its attach handler would be undone by the next click, and
 * would put developer tooling text into the application's translation bundles
 * for no reader's benefit.
 *
 * The match is deliberately narrow. A file qualifies only if it declares a route
 * under {@code __copilot/} and keeps that route out of the startup registry,
 * which together are what make it a tool rather than a screen of this
 * application. An ordinary view cannot drift into this exemption by accident.
 */
final class GeneratedSources {

    private GeneratedSources() {
    }

    static boolean isCopilotTool(Path file) throws IOException {
        var source = Files.readString(file);
        return source.contains("\"__copilot/") && source.contains("registerAtStartup = false");
    }
}
