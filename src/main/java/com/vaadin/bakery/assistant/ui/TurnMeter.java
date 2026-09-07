package com.vaadin.bakery.assistant.ui;

import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * What the last answer cost, and whether the model finished saying it.
 *
 * The numbers come from the response metadata that 25.3 exposes per turn. The
 * finish reason is the interesting one: an answer cut off at the token limit
 * looks complete to a user unless somebody says so.
 */
public class TurnMeter extends Div {

    /** Rough public pricing, only ever shown as an estimate. */
    private static final double CENTS_PER_THOUSAND_TOKENS = 0.5;

    private final ValueSignal<Integer> turns = new ValueSignal<>(0);
    private final ValueSignal<Integer> tokens = new ValueSignal<>(0);
    private final ValueSignal<String> lastFinishReason = new ValueSignal<>("");

    public TurnMeter() {
        addClassName("turn-meter");

        var turnCount = Translations.bindText(new Span(),
                locale -> getTranslation(locale, "assistant.meter.turns", turns.get()));

        var tokenCount = Translations.bindText(new Span(),
                locale -> getTranslation(locale, "assistant.meter.tokens", tokens.get()));

        var cost = Translations.bindText(new Span(), locale -> getTranslation(locale, "assistant.meter.cost",
                String.format("%.2f", tokens.get() * CENTS_PER_THOUSAND_TOKENS / 1000)));

        // The text never varies, but the locale does, so it binds like the rest.
        var truncated = Translations.bindText(new Span(), "assistant.meter.truncated");
        truncated.getElement().getThemeList().add("badge error small");
        truncated.bindVisible(Signal.computed(() -> "max_tokens".equals(lastFinishReason.get())));

        add(turnCount, tokenCount, cost, truncated);
    }

    public void record(int totalTokens, String finishReason) {
        turns.update(value -> value + 1);
        tokens.update(value -> value + totalTokens);
        lastFinishReason.set(finishReason == null ? "" : finishReason);
    }

    public int turns() {
        return turns.peek();
    }

    public int tokens() {
        return tokens.peek();
    }

    public String lastFinishReason() {
        return lastFinishReason.peek();
    }

    public boolean wasTruncated() {
        return "max_tokens".equals(lastFinishReason.peek());
    }
}
