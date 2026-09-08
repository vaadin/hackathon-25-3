package com.vaadin.bakery.assistant.ui;

import com.vaadin.bakery.assistant.AssistantConfiguration.AssistantStatus;
import com.vaadin.bakery.assistant.AssistantHistory;
import com.vaadin.bakery.assistant.AssistantPolicy;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ai.orchestrator.AIController;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A question, and whatever the answer is drawn into.
 *
 * The order board and the dashboard ask the same shape of question and differ
 * only in what the answer looks like, so the orchestrator, the policy hook and
 * the meter are wired once here and the surface supplies its controller and the
 * component that controller renders into.
 *
 * The controller is built lazily, inside the try. Both of these are commercial
 * and both check their licence when they are constructed, so an unlicensed
 * machine has to end up with a panel that says so rather than a view that
 * fails to open.
 */
public class AskPanel extends Div {

    private static final Logger LOG = LoggerFactory.getLogger(AskPanel.class);

    private final TurnMeter meter = new TurnMeter();
    private AIOrchestrator orchestrator;
    private AIController controller;

    /**
     * @param titleKey     the heading over the question box
     * @param promptName   the file under {@code ai/prompts}
     * @param answer       the grid or chart the controller writes into
     * @param controllerFor a factory, so the licence check happens where it can be caught
     */
    public AskPanel(AssistantStatus assistant, AssistantPolicy policy, AssistantHistory history, String titleKey,
            String promptName, Component answer, Supplier<AIController> controllerFor) {
        addClassName("ask-panel");
        add(Translations.bindText(new H3(), titleKey));

        if (!assistant.isAvailable()) {
            add(Assistants.unavailable(assistant), answer, meter);
            return;
        }

        var messages = new MessageList();
        // A model writes markdown whether or not anybody asked it to, and a
        // message list renders text, so its emphasis arrived as asterisks
        // around the words it meant to stress.
        messages.setMarkdown(true);
        var input = new MessageInput();

        try {
            controller = controllerFor.get();
            orchestrator = AIOrchestrator.builder(assistant.newSession(),
                            com.vaadin.bakery.assistant.Prompts.of(promptName))
                    .withInput(input)
                    .withMessageList(messages)
                    // The questions this panel has already answered, so leaving
                    // the board and coming back does not lose the thread.
                    .withHistory(history.of(promptName), java.util.Map.of())
                    .withController(controller)
                    .withRequestInterceptor(policy.interceptor())
                    .withResponseListener(event -> {
                        event.getMetadata().ifPresent(metadata -> meter.record(
                                metadata.tokenUsage() == null ? 0 : metadata.tokenUsage().totalTokens(),
                                metadata.finishReason()));
                        if (event.getError().isEmpty()) {
                            history.keep(promptName, orchestrator.getHistory());
                        }
                    })
                    .withAssistantName(getTranslation("app.name"))
                    .build();
            add(input, messages);
        } catch (RuntimeException unavailable) {
            LOG.info("The {} assistant is not available: {}", promptName, unavailable.getMessage());
            add(Translations.bindText(new Paragraph(), "assistant.unavailable"));
        }

        add(answer, meter);
    }

    /** Whether the controller was built, which is what a test needs to know. */
    public boolean isAvailable() {
        return orchestrator != null;
    }

    AIController controller() {
        return controller;
    }

    TurnMeter meter() {
        return meter;
    }

    /** One turn, driven from code rather than by a person typing. */
    public void ask(String question) {
        orchestrator.prompt(question);
    }
}
