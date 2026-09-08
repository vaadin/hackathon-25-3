package com.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.menu.MenuConfiguration;

/**
 * A shell whose header asks the platform what the open page is called, which is
 * what an application does instead of repeating the title in every view.
 *
 * The header and the browser tab come from the same generator and disagree: the
 * router resolves the title with the route parameters, and
 * `MenuConfiguration.getPageHeader` resolves it with a context that has none.
 */
@Layout
public class Shell extends AppLayout implements AfterNavigationObserver {

    private final Span header = new Span();

    public Shell() {
        header.getStyle().set("font-weight", "600");
        addToNavbar(header);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        var view = getContent();
        header.setText(view instanceof Component component
                ? MenuConfiguration.getPageHeader(component).orElse("(no header)")
                : "(no view)");
    }
}
