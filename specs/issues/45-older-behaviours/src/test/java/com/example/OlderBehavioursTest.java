package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Two of the four, as tests. Both pass, and both passing is the finding. */
@SpringBootTest(classes = Application.class)
class OlderBehavioursTest extends SpringBrowserlessTest {

    @Test
    void navigatingToTheRouteYouAreAlreadyOnDoesNotRebuildTheView() {
        CountingView.BUILDS.set(0);

        navigate(CountingView.class);
        navigate(CountingView.class);

        assertEquals(1, CountingView.BUILDS.get(),
                "the second navigation is a no operation, so anything the constructor computed is stale");
    }

    @Test
    void aConstraintMessageComesFromHibernateValidatorInEnglish() {
        var binder = new BeanValidationBinder<>(Person.class);
        var field = new TextField();
        binder.bind(field, "name");
        binder.setBean(new Person());

        field.setValue("");
        binder.validate();

        assertEquals("must not be blank", field.getErrorMessage(),
                "in English, whatever the application's locale is, because the message is not from the i18n provider");
    }
}
