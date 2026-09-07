package com.vaadin.bakery.diagnostics;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * OBS-05. Metrics say a great deal about a business, so the only public
 * actuator endpoint is health. The kit itself runs under the observability
 * profile, which CI does not build: what this test protects is the access
 * rule, which is the part that can leak.
 *
 * A plain HTTP client rather than a Spring test client: the test client classes
 * moved package in Boot 4, and this needs no guessing.
 */
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
// This is the only test that starts a real servlet container. That context
// becomes the current Vaadin service for the JVM, which breaks every
// browserless test that runs after it, so it is disposed straight away.
@org.springframework.test.annotation.DirtiesContext(
        classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ObservabilityEndpointTest {

    @LocalServerPort
    private int port;

    private int statusOf(String path) throws Exception {
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
        }
    }

    @Test
    void healthIsReachableWithoutLoggingIn() throws Exception {
        int status = statusOf("/actuator/health");
        assertTrue(status == 200 || status == 404,
                "health is public when actuator is on the classpath and absent otherwise, never a login redirect, got "
                        + status);
    }

    @Test
    void metricsAreNotPublic() throws Exception {
        assertNotEquals(200, statusOf("/actuator/prometheus"),
                "an anonymous request must not receive the metrics");
    }
}
