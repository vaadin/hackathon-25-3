package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * The four assertions below are the ones somebody writes first, and three of
 * them fail. Run with `mvn test`.
 */
@SpringBootTest(classes = Application.class)
class FindTest extends SpringBrowserlessTest {

    @Test
    void everyCheckboxOnTheScreenIsFound() {
        navigate(FindView.class);

        // The browser shows five checkboxes: one in the view, two in the grid
        // cells, one in the grid's context menu. The finder sees one.
        assertEquals(4, find(Checkbox.class).all().size(),
                "one in the view, two rendered into cells, one in the context menu");
    }

    @Test
    void theColumnHeaderComponentIsFound() {
        navigate(FindView.class);

        assertEquals(1, find(Button.class).all().size(), "the button set as a column header");
    }
}
