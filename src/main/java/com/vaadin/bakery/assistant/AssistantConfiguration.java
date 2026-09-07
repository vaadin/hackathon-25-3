package com.vaadin.bakery.assistant;

import com.vaadin.flow.component.ai.provider.LLMProvider;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

/**
 * Whether there is an assistant at all.
 *
 * There is one provider and it is the real one. Built with the {@code ai} Maven
 * profile and a key, the application talks to OpenAI; without either, the
 * assistant is off and every surface that offers one says so, in red, where
 * somebody can see it.
 *
 * Nothing is stubbed and nothing is replayed. An application that answers a
 * question from a recording is not demonstrating that it can answer questions,
 * and the failure it hides is the exact one worth catching: a panel that has
 * quietly fallen back and looks, to anyone watching a demo, like a working
 * assistant.
 *
 * The live half arrives as an {@link LLMProviderFactory} rather than as a
 * provider, and this class never names a Spring AI type. That is deliberate:
 * without the profile there is no Spring AI on the classpath, and a
 * configuration class that mentioned one would not load at all.
 */
@Configuration
public class AssistantConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AssistantConfiguration.class);

    /** The one Spring AI type whose presence says the {@code ai} profile was built. */
    private static final String CHAT_MODEL = "org.springframework.ai.chat.model.ChatModel";

    @Bean
    public AssistantStatus assistantStatus(ObjectProvider<LLMProviderFactory> factories,
            Environment environment) {
        var live = factories.getIfAvailable();
        if (live == null) {
            var reason = whyNot(environment);
            LOG.warn("The assistant is disabled: {}. Until then the phone order, the order board "
                    + "and the dashboard show their assistant panels as unavailable, and "
                    + "everything else on those screens works as usual.", reason.explanation());
            return new AssistantStatus(null, null, reason);
        }

        // An orchestrator claims its provider exclusively, so each one needs an
        // instance of its own. A live provider is per orchestrator for a second
        // reason, that it carries the conversation memory of whoever is typing
        // into that one screen.
        var name = live.newProvider().getClass().getSimpleName();
        LOG.info("Assistant provider: {}", name);
        return new AssistantStatus(name, live::newProvider, Reason.NONE);
    }

    /**
     * Which of the two switches is off.
     *
     * Worth the twenty lines because the single message this replaces named
     * both of them at once, and the first person to read it had the key
     * exported and spent the time looking at the key. The provider is compiled
     * only under the {@code ai} Maven profile, so the default build, and the
     * dev loop with it, has no Spring AI on the classpath at all: that is a
     * different thing from a missing key and it deserves a different sentence.
     */
    private Reason whyNot(Environment environment) {
        if (!ClassUtils.isPresent(CHAT_MODEL, getClass().getClassLoader())) {
            return Reason.NO_AI_PROFILE;
        }
        var key = environment.getProperty("spring.ai.openai.api-key", "");
        return key.isBlank() ? Reason.NO_KEY : Reason.DISABLED;
    }

    /** Why there is no assistant, in a form a screen can render. */
    public enum Reason {

        /** There is one. */
        NONE("assistant.off.why.none"),
        /** Built without the {@code ai} profile, so no provider was compiled. */
        NO_AI_PROFILE("assistant.off.why.profile"),
        /** Built with the profile and given no key. */
        NO_KEY("assistant.off.why.key"),
        /** Everything is present and something else turned it off, such as the test profile. */
        DISABLED("assistant.off.why.other");

        private final String translationKey;

        Reason(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }

        /** The same thing for the log, which has no translations. */
        String explanation() {
            return switch (this) {
                case NO_AI_PROFILE -> "this build has no language model provider in it. Build with"
                        + " -Pai, which is what compiles it. An OPENAI_API_KEY alone does nothing"
                        + " here, because there is nothing to give it to";
                case NO_KEY -> "the ai profile is built but OPENAI_API_KEY is empty. Export it"
                        + " rather than only setting it, or the JVM never sees it";
                case DISABLED -> "the provider is on the classpath and configured, and something"
                        + " else declined to build one";
                case NONE -> "it is not disabled";
            };
        }
    }

    /** What the about page and every assistant panel ask before offering one. */
    public record AssistantStatus(String name, Supplier<LLMProvider> perOrchestrator, Reason reason) {

        public boolean isAvailable() {
            return perOrchestrator != null;
        }

        /**
         * A provider for one orchestrator. Never hand the same instance to two:
         * the second is refused and its screen silently falls back.
         */
        public LLMProvider newSession() {
            if (!isAvailable()) {
                throw new IllegalStateException("There is no language model provider. "
                        + "Ask isAvailable() before building an orchestrator.");
            }
            return perOrchestrator.get();
        }

        /** The provider's name, or null when there is none. */
        public String describe() {
            return name;
        }
    }
}
