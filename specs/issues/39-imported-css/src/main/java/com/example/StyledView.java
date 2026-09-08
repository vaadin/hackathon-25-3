package com.example;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * The heading is pink if the imported stylesheet arrived, and black if it did
 * not. Nothing else on this page matters.
 */
@Route("")
@StyleSheet("styles/main.css")
@AnonymousAllowed
public class StyledView extends VerticalLayout {

    public StyledView() {
        add(new H2("Pink if parts.css arrived"),
                new Paragraph("main.css is declared and served. It imports parts.css, "
                        + "which is an ordinary request the security setup does not permit."));
    }
}
