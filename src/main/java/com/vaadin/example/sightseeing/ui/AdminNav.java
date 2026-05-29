package com.vaadin.example.sightseeing.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class AdminNav extends HorizontalLayout {

    public AdminNav(String current) {
        addClassName("admin-nav");

        // TODO: more accessible navigation
        Button mapButton = new Button("Map",
                e -> getUI().get().navigate("map"));
        mapButton.setVisible(!"map".equals(current));
        mapButton.setId("mapButton");

        Button nothingButton = new Button("Nothing",
                e -> getUI().get().navigate("nothing"));
        nothingButton.setVisible(!"nothing".equals(current));
        nothingButton.setId("nothingButton");

        Button somethingButton = new Button("Something",
                e -> getUI().get().navigate("something"));
        somethingButton.setVisible(!"something".equals(current));
        somethingButton.setId("somethingButton");

        Button placesButton = new Button("Places",
                e -> getUI().get().navigate("places"));
        placesButton.setVisible(!"places".equals(current));
        placesButton.setId("placesButton");

        Button tagsButton = new Button("Tags",
                e -> getUI().get().navigate("tags"));
        tagsButton.setVisible(!"tags".equals(current));
        tagsButton.setId("tagsButton");

        add(mapButton, nothingButton, somethingButton, placesButton,
                tagsButton);
        expand(mapButton, nothingButton, somethingButton, placesButton,
                tagsButton);
        setWidthFull();
    }
}
