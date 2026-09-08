package com.vaadin.bakery.assistant;

import com.vaadin.flow.component.ai.common.ChatMessage;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * What each assistant has already said, for as long as the person is logged in.
 *
 * An orchestrator keeps its own history and dies with the view that built it,
 * so leaving the counter order screen and coming back used to lose the whole
 * exchange: the form still held the values, and the account of where they came
 * from was gone. This bean is one map, keyed by the prompt the panel runs, and
 * every panel hands its history back at the end of each turn and asks for it
 * again when it is rebuilt.
 *
 * Text only, deliberately. Attachments correlate to a message by its id and the
 * orchestrator would take them back happily, but a photograph of a note is up
 * to eight megabytes and its job is finished the moment the model has read it,
 * so keeping one per turn in the session for the sake of a thumbnail is a
 * trade nobody asked for. A restored transcript therefore shows the words of a
 * message whose picture is no longer there, which is why the message that
 * carried one says so in its own text.
 */
@Component
@VaadinSessionScope
public class AssistantHistory {

    private final Map<String, List<ChatMessage>> byPanel = new ConcurrentHashMap<>();

    /** What that panel said last time, or nothing on a first visit. */
    public List<ChatMessage> of(String panel) {
        return byPanel.getOrDefault(panel, List.of());
    }

    /** Called at the end of a turn, with the orchestrator's own snapshot. */
    public void keep(String panel, List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return;
        }
        byPanel.put(panel, List.copyOf(history));
    }

    /** For a panel that wants to start again, and for the tests. */
    public void forget(String panel) {
        byPanel.remove(panel);
    }
}
