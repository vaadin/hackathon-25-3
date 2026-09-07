package com.vaadin.bakery.base.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.BrowserIT;
import com.vaadin.testbench.BrowserTest;
import java.util.List;

/**
 * FIX-01. The first page anybody sees has a theme.
 *
 * No theme is declared on the app shell, on purpose, so that plain Lumo and
 * plain Aura can be compared without one leaking into the other. That makes
 * this a question only a cold load in a browser can answer: the stylesheet is
 * added at runtime, and it used to be added by the shell, which the login page
 * is not inside. It arrived in serif type with a black button, and looked right
 * afterwards only because a session that had been inside the shell left the
 * sheet on the page.
 */
class ColdLoginThemeIT extends BrowserIT {

    @BrowserTest
    @SuppressWarnings("unchecked")
    void theLoginPageIsThemedOnAColdLoad() {
        // No session, no prior navigation: exactly what a visitor gets.
        getDriver().manage().deleteAllCookies();
        open("/login");

        var sheets = (List<String>) script("return [...document.querySelectorAll('link[rel=stylesheet]')]"
                + ".map(l => l.href);");

        assertTrue(sheets.stream().anyMatch(href -> href.contains("lumo") || href.contains("aura")),
                "a theme stylesheet is on the page, got " + sheets);
        assertTrue(sheets.stream().anyMatch(href -> href.contains("bakery.css")),
                "and the bakery palette with it, got " + sheets);
    }

    /**
     * And the theme actually took. A linked stylesheet that has not applied
     * looks the same from the DOM, so this asks the rendered button.
     */
    @BrowserTest
    void theSubmitButtonIsThemedRatherThanABareControl() {
        getDriver().manage().deleteAllCookies();
        open("/login");
        waitFor(org.openqa.selenium.By.cssSelector("vaadin-button[slot=submit]"));

        var background = String.valueOf(script(
                "var b = document.querySelector('vaadin-button[slot=submit]');"
                        + "return getComputedStyle(b).backgroundColor;"));

        // The unthemed control is near black. The bakery primary is a brown.
        assertTrue(!background.startsWith("rgb(0, 0, 0)") && !background.contains("26, 26, 26"),
                "the submit button carries the theme's colour, got " + background);
    }
}
