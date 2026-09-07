package com.vaadin.bakery.assistant.ui;

import com.vaadin.bakery.assistant.AssistantConfiguration.AssistantStatus;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;

/**
 * What an assistant surface shows when there is no model behind it.
 *
 * It is red, it is where the question box would have been, and it says which
 * switch is off: the build without the {@code ai} profile and the profile
 * without a key are two different problems and naming both at once sends the
 * reader to the wrong one. The alternative, a panel that quietly answers from
 * somewhere else, is the failure this application is built to avoid: on a
 * screen it looks exactly like a working assistant.
 */
final class Assistants {

    private Assistants() {
    }

    static Component unavailable(AssistantStatus status) {
        var badge = Translations.bindText(new Span(), "assistant.off");
        badge.getElement().getThemeList().add("badge error small");

        var block = new Div(badge,
                Translations.bindText(new Paragraph(), status.reason().translationKey()));
        block.addClassName("assistant-off");
        return block;
    }
}
