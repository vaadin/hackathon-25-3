package com.vaadin.bakery.base.ui;

import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.textfield.EmailField;

/** Fields that need something said about them before they are usable. */
public final class Fields {

    private Fields() {
    }

    /**
     * An email field that says why it is unhappy.
     *
     * An `EmailField` checks the address itself, and in 25.3.0-beta1 that check
     * wins over the bean validation message and then reports nothing: the field
     * turns red and stays silent. The same property bound to a `TextField` reads
     * "must be a well-formed email address", so the message exists and the field
     * swallows it. Recorded in `specs/FEEDBACK-25.3.md`.
     *
     * Setting the message here also translates it, which the platform's own
     * message never was: it arrives in English from Hibernate Validator whatever
     * language the reader chose.
     */
    public static EmailField email(String labelKey) {
        var field = new EmailField();
        Translations.bind(field, field::setLabel, labelKey);
        Translations.onLocale(field, locale -> field.setI18n(new EmailField.EmailFieldI18n()
                .setPatternErrorMessage(field.getTranslation(locale, "validation.email"))));
        return field;
    }
}
