package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.browserless.SpringBrowserlessTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * The same constraint, the same bad value, two fields. One reports the message
 * and the other reports nothing. Run with `mvn test`: the second assertion is
 * the finding, and it passes, which is the problem.
 */
@SpringBootTest(classes = Application.class)
class EmailFieldMessageTest extends SpringBrowserlessTest {

    @Test
    void theTextFieldReportsTheConstraintAndTheEmailFieldDoesNot() {
        navigate(FormView.class);
        var view = find(FormView.class).single();

        test(view.asText).setValue("not an address");
        test(view.asEmail).setValue("not an address");

        assertTrue(view.asText.isInvalid(), "both fields are invalid");
        assertTrue(view.asEmail.isInvalid());

        assertEquals("must be a well-formed email address", view.asText.getErrorMessage(),
                "the text field reports what the constraint said");
        assertEquals("", view.asEmail.getErrorMessage(),
                "and the email field reports nothing at all, which is the finding");
    }
}
