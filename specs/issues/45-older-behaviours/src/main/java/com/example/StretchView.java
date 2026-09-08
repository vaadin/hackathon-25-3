package com.example;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * A CSS grid inside a vertical layout. Open it in a browser: the panels are in
 * one column however wide the window is, because the child is only as wide as
 * its contents. The commented line is the fix.
 */
@Route("stretch")
@AnonymousAllowed
public class StretchView extends VerticalLayout {

    public StretchView() {
        var grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(20rem, 1fr))")
                .set("gap", "1rem");
        // grid.setWidthFull();
        for (int i = 1; i <= 4; i++) {
            var panel = new Div();
            panel.setText("Panel " + i);
            panel.getStyle().set("background", "#eee").set("padding", "2rem");
            grid.add(panel);
        }
        add(grid);
    }
}
