package com.vaadin.example.sightseeing.views.nothing;

import com.vaadin.example.sightseeing.data.Role;
import com.vaadin.example.sightseeing.security.AuthenticatedUser;
import com.vaadin.example.sightseeing.ui.AdminNav;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@PageTitle("Nothing")
@Route(value = "nothing")
@PermitAll
public class NothingView extends VerticalLayout {

    public NothingView(AuthenticatedUser authenticatedUser) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        authenticatedUser.get().ifPresent(u -> {
            if (u.getRoles().contains(Role.ADMIN)) {
                add(new AdminNav("nothing"));
            }
        });
    }
}
