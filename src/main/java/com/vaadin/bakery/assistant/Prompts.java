package com.vaadin.bakery.assistant;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

/**
 * System prompts, one file per surface, under {@code ai/prompts}.
 *
 * They live in resources rather than in a text block because they are edited
 * far more often than the view around them, they are reviewed by people who do
 * not read Java, and a diff of a prompt should not be buried in a diff of a
 * layout. They are not translations: what pins the answer to the reader's
 * language is the instruction inside the prompt, not a bundle.
 */
public final class Prompts {

    private Prompts() {
    }

    public static String of(String name) {
        var resource = new ClassPathResource("ai/prompts/" + name + ".txt");
        try (var stream = resource.getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException("No system prompt at ai/prompts/" + name + ".txt", failure);
        }
    }
}
