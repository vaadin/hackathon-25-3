package com.vaadin.bakery.base;

import java.util.List;

/**
 * The preview features this application is built on, in one place.
 *
 * They were listed twice: once in the about page and once nowhere, which is why
 * nothing checked them at startup. A flag that is off does not break the build
 * and does not throw. It removes a component, and the first sign is a screen
 * that renders half of itself, usually in front of somebody.
 *
 * The file is named here as well, because "breadcrumbsComponent is off" is only
 * half an instruction: the other half is where to turn it on.
 */
public final class RequiredFeatures {

    /** Where the flags are set. Toggling in the dev tools does not survive a restart. */
    public static final String FILE = "src/main/resources/vaadin-featureflags.properties";

    public record Feature(String id, String usedFor) {
    }

    public static final List<Feature> ALL = List.of(
            new Feature("breadcrumbsComponent", "Checkout trail and order detail"),
            new Feature("switchComponent", "Availability and lock toggles"),
            new Feature("aiComponents", "The assistant"));

    private RequiredFeatures() {
    }
}
