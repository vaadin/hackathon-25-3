package com.vaadin.example.sightseeing.views.something;

import com.vaadin.example.sightseeing.data.Role;
import com.vaadin.example.sightseeing.security.AuthenticatedUser;
import com.vaadin.example.sightseeing.ui.AdminNav;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

@PageTitle("Something")
@PermitAll
@Route(value = "something")
public class SomethingView extends VerticalLayout {

    public SomethingView(AuthenticatedUser authenticatedUser) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        authenticatedUser.get().ifPresent(u -> {
            if (u.getRoles().contains(Role.ADMIN)) {
                add(new AdminNav("something"));
            }
        });
VerticalLayout verticallayout = new VerticalLayout();
        H1 heading1 = new H1("Heading 1");
        HorizontalLayout horizontallayout = new HorizontalLayout();
        Div div = new Div();
        Button button4 = new Button("Button");
add(verticallayout);
Button button2 = new Button("Button");
Button button3 = new Button("Button");
Button button = new Button("Button");
div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.ROW);
H3 heading31 = new H3("Heading 3-1");
H4 heading41 = new H4("Heading 4-1");
H4 heading422 = new H4("Heading 4-2");
H4 heading43 = new H4("Heading 4-3");
H4 heading44 = new H4("Heading 4-4");
Button button5 = new Button("Button");
verticallayout.addComponentAsFirst(button5);
Button button6 = new Button("Button");
Div div2 = new Div();
div2.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
div2.add(heading41, heading422, heading43, heading44);
div2.getStyle().setWidth("100%");
verticallayout.add(heading1, horizontallayout, div, button6, div2, button4);
H3 heading33 = new H3("Heading 3-3");
H3 heading32 = new H3("Heading 3-2");
div.add(heading31, heading32, heading33, button2, button3, button);
div.getStyle().setWidth("100%");
H2 heading21 = new H2("Heading 2-1");
H2 heading22 = new H2("Heading 2-2");
horizontallayout.add(heading21, heading22);
    }
}
