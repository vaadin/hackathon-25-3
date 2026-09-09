package com.vaadin.bakery.base;

import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Whether this machine has a Vaadin commercial key, asked without touching the
 * network.
 *
 * This class exists because the platform has no way to ask. A commercial
 * component validates its licence inside its own constructor, over HTTP, and on
 * a machine with no key that call blocks for five minutes and then throws, so
 * the view it was building fails and every test that opens that view fails with
 * it. There is no {@code isLicensed()} to ask first, which is one of the two
 * requests in the server side gaps report.
 *
 * So we answer the cheap half of the question ourselves: is there a key here to
 * try. A key that exists can still be expired or wrong, and that is fine: the
 * point is to skip the components that would hang when there is clearly nothing
 * to validate with.
 */
public final class CommercialLicence {

    private static final Logger LOG = LoggerFactory.getLogger(CommercialLicence.class);

    /** What the licence checker itself reads, in the order it reads it. */
    private static final String[] PROPERTIES = { "vaadin.offlineKey", "vaadin.proKey" };
    private static final String[] VARIABLES = { "VAADIN_OFFLINE_KEY", "VAADIN_PRO_KEY" };
    private static final String[] FILES = { "offlineKey", "proKey" };

    /** Asked on every view construction, answered once. */
    private static volatile Boolean present;

    private CommercialLicence() {
    }

    public static boolean isPresent() {
        if (present == null) {
            present = look();
            LOG.info("Commercial key {}, so commercial components are {}",
                    present ? "found" : "not found on this machine",
                    present ? "built" : "skipped where the application can do without them");
        }
        return present;
    }

    private static boolean look() {
        for (String property : PROPERTIES) {
            if (!isBlank(System.getProperty(property))) {
                return true;
            }
        }
        for (String variable : VARIABLES) {
            if (!isBlank(System.getenv(variable))) {
                return true;
            }
        }
        var home = System.getProperty("user.home");
        if (home == null) {
            return false;
        }
        for (String file : FILES) {
            if (Files.isReadable(Path.of(home, ".vaadin", file))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
