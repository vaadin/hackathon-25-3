package com.vaadin.bakery;

import com.vaadin.testbench.BrowserTestBase;
import com.vaadin.testbench.parallel.Browser;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
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
 */
public abstract class BrowserIT extends BrowserTestBase {

    private static final String PORT = System.getProperty("it.port", "8081");
    private static final Duration PATIENCE = Duration.ofSeconds(30);

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

    protected void open(String path) {
        getDriver().get(url(path));
        waitForVaadin();
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
     * It waits for the hidden CSRF field before submitting. Posting as soon as
     * the two visible inputs exist sends a form Spring Security rejects, and
     * the browser lands back on the login page looking exactly as if the
     * password were wrong. That cost an hour the first time.
     */
    protected void signIn(String email, String password) {
        open("/login");
        patiently().until(driver -> Boolean.TRUE.equals(script(
                "var f = document.querySelector('form');"
                        + "return !!f && !!f.querySelector('input[name=username]')"
                        + " && f.querySelectorAll('input[type=hidden]').length > 0;")));

        ((JavascriptExecutor) getDriver()).executeScript(
                "var f = document.querySelector('form');"
                        + "f.querySelector('input[name=username]').value = arguments[0];"
                        + "f.querySelector('input[name=password]').value = arguments[1];"
                        + "f.submit();",
                email, password);

        patiently().until(driver -> !driver.getCurrentUrl().contains("/login"));
        waitForVaadin();
    }

    /** What the page actually says, so a failure names the page it was on. */
    protected String whereAmI() {
        return getDriver().getCurrentUrl() + " showing: "
                + script("return document.body.innerText.slice(0, 120).replace(/\\n/g, ' | ');");
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
