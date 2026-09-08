package com.example;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.concurrent.atomic.AtomicInteger;

/** Counts how many times it was built, which is what navigating twice tests. */
@Route("counting")
@AnonymousAllowed
public class CountingView extends VerticalLayout {

    static final AtomicInteger BUILDS = new AtomicInteger();

    public CountingView() {
        add(new H2("Built " + BUILDS.incrementAndGet() + " time(s)"));
    }
}
