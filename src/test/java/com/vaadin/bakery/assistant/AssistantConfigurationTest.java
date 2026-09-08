package com.vaadin.bakery.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
     * Two builds reach this test and they have different right answers. Without
     * the {@code ai} profile no provider is compiled at all, so the honest
     * answer is "this build has none". With the profile, which activates on the
     * key being present in the environment, Spring AI is on the classpath and
     * the test profile is what holds the assistant back, so the answer is
     * "something else turned it off". Both are named reasons and neither is
     * {@code NONE}, which is the criterion: a screen says which switch, and it
     * never says the assistant is on when it is not.
     *
     * Asserting one of the two would make this test pass or fail on whether
     * whoever ran it had a key exported, which is a property of the machine and
     * not of the application.
     */
    @Test
    void theAssistantSaysWhichSwitchIsOff() {
        assertNotEquals(Reason.NONE, assistant.reason(),
                "the assistant is off, so there is a switch to name");
        assertTrue(assistant.reason() == Reason.NO_AI_PROFILE || assistant.reason() == Reason.DISABLED,
                "either this build has no provider, or the test profile is holding it: " + assistant.reason());
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
