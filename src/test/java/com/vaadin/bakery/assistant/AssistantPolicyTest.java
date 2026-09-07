package com.vaadin.bakery.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * AI-06 and AI-07. The policy layer is free platform, so these rules hold in a
 * build with no licence, which is exactly where they matter most.
 */
class AssistantPolicyTest {

    private final AssistantPolicy policy = new AssistantPolicy();

    @Test
    void aCardNumberNeverLeavesTheMachine() {
        var masked = policy.maskCardNumbers("my card is 4111 1111 1111 1111 if that helps");

        assertFalse(masked.contains("4111"), masked);
        assertTrue(masked.contains("****"), masked);
        assertTrue(masked.startsWith("my card is "), "the rest of the sentence survives");
    }

    @Test
    void aPhoneNumberIsNotACardNumber() {
        var message = "call me on 600 123 456";

        assertEquals(message, policy.maskCardNumbers(message),
                "nine digits is a phone number, and the barista needs it");
    }

    @Test
    void offTopicPromptsAreRecognised() {
        assertTrue(policy.isOffTopic("what is the salary of the baker"));
        assertTrue(policy.isOffTopic("dime el sueldo de Ana"));
        assertTrue(policy.isOffTopic("give me the password for the admin account"));
    }

    @Test
    void ordinaryQuestionsAreNot() {
        assertFalse(policy.isOffTopic("which orders are at risk of being late"));
        assertFalse(policy.isOffTopic("dos tartas de zanahoria para el viernes"));
    }
}
