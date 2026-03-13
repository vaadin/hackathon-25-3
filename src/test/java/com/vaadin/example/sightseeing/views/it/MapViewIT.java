package com.vaadin.example.sightseeing.views.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
// FIXME: static assertEquals import doesn't work

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.example.sightseeing.data.generator.DataGenerator;
import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.dialog.testbench.DialogElement;
import com.vaadin.flow.component.grid.testbench.GridElement;
import com.vaadin.flow.component.map.configuration.Coordinate;
import com.vaadin.flow.component.map.testbench.MapElement;
import com.vaadin.testbench.BrowserTest;

public class MapViewIT extends AbstractIT {
    private Coordinate center = DataGenerator.CENTER;

    @BrowserTest
    public void loginAsUserAndTestDialog() {
        loginAsUser();
        waitUntil(ExpectedConditions
                .presenceOfElementLocated(By.tagName("vaadin-map")));

        assertFalse($(ButtonElement.class).exists(),
                "There shouldn't be any buttons on the user map view");

        // dialog is already present before the first click
        DialogElement dialog = $(DialogElement.class).single();
        assertFalse(dialog.isOpen(), "Dialog shouldn't be open yet");

        $(MapElement.class).single().clickAtCoordinates(center.getX(),
                center.getY());

        try {
            waitUntil(webDriver -> dialog.isOpen());
        } catch (TimeoutException e) {
            fail("Dialog wasn't opened");
        }
        assertTrue(dialog.getText().contains("Nearby:"),
                "Unexpected dialog contents: " + dialog.getText());

        // click outside the dialog
        new Actions(getDriver()).moveToElement(dialog)
                .moveByOffset(10 + dialog.getSize().getWidth() / 2,
                        10 + dialog.getSize().getHeight() / 2)
                .click().perform();
        try {
            waitUntil(webDriver -> !dialog.isOpen());
        } catch (TimeoutException e) {
            fail("Dialog didn't close");
        }
    }

    @BrowserTest
    public void loginAsAdminAndNavigateAway() {
        loginAsAdmin();
        waitUntil(ExpectedConditions
                .presenceOfElementLocated(By.tagName("vaadin-map")));

        assertTrue($(ButtonElement.class).exists(),
                "There should be buttons on the admin map view");

        ButtonElement button = $(ButtonElement.class).id("placesButton");
        Assertions.assertEquals("Places", button.getText());
        button.click();
        try {
            waitUntil(webDriver -> $(GridElement.class).exists());
        } catch (TimeoutException e) {
            fail("Failed to navigate away from map view");
        }

        assertFalse($(MapElement.class).exists(),
                "There shouldn't be a map on the places view");
    }

}
