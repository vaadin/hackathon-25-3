package com.vaadin.bakery.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.assistant.AssistantConfiguration.AssistantStatus;
import com.vaadin.bakery.assistant.AssistantConfiguration.Reason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * AI-12. With no model, the application still boots, and the assistant is off
 * rather than pretending.
 *
 * The live half of this is {@link LiveAssistantTest}, which is the only place
 * that can prove a provider is handed out at all, because there is only one
 * provider and it is real.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class AssistantConfigurationTest {

    @Autowired
    private AssistantStatus assistant;

    @Test
    void withNoProviderTheApplicationStartsAndTheAssistantIsOff() {
        assertFalse(assistant.isAvailable(), "the test profile never goes live");
        assertNull(assistant.describe(), "and there is no provider to name");
    }

    /**
     * Which switch is off, not just that one is.
     *
     * The default build compiles no provider at all, so the honest answer is
     * "this build has none", and it is a different answer from "the key is
     * missing". Naming both at once is what sent the first reader to look at a
     * key that was already exported.
     */
    @Test
    void theAssistantSaysWhichSwitchIsOff() {
        assertEquals(Reason.NO_AI_PROFILE, assistant.reason(),
                "the default build has no Spring AI on the classpath");
        assertFalse(assistant.reason().translationKey().isBlank(), "and a screen can say so");
    }

    /**
     * Asking for a session when there is none is a programming error, not a
     * silent fallback. A panel that quietly answered from somewhere else is the
     * failure this whole arrangement exists to make impossible.
     */
    @Test
    void askingForASessionWhenThereIsNoneFailsLoudly() {
        var refusal = assertThrows(IllegalStateException.class, assistant::newSession);

        assertFalse(refusal.getMessage().isBlank(), "and it says what to check");
    }
}
