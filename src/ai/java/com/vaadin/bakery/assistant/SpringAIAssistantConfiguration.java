package com.vaadin.bakery.assistant;

import com.vaadin.flow.component.ai.provider.SpringAILLMProvider;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * The live assistant, when there is one.
 *
 * Nothing here is written by hand: {@code SpringAILLMProvider} ships inside
 * {@code vaadin-ai-core-flow}, which is free, and Spring AI supplies the
 * {@link ChatModel}. All this class does is decide when to build one.
 *
 * This file is compiled only under the {@code ai} Maven profile, which is why it
 * sits in {@code src/ai/java} rather than beside the rest of the package. The
 * default build has no Spring AI on the classpath at all, so a class that
 * imports one could not be compiled there, let alone loaded.
 *
 * Two conditions on top of that. The expression checks that a key was actually
 * given, so building with the profile and no key still boots on the mock instead
 * of failing at startup. And tests never go live, whatever is on the classpath,
 * because a test that talks to OpenAI is not a test.
 */
@Configuration
@Profile("!test")
@ConditionalOnExpression("!'${spring.ai.openai.api-key:}'.isEmpty()")
public class SpringAIAssistantConfiguration {

    @Bean
    public LLMProviderFactory springAILLMProviderFactory(ChatModel chatModel) {
        // A new provider per orchestrator, over the one shared model. The model
        // is stateless, the provider is not: it holds the chat memory.
        return () -> new SpringAILLMProvider(chatModel);
    }
}
