package com.example;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Calls the repository method, so the failure shows on a screen. */
@Route("ticket")
@AnonymousAllowed
public class TicketView extends VerticalLayout {

    public TicketView(Tickets tickets) {
        var found = tickets.byReference("T-1");
        add(new H2(found.map(Ticket::getReference).orElse("no ticket, and no exception either")));
    }
}
