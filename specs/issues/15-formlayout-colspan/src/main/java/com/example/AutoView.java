package com.example;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * The auto responsive half of the same report. Both forms ask for four columns
 * and the only difference is one line.
 */
@Route("auto")
@AnonymousAllowed
public class AutoView extends VerticalLayout {

    public AutoView() {
        add(new H3("setMaxColumns(4), fields added directly"),
                new Paragraph("Renders one column."), form(false),
                new H3("The same, with setAutoRows(true)"),
                new Paragraph("Renders four."), form(true));
        setWidthFull();
    }

    private FormLayout form(boolean autoRows) {
        var layout = new FormLayout();
        layout.setAutoResponsive(true);
        layout.setMaxColumns(4);
        layout.setAutoRows(autoRows);
        layout.add(new TextField("One"), new TextField("Two"), new TextField("Three"),
                new TextField("Four"));
        return layout;
    }
}
