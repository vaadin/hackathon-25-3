package com.example;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.aura.Aura;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * The two mechanisms, one button each, and a theme switch to try them under.
 *
 * No theme is declared on the app shell, so the page starts unstyled and the
 * theme is whichever button was pressed. That is what makes the comparison
 * honest: neither theme leaks into the other.
 */
@Route("")
@AnonymousAllowed
public class DarkView extends VerticalLayout {

    public DarkView() {
        var themeAttribute = new Button("theme=\"dark\" on the body", event -> {
            getUI().ifPresent(ui -> ui.getElement().getThemeList().add("dark"));
        });
        var themeAttributeOff = new Button("remove theme=\"dark\"", event -> {
            getUI().ifPresent(ui -> ui.getElement().getThemeList().remove("dark"));
        });
        var colourScheme = new Button("Page.setColorScheme(DARK)", event -> {
            getUI().ifPresent(ui -> ui.getPage().setColorScheme(ColorScheme.Value.DARK));
        });
        var colourSchemeOff = new Button("Page.setColorScheme(LIGHT)", event -> {
            getUI().ifPresent(ui -> ui.getPage().setColorScheme(ColorScheme.Value.LIGHT));
        });

        var lumo = new Button("load Lumo", event -> load(Lumo.STYLESHEET));
        var aura = new Button("load Aura", event -> load(Aura.STYLESHEET));

        add(new H2("Two ways to go dark"),
                new Paragraph("Load a theme first, then press one of the dark buttons and "
                        + "look at the page. Under Lumo the theme attribute is enough. Under "
                        + "Aura it does nothing, and the colour scheme is what works."),
                new HorizontalLayout(lumo, aura),
                new HorizontalLayout(themeAttribute, themeAttributeOff),
                new HorizontalLayout(colourScheme, colourSchemeOff));
    }

    private void load(String stylesheet) {
        getUI().ifPresent(ui -> ui.getPage().addStyleSheet(stylesheet));
    }
}
