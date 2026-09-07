package com.vaadin.bakery.base;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.Version;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * What this machine can actually do, said once at startup.
 *
 * The about page answers the same question and answers it better, but somebody
 * has to already suspect something to open it. This is the line in the log that
 * a person reads when a screen looks wrong and they have not thought of the
 * flags yet.
 *
 * A missing flag is an error and not a failure to boot. The application runs
 * with a component missing, which is worth saying loudly and is not worth
 * refusing to start over: the rest of the bakery still works, and a build that
 * will not start tells nobody which flag it was.
 */
@Component
public class StartupReport implements VaadinServiceInitListener {

    private static final Logger LOG = LoggerFactory.getLogger(StartupReport.class);

    private final Environment environment;

    public StartupReport(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        var flags = FeatureFlags.get(event.getSource().getContext());
        var profiles = environment.getActiveProfiles();

        LOG.info("Bakery on Vaadin {}, profiles {}", Version.getFullVersion(),
                profiles.length == 0 ? "[none]" : List.of(profiles));

        for (RequiredFeatures.Feature feature : RequiredFeatures.ALL) {
            if (flags.isEnabled(feature.id())) {
                LOG.info("Feature flag {} is on, which is what {} needs", feature.id(), feature.usedFor());
            } else {
                LOG.error("""
                        Feature flag {} is OFF and this application depends on it for {}. \
                        Set com.vaadin.experimental.{}=true in {} and restart. The application \
                        will start, and that part of it will be missing.""",
                        feature.id(), feature.usedFor(), feature.id(), RequiredFeatures.FILE);
            }
        }
    }

    /** The ones that are off, which is what the about page paints red. */
    public static List<RequiredFeatures.Feature> missing(FeatureFlags flags) {
        return RequiredFeatures.ALL.stream()
                .filter(feature -> !flags.isEnabled(feature.id()))
                .toList();
    }
}
