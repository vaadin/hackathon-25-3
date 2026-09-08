package com.example;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** A view over the shared signal, so a browserless test has something to open. */
@Route("")
@AnonymousAllowed
public class CounterView extends VerticalLayout {

    public CounterView(Tickets tickets) {
        var count = new Span();
        count.bindText(tickets.waiting().map(String::valueOf));
        add(count, new Button("One more", event -> tickets.waiting().update(value -> value + 1)));
    }
}
