package com.example;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * A form with three responsive steps and one field asked to be three columns
 * wide. Resize the window and read the spans.
 *
 * What is wanted: three columns wide at four columns, two at two columns, one
 * at one. What happens: min(colspan, columns), so the middle step gives the
 * field the whole row and the other three fields drop below it.
 */
@Route("steps")
@AnonymousAllowed
public class StepsView extends VerticalLayout {

    public StepsView() {
        var search = new TextField("Search");
        var category = new ComboBox<String>("Category");
        var without = new ComboBox<String>("Without");
        var sort = new ComboBox<String>("Sort");

        var layout = new FormLayout();
        layout.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("40em", 2),
                new ResponsiveStep("60em", 4));
        layout.add(search, category, without, sort);
        layout.setColspan(search, 3);

        add(new H3("Three steps, one colspan"),
                new Paragraph("Search asks for 3 columns. Read its span at 1200, 700 and 400 pixels."),
                layout);
        setWidthFull();
    }
}
