package com.vaadin.bakery.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.assistant.AssistantConfiguration.AssistantStatus;
import com.vaadin.flow.component.ai.common.AIAttachment;
import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.ai.provider.ResponseMetadata;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * AI-01, the half no mock can prove: that the wiring reaches OpenAI.
 *
 * This is the only test in the suite that leaves the machine, so it is tagged
 * and excluded by default. Run it with:
 *
 * <pre>
 * ./mvnw test -Pai -Dtest=LiveAssistantTest -Dsurefire.excludedGroups=
 * </pre>
 *
 * It is what tells the difference between "the assistant works" and "the mock
 * works", which is the failure this whole class exists to catch: the panel
 * falling back silently and looking, to anyone watching a demo, exactly like a
 * working assistant.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("live-ai")
@Tag("live-ai")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class LiveAssistantTest {

    @Autowired
    private AssistantStatus assistant;

    @Test
    void theProviderIsTheRealOne() {
        assertTrue(assistant.isAvailable(), "expected a live provider, the application reports " + assistant.describe());
        assertEquals("SpringAILLMProvider", assistant.describe());
    }

    @Test
    void everyOrchestratorStillGetsItsOwnProvider() {
        // The live path used to hand out one shared bean, which left the second
        // screen on the fallback with nothing said about why.
        assertNotSame(assistant.newSession(), assistant.newSession());
    }

    @Test
    void theModelAnswersAndReportsWhatTheTurnCost() {
        var metadata = new AtomicReference<ResponseMetadata>();
        // Something with one right answer, not something the model has to be
        // obedient about. Asked for a word it was told to repeat, gpt-4o-mini
        // answered a different word and the test failed for the wrong reason.
        var answer = String.join("", assistant.newSession()
                .stream(ask("How much is 17 plus 25? Reply with the number and nothing else.", metadata::set))
                .collectList()
                .block(Duration.ofSeconds(60)));

        assertTrue(answer.contains("42"), "the model answered: " + answer);
        assertNotNull(metadata.get(), "the turn meter needs the metadata the provider reports");
        assertTrue(metadata.get().tokenUsage().totalTokens() > 0, "and a turn that cost nothing did not happen");
    }

    private LLMProvider.LLMRequest ask(String message, Consumer<ResponseMetadata> metadataSink) {
        return new LLMProvider.LLMRequest() {
            @Override
            public String userMessage() {
                return message;
            }

            @Override
            public List<AIAttachment> attachments() {
                return List.of();
            }

            @Override
            public String systemPrompt() {
                return "You are being tested. Follow the instruction exactly.";
            }

            @Override
            public Object[] tools() {
                return new Object[0];
            }

            @Override
            public List<LLMProvider.ToolSpec> explicitTools() {
                return List.of();
            }

            @Override
            public Consumer<ResponseMetadata> metadataSink() {
                return metadataSink;
            }
        };
    }
}
