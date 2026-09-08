package com.example;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Removing a stylesheet and adding the same URL back in one round trip. The
 * page ends up with neither, and nothing is logged.
 *
 * This is what an application switching between two themes that share a sheet
 * does without thinking about it.
 */
@Route("swap")
@AnonymousAllowed
public class SwapView extends VerticalLayout {

    private Registration sheet;

    public SwapView() {
        add(new H2("Remove and add the same URL, in one round trip"),
                new Paragraph("1. Load. 2. Swap. Read the link elements after each one."),
                new Button("1. load Lumo", event -> sheet = page().addStyleSheet(Lumo.STYLESHEET)),
                new Button("2. remove it and add the same URL", event -> {
                    sheet.remove();
                    sheet = page().addStyleSheet(Lumo.STYLESHEET);
                }),
                new Button("the same, in two round trips: remove", event -> sheet.remove()),
                new Button("and then add", event -> sheet = page().addStyleSheet(Lumo.STYLESHEET)));
    }

    private com.vaadin.flow.component.page.Page page() {
        return getUI().orElseThrow().getPage();
    }
}
