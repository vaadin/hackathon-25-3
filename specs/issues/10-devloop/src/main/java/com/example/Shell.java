package com.example;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;

/** A drawer built from the menu configuration, which is where the order shows. */
@Layout
@AnonymousAllowed
public class Shell extends AppLayout {

    public Shell() {
        var nav = new SideNav();
        MenuConfiguration.getMenuEntries()
                .forEach(entry -> nav.addItem(new SideNavItem(entry.title(), entry.path())));
        addToDrawer(nav);
        setPrimarySection(Section.DRAWER);
    }
}
