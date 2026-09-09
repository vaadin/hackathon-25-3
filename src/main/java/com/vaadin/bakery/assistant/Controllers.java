package com.vaadin.bakery.assistant;

import com.vaadin.flow.component.ai.orchestrator.AIController;
import com.vaadin.flow.component.ai.orchestrator.ResponseListener;
import com.vaadin.flow.component.ai.provider.LLMProvider;
import java.util.List;
import java.util.stream.Stream;

/**
 * Putting an application's own tools next to a platform controller.
 *
 * Two things in the orchestrator make this necessary. The tools a provider
 * actually receives as {@code explicitTools()} come from
 * {@code controller.getTools()} and from nowhere else: {@code withTools(...)}
 * fills a separate, opaque {@code Object[]} meant for a provider that knows how
 * to read annotated objects, so an application that implements {@code
 * LLMProvider} itself, as the mock here does, never sees them. And
 * {@code withController} takes one controller, so a surface cannot have both a
 * {@code FormAIController} and a tool of its own.
 *
 * Delegating is the whole of the workaround, and it is small enough to keep.
 * Recorded in {@code specs/FEEDBACK-25.3.md}.
 */
public record Controllers(AIController delegate, List<LLMProvider.ToolSpec> extra) implements AIController {

    public static Controllers of(AIController delegate, LLMProvider.ToolSpec... extra) {
        return new Controllers(delegate, List.of(extra));
    }

    /**
     * A null delegate is a real case and not a defect: the form controller is a
     * commercial component and is not built on a machine with no key, and the
     * tools of our own still work without it.
     */
    @Override
    public List<LLMProvider.ToolSpec> getTools() {
        return delegate == null ? extra
                : Stream.concat(delegate.getTools().stream(), extra.stream()).toList();
    }

    @Override
    public void onRequest() {
        if (delegate != null) {
            delegate.onRequest();
        }
    }

    @Override
    public void onResponse(ResponseListener.ResponseEvent event) {
        if (delegate != null) {
            delegate.onResponse(event);
        }
    }
}
