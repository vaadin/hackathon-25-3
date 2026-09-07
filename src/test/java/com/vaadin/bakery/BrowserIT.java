package com.vaadin.bakery;

import com.vaadin.testbench.BrowserTestBase;
import com.vaadin.testbench.parallel.Browser;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * What every browser test starts from.
 *
 * The tier exists because the browserless one has been wrong about the browser
 * more than once in this repository: a page title generator renamed every page
 * while 224 browserless tests resolved the right titles, a grid rendered empty
 * with exactly the columns a test asserted on, and a chart drew Highcharts' own
 * palette on a dark page. Those are the questions that belong here, and the
 * closed list of them is in {@code specs/08-testing.md}.
 *
 * The application is started once by the {@code it} profile, not per test, so a
 * test must leave the database as it found it or say in its name that it does
 * not.
 *
 * <p>Two things to know before writing one. {@code assertEquals} does not mean
 * what it usually means here: {@code AbstractBrowserTestBase} declares an
 * {@code assertEquals(WebElement, WebElement)} that shadows the static import
 * in every subclass, so compare with {@code assertTrue(a.equals(b), ...)} or
 * qualify the call. And a failure should say where it was, which is what
 * {@link #whereAmI()} is for: the first three failures in this tier all read
 * "expected true but was false" until it existed.
 */
public abstract class BrowserIT extends BrowserTestBase {

    private static final String PORT = System.getProperty("it.port", "8081");
    private static final Duration PATIENCE = Duration.ofSeconds(30);

    /** Whoever is signed in, so a lost session can be picked up again. */
    private String email;
    private String password;

    @BeforeEach
    void sizeTheWindow() {
        getDriver().manage().window().setSize(new Dimension(1400, 1000));
    }

    @Override
    protected Browser getRunLocallyBrowser() {
        return Browser.CHROME;
    }

    protected String url(String path) {
        return "http://localhost:" + PORT + (path.startsWith("/") ? path : "/" + path);
    }

    /**
     * Opens a page, and signs in again if the server sends us to the login
     * screen instead.
     *
     * A session occasionally goes away between signing in and asking for the
     * first page, roughly one run in four across six classes. We have not found
     * why. What it produced before this was never an honest failure: the page
     * that came back was the login screen, and the assertion that followed
     * reported nought charts, or an empty list, in a test about charts and
     * lists. So the bounce is handled where it is visible, and a bounce that
     * cannot be healed fails as what it is, in {@link #signIn}.
     */
    protected void open(String path) {
        goTo(path);
        if (email == null || path.startsWith("/login") || !bouncedToLogin(path)) {
            return;
        }
        signIn(email, password);
        goTo(path);
    }

    private void goTo(String path) {
        getDriver().get(url(path));
        waitForVaadin();
    }

    private boolean bouncedToLogin(String path) {
        return !path.startsWith("/login") && getDriver().getCurrentUrl().contains("/login");
    }

    /** Vaadin renders client side, so nothing is on the page when the load ends. */
    protected void waitForVaadin() {
        patiently().until(driver -> Boolean.TRUE.equals(script(
                "return !!window.Vaadin && document.body.innerText.trim().length > 0;")));
    }

    protected WebElement waitFor(By locator) {
        return patiently().until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Signs in through the real form, which is the only way in.
     *
     * Four things here are not decoration, and between them they took the
     * suite from two failed runs in three to eight clean ones in a row. Which
     * of the four did it is now known, and it was not the one we first
     * credited: see the note on the token below.
     *
     * <p>The CSRF token is written into the hidden field by hand, from the
     * {@code _csrf} meta tags, because the overlay renders
     * {@code <input id="csrf" type="hidden">} with no name and no value and
     * fills both in its own submit handler. That much is true and it turned out
     * not to matter: a bare project and this application both sign in from a
     * scripted submit carrying no token, because CSRF is not enforced on that
     * POST. It is kept because it costs nothing and would be correct in a setup
     * that does enforce it. It is not why the flake stopped.
     *
     * <p>The wait afterwards is positive: off the login route <em>and</em> the
     * application shell present. "The URL no longer contains /login" passes on
     * whatever the browser reports mid POST, and the next navigation then
     * bounces to the login page with the failure landing in an unrelated
     * assertion.
     *
     * <p>It ends by asking the server, not the address bar, whether anybody is
     * actually signed in. The landing page is open to everyone, so a POST the
     * server declined can leave the browser looking exactly like a successful
     * login.
     *
     * <p>And it retries once, because the failures were intermittent and we
     * never found the reason. That is worth saying plainly rather than dressing
     * up: what is fixed is that a bad sign in now fails as a bad sign in, and
     * the probe above is almost certainly what stopped the flake, because it is
     * the only one of the four that catches a login that did not take.
     */
    protected void signIn(String email, String password) {
        this.email = email;
        this.password = password;
        if (attemptSignIn(email, password) || attemptSignIn(email, password)) {
            return;
        }
        throw new AssertionError("could not sign in as " + email + ", at " + whereAmI());
    }

    private boolean attemptSignIn(String email, String password) {
        goTo("/login");
        // The token, not the empty field that holds it.
        patiently().until(driver -> Boolean.TRUE.equals(script(
                "var f = document.querySelector('form');"
                        + "var t = document.querySelector('meta[name=_csrf]');"
                        + "var p = document.querySelector('meta[name=_csrf_parameter]');"
                        + "return !!f && !!f.querySelector('input[name=username]')"
                        + " && !!t && !!t.content && !!p && !!p.content;")));

        ((JavascriptExecutor) getDriver()).executeScript(
                "var f = document.querySelector('form');"
                        + "var hidden = f.querySelector('input[type=hidden]');"
                        + "hidden.name = document.querySelector('meta[name=_csrf_parameter]').content;"
                        + "hidden.value = document.querySelector('meta[name=_csrf]').content;"
                        + "f.querySelector('input[name=username]').value = arguments[0];"
                        + "f.querySelector('input[name=password]').value = arguments[1];"
                        + "f.submit();",
                email, password);

        try {
            patiently().until(driver -> Boolean.TRUE.equals(script(
                    "return !location.pathname.startsWith('/login')"
                            + " && !!document.querySelector('vaadin-app-layout');")));
        } catch (TimeoutException notSignedIn) {
            return false;
        }
        waitForVaadin();
        return isSignedIn();
    }

    /**
     * Asks the server, rather than reading the address bar.
     *
     * Landing off the login route is not the same as being signed in: the
     * landing page is open to anybody, so a POST that Spring declined can leave
     * the browser on a page with the full application shell around it and
     * nothing signed in behind it. This asks for a route that every role can
     * reach and no anonymous visitor can, and looks at where the request ended.
     */
    private boolean isSignedIn() {
        return Boolean.TRUE.equals(script(
                "var probe = new XMLHttpRequest();"
                        + "probe.open('GET', '/orders', false);"
                        + "probe.send();"
                        + "return probe.responseURL.indexOf('/login') === -1;"));
    }

    /** What the page actually says, so a failure names the page it was on. */
    protected String whereAmI() {
        return getDriver().getCurrentUrl() + " showing: "
                + script("return document.body.innerText.slice(0, 120).replace(/\\n/g, ' | ');");
    }

    /**
     * Waits until a measurement stops changing, and returns it.
     *
     * A layout that animates is briefly neither where it was nor where it is
     * going, and a test that measures the moment an element appears measures
     * the animation. Sleeping is forbidden in this suite and would be the wrong
     * tool anyway: what the test is waiting for is not a duration, it is a
     * value that has settled.
     */
    /** Waits until a snippet of JavaScript says true. */
    protected void waitUntil(String javascript) {
        patiently().until(driver -> Boolean.TRUE.equals(script(javascript)));
    }

    /**
     * There is nothing to wait for, and this is the note that says so.
     *
     * A helper lived here that waited for `AppLayout` to have `drawer-opened`
     * and no `overlay`, on the assumption that those attributes mark the point
     * where the shell has settled. They do not. Measured on a cold load at a
     * 1533 pixel viewport: at 169 ms the drawer is already 256 pixels wide, the
     * navigation is visible, the view is opaque, and the content is still 1533
     * wide; at 190 ms the same content is 1277. Both attributes hold their
     * final values in both samples.
     *
     * So a layout test must not compare an absolute width against one it read
     * earlier. Measure a width and its container's in one script call and
     * compare the share, the way {@code KitchenSummaryOverlayIT} does. The
     * measurements are in {@code specs/FEEDBACK-PLATFORM.md}.
     */

    protected <T> T settled(java.util.function.Supplier<T> measure) {
        var seen = new java.util.concurrent.atomic.AtomicReference<T>(measure.get());
        patiently().until(driver -> {
            var now = measure.get();
            var same = now != null && now.equals(seen.get());
            seen.set(now);
            return same;
        });
        return seen.get();
    }

    protected Object script(String javascript) {
        return ((JavascriptExecutor) getDriver()).executeScript(javascript);
    }

    protected long count(String selector) {
        return (Long) script("return document.querySelectorAll('" + selector + "').length;");
    }

    private WebDriverWait patiently() {
        return new WebDriverWait(getDriver(), PATIENCE);
    }
}
