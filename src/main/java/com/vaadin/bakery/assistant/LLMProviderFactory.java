package com.vaadin.bakery.assistant;

import com.vaadin.flow.component.ai.provider.LLMProvider;

/**
 * A source of fresh providers.
 *
 * An orchestrator claims its provider exclusively, so nothing may hand the same
 * instance to two screens. A real provider also carries the conversation memory
 * of whoever is typing, which is a second reason never to share one.
 *
 * This interface exists so that {@link AssistantConfiguration} can discover a
 * live provider without importing anything from Spring AI: the default build has
 * no Spring AI on the classpath at all.
 */
@FunctionalInterface
public interface LLMProviderFactory {

    /** A provider for one orchestrator, with its own memory. */
    LLMProvider newProvider();
}
