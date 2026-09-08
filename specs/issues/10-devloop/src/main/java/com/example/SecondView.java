package com.example;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("second")
@Menu(order = 12, title = "Second")
@AnonymousAllowed
public class SecondView extends VerticalLayout {

    public SecondView() {
        add(new H2("Second"));
    }
}
