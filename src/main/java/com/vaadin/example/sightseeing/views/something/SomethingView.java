package com.vaadin.example.sightseeing.views.something;

import com.vaadin.flow.component.checkbox.Checkbox;

import com.vaadin.flow.component.html.Paragraph;

import com.vaadin.flow.component.charts.Chart;

import com.vaadin.flow.component.charts.model.ChartType;

import com.vaadin.flow.component.charts.model.DataSeries;

import com.vaadin.flow.component.charts.model.DataSeriesItem;

import java.util.List;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;

import com.vaadin.flow.component.textfield.TextArea;

import com.vaadin.flow.component.formlayout.FormLayout;

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
Chart chart = new Chart(ChartType.PIE);
chart.setMinHeight("400px");
chart.getConfiguration().setTitle("Sales 2023");
chart.getConfiguration().getxAxis().setCategories("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
chart.getConfiguration().getyAxis().setTitle("Euro (€)");
DataSeries dataseries = new DataSeries("Sales");
DataSeriesItem dataseriesitem = new DataSeriesItem();
dataseriesitem.setName("Product A");
dataseriesitem.setY(42112);
DataSeriesItem dataseriesitem2 = new DataSeriesItem();
dataseriesitem2.setName("Product B");
dataseriesitem2.setY(58698);
DataSeriesItem dataseriesitem3 = new DataSeriesItem();
dataseriesitem3.setName("Product C");
dataseriesitem3.setY(12276);
DataSeriesItem dataseriesitem4 = new DataSeriesItem();
dataseriesitem4.setName("Product D");
dataseriesitem4.setY(33202);
dataseries.setData(List.of(dataseriesitem, dataseriesitem2, dataseriesitem3, dataseriesitem4));
chart.getConfiguration().addSeries(dataseries);
TextArea textArea = new TextArea("Text area");
textArea.getStyle().setHeight("100%");
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
HorizontalLayout horizontallayout2 = new HorizontalLayout();
horizontallayout2.add(button4);
div2.add(horizontallayout2);
verticallayout.add(heading1, horizontallayout, div, button6, div2);
MultiSelectComboBox<LabelAndValue> multiSelect = new MultiSelectComboBox<>("Multi select");
multiSelect.setItems(new LabelAndValue("First", "first"), new LabelAndValue("Second", "second"), new LabelAndValue("Third", "third"), new LabelAndValue("Fourth", "fourth"));
multiSelect.setItemLabelGenerator(LabelAndValue::label);
Paragraph loremIpsumDolorSitAm2 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
loremIpsumDolorSitAm2.addClassNames(LumoUtility.FontSize.SMALL);
FormLayout formlayout = new FormLayout();
formlayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("320px", 2), new FormLayout.ResponsiveStep("500px", 3));
formlayout.getStyle().set("minWidth", "500px");
formlayout.addComponentAsFirst(multiSelect);
formlayout.add(chart);
formlayout.add(textArea);
verticallayout.add(loremIpsumDolorSitAm2);
verticallayout.add(formlayout);
Paragraph loremIpsumDolorSitAm = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
verticallayout.add(loremIpsumDolorSitAm);
Checkbox checkbox = new Checkbox("Checkbox");
FormLayout formlayout2 = new FormLayout();
formlayout2.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("320px", 2), new FormLayout.ResponsiveStep("500px", 3));
formlayout2.getStyle().set("minWidth", "500px");
formlayout2.add(checkbox);
Checkbox checkbox3 = new Checkbox("Checkbox");
Checkbox checkbox2 = new Checkbox("Checkbox");
formlayout2.add(checkbox3);
formlayout2.add(checkbox2);
verticallayout.add(formlayout2);
Paragraph loremIpsumDolorSitAm3 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
loremIpsumDolorSitAm3.addClassNames(LumoUtility.FontSize.SMALL);
verticallayout.add(loremIpsumDolorSitAm3);
Button button8 = new Button("Button");
button8.setThemeName("primary");
Button button7 = new Button("Button");
horizontallayout2.add(button8);
horizontallayout2.add(button7);
button7.setThemeName("tertiary");
H3 heading33 = new H3("Heading 3-3");
H3 heading32 = new H3("Heading 3-2");
div.add(heading31, heading32, heading33, button2, button3, button);
div.getStyle().setWidth("100%");
H2 heading21 = new H2("Heading 2-1");
H2 heading22 = new H2("Heading 2-2");
horizontallayout.add(heading21, heading22);
    }
public record LabelAndValue(String label, String value, boolean enabled) {

    LabelAndValue(String label, String value) {
        this(label, value, true);
    }
}
}
