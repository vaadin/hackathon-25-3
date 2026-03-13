package com.vaadin.example.sightseeing.views.it;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.chrome.ChromeDriver;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.textfield.testbench.PasswordFieldElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.BrowserTestBase;

public class AbstractIT extends BrowserTestBase {

    @BeforeEach
    public void setup() throws Exception {
        // Create a new browser instance
        setDriver(new ChromeDriver());
        // Open the application
        getDriver().get("http://localhost:8080/");
    }

    public void loginAsAdmin() {
        $(TextFieldElement.class).single().sendKeys("admin");
        $(PasswordFieldElement.class).single().sendKeys("admin");
        $(ButtonElement.class).get(0).click();
    }

    public void loginAsUser() {
        $(TextFieldElement.class).single().sendKeys("user");
        $(PasswordFieldElement.class).single().sendKeys("user");
        $(ButtonElement.class).get(0).click();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // close the browser instance when all tests are done
        getDriver().quit();
    }

}
