package com.vaadin.bakery.base.error;

/**
 * Every refusal the domain makes carries a translation key and its arguments,
 * so the UI renders a sentence rather than a stack trace. This replaces the
 * exception to message mapping the old Bakery kept inside its presenters.
 */
public class DomainException extends RuntimeException {

    private final String translationKey;
    private final transient Object[] arguments;

    public DomainException(String translationKey, Object... arguments) {
        super(translationKey);
        this.translationKey = translationKey;
        this.arguments = arguments;
    }

    public String translationKey() {
        return translationKey;
    }

    public Object[] arguments() {
        return arguments == null ? new Object[0] : arguments.clone();
    }

    /** The thing is not there. */
    public static class NotFound extends DomainException {
        public NotFound(String translationKey, Object... arguments) {
            super(translationKey, arguments);
        }
    }

    /** Somebody else changed it first, or it is in use. */
    public static class Conflict extends DomainException {
        public Conflict(String translationKey, Object... arguments) {
            super(translationKey, arguments);
        }
    }

    /** The request is understood and refused. */
    public static class RuleViolation extends DomainException {
        public RuleViolation(String translationKey, Object... arguments) {
            super(translationKey, arguments);
        }
    }
}
