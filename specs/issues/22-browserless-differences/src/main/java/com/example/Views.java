package com.example;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Two plain views. The title comes from a bean, with no annotation anywhere. */
public class Views {

    @Route("plain")
    @AnonymousAllowed
    public static class PlainView extends VerticalLayout {
        public PlainView() {
            add(new H2("Plain"));
        }
    }

    @Route("titled")
    @AnonymousAllowed
    public static class TitledView extends VerticalLayout {
        public TitledView() {
            add(new H2("Titled"));
        }
    }
}
