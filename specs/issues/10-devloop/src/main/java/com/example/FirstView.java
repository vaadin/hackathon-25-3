package com.example;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Swap this order with the other view's and press apply. */
@Route("first")
@Menu(order = 11, title = "First")
@AnonymousAllowed
public class FirstView extends VerticalLayout {

    public FirstView() {
        add(new H2("First"));
    }
}
