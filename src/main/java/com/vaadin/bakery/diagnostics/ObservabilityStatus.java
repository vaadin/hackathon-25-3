package com.vaadin.bakery.diagnostics;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Whether the kit half of observability is running, and which switch is off
 * when it is not.
 *
 * Three switches, and all three have to be on, which is why "it does not work"
 * is not a useful answer on its own: the dependency has to be in the build, the
 * kit has to be enabled, and the actuator has to expose the endpoint. The kit
 * is looked up by name rather than imported, because the default build does not
 * have it on the classpath and this class is in every build.
 *
 * Same shape as {@code AssistantStatus} on purpose. A screen that says a
 * feature is off and cannot say which switch sends its reader to the wrong one:
 * that lesson was learnt on the assistant panel and it applies here.
 */
@Component
public class ObservabilityStatus {

    /** Present only when `observability-kit-spring` is on the classpath. */
    private static final String KIT_CLASS = "com.vaadin.observability.spring.ObservabilityConfiguration";

    /** What the kit's own endpoint is called under the actuator. */
    public static final String METRICS_PATH = "/actuator/prometheus";
    public static final String INSIGHTS_PATH = "/actuator/vaadin/observability";
    public static final String HEALTH_PATH = "/actuator/health";

    public enum Reason {

        /** It is running. */
        NONE("diagnostics.kit.off.none"),
        /** Built without the `observability` profile, so the kit is not here. */
        NOT_IN_BUILD("diagnostics.kit.off.classpath"),
        /** On the classpath and switched off by `vaadin.observability.enabled`. */
        DISABLED("diagnostics.kit.off.disabled"),
        /** Enabled, and the actuator is not publishing the endpoint. */
        ENDPOINT_NOT_EXPOSED("diagnostics.kit.off.endpoints");

        private final String translationKey;

        Reason(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    private final Environment environment;

    public ObservabilityStatus(Environment environment) {
        this.environment = environment;
    }

    public boolean isActive() {
        return reason() == Reason.NONE;
    }

    public Reason reason() {
        if (!ClassUtils.isPresent(KIT_CLASS, getClass().getClassLoader())) {
            return Reason.NOT_IN_BUILD;
        }
        if (!environment.getProperty("vaadin.observability.enabled", Boolean.class, false)) {
            return Reason.DISABLED;
        }
        var exposed = environment.getProperty("management.endpoints.web.exposure.include", "");
        if (!exposed.contains("prometheus") && !exposed.contains("*")) {
            return Reason.ENDPOINT_NOT_EXPOSED;
        }
        return Reason.NONE;
    }
}
