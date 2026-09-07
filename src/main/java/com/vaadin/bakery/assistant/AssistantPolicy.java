package com.vaadin.bakery.assistant;

import com.vaadin.flow.component.ai.orchestrator.RequestInterceptor;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * What the assistant is allowed to see and to be asked.
 *
 * The interceptor runs before the message list, the history and the request, so
 * this is the one place where a card number is removed and an off topic prompt
 * is stopped. It is free platform, which means the policy still applies in a
 * build with no commercial licence.
 */
@Component
public class AssistantPolicy {

    private static final Logger LOG = LoggerFactory.getLogger(AssistantPolicy.class);

    /** Thirteen to nineteen digits, in the usual groupings people type. */
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");

    private static final List<String> OFF_TOPIC = List.of("salary", "salaries", "sueldo", "nomina",
            "password", "contrasena", "home address of", "personal data of");

    private int masked;
    private int rejected;

    public RequestInterceptor interceptor() {
        return event -> {
            var message = event.getUserMessage();
            if (message == null) {
                return;
            }
            var lower = message.toLowerCase();

            if (OFF_TOPIC.stream().anyMatch(lower::contains)) {
                rejected++;
                // reject with a message tells the user why, reject() alone is silent.
                event.reject("I can only help with orders, products and what is on your screen.");
                return;
            }

            var cleaned = CARD.matcher(message).replaceAll("**** **** **** ****");
            if (!cleaned.equals(message)) {
                masked++;
                LOG.info("Masked something card shaped before it left the machine");
                event.setUserMessage(cleaned);
            }
        };
    }

    public int maskedCount() {
        return masked;
    }

    public int rejectedCount() {
        return rejected;
    }

    /** Exposed for the tests, which need to assert on the transformation itself. */
    public String maskCardNumbers(String message) {
        return message == null ? null : CARD.matcher(message).replaceAll("**** **** **** ****");
    }

    public boolean isOffTopic(String message) {
        return message != null && OFF_TOPIC.stream().anyMatch(message.toLowerCase()::contains);
    }
}
