package com.example;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** An ordinary shell: a drawer with a navigation in it, and a navbar. */
@Layout
@AnonymousAllowed
public class Shell extends AppLayout {

    public Shell() {
        setPrimarySection(Section.DRAWER);
        addToNavbar(true, new DrawerToggle(), new H2("Shell"));

        var nav = new SideNav();
        nav.addItem(new SideNavItem("The view", ""));
        addToDrawer(nav);
    }
}
