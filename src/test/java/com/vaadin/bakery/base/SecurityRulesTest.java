package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * FND-02 and FND-03. Access control is by annotation, so a route that carries
 * none is a hole. This test walks every route in the application and fails on
 * the first one that forgot, which is why adding a view cannot leak data by
 * omission.
 */
class SecurityRulesTest {

    @Test
    void everyRouteDeclaresItsAccess() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Route.class));

        List<String> offenders = new ArrayList<>();
        int routes = 0;
        for (var candidate : scanner.findCandidateComponents("com.vaadin.bakery")) {
            var type = Class.forName(candidate.getBeanClassName());
            routes++;
            boolean declared = type.isAnnotationPresent(AnonymousAllowed.class)
                    || type.isAnnotationPresent(PermitAll.class)
                    || type.isAnnotationPresent(RolesAllowed.class)
                    || type.isAnnotationPresent(DenyAll.class);
            if (!declared) {
                offenders.add(type.getName());
            }
        }

        assertTrue(routes > 0, "No routes were found, the scan is broken");
        assertTrue(offenders.isEmpty(), "Routes without an access annotation: " + offenders);
    }
}
