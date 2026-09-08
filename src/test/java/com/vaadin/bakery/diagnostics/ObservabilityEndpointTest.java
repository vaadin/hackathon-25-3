package com.vaadin.bakery.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        return statusOf(path, null, null);
    }

    /** The same request, with HTTP Basic credentials when there are any. */
    private int statusOf(String path, String user, String password) throws Exception {
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
            if (user != null) {
                request.header("Authorization", "Basic " + java.util.Base64.getEncoder()
                        .encodeToString((user + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            }
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
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

    /**
     * A challenge, not a redirect, and the difference is what makes both
     * readers work.
     *
     * A 302 to the login view sends a browser to a form it can use and a
     * scraper to a form it cannot, and it hides the fact that credentials were
     * wanted at all. A 401 with `WWW-Authenticate` makes the browser ask for a
     * password, which is how the links on the diagnostics screen are opened,
     * and tells anything else exactly what to send.
     *
     * It was a 302 for a while, with the challenge header on it, because the
     * container's error dispatch went through the chain that matches
     * everything and that chain redirects. Hence the explicit permit for the
     * error dispatch, and hence this test.
     */
    @Test
    void anUnauthenticatedRequestIsChallengedRatherThanRedirected() throws Exception {
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()) {
            var request = HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/actuator/prometheus")).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(401, response.statusCode(), "a challenge rather than a redirect");
            assertTrue(response.headers().firstValue("WWW-Authenticate").orElse("").startsWith("Basic"),
                    "and it says how to authenticate");
        }
    }

    /**
     * A scraper authenticates with HTTP Basic, because it cannot use a login
     * form. That is what the actuator's own security chain is for, and it is
     * testable here whether or not the kit is on the classpath: authorization
     * runs before routing, so an admin gets past the rule and reaches either
     * the metrics or a 404, while anyone else is refused by the rule itself.
     *
     * Without that chain the answer for all three was a 302 to the login page,
     * so the committed Prometheus job collected a login form every five
     * seconds and the dashboard stayed empty with nothing saying why.
     */
    @Test
    void aScraperGetsInWithBasicCredentialsAndOnlyAsAnAdmin() throws Exception {
        int asAdmin = statusOf("/actuator/prometheus", "admin@bakery.test", "admin");
        assertTrue(asAdmin == 200 || asAdmin == 404,
                "an admin's credentials are accepted rather than redirected, got " + asAdmin);

        assertEquals(403, statusOf("/actuator/prometheus", "barista@bakery.test", "barista"),
                "and a barista is refused by the rule, not by the endpoint");
    }
}
