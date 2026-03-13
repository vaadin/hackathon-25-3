package com.vaadin.example.sightseeing.views.it;

import static org.junit.jupiter.api.Assertions.assertTrue;
//FIXME: static assertEquals import doesn't work

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.textfield.testbench.PasswordFieldElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.BrowserTest;

public class LoginViewIT extends AbstractIT {

    @BrowserTest
    public void testLogin() {
        List<WebElement> mapElements = findElements(By.tagName("vaadin-map"));
        Assertions.assertEquals(0, mapElements.size(),
                "There should be no map element on login view");

        // Find the first button (<vaadin-button>) on the page
        ButtonElement button = $(ButtonElement.class).get(0);

        String buttonText = button.getText();
        assertTrue(buttonText.contains("Log in"),
                "Unexpected button text: " + buttonText);

        // Enter login credentials
        $(TextFieldElement.class).single().sendKeys("admin");
        $(PasswordFieldElement.class).single().sendKeys("admin");

        button.click();

        waitUntil(ExpectedConditions
                .presenceOfElementLocated(By.tagName("vaadin-map")));
    }

}
