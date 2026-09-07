package com.vaadin.bakery;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.component.dependency.StyleSheet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bakery 25.3. See specs/00-overview.md before changing anything here.
 */
@SpringBootApplication
// The assistant streams its answer token by token, which reaches the browser
// only over push. Without this the chat looks dead until the next request.
@Push
@Viewport("width=device-width, initial-scale=1")
@PWA(name = "Bakery", shortName = "Bakery", offlinePath = "offline.html",
        offlineResources = { "images/products/sourdough-loaf.webp" })
// No theme is declared here on purpose: the shell loads exactly one at
// runtime, from the theme selector, so plain Lumo and plain Aura can be
// compared without one of them leaking into the other.
@StyleSheet("styles.css")
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
