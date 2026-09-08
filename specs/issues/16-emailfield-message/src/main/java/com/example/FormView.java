package com.example;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** The same property bound twice: once to a text field, once to an email field. */
@Route("")
@AnonymousAllowed
public class FormView extends VerticalLayout {

    final TextField asText = new TextField("Email in a TextField");
    final EmailField asEmail = new EmailField("Email in an EmailField");
    final BeanValidationBinder<Person> textBinder = new BeanValidationBinder<>(Person.class);
    final BeanValidationBinder<Person> emailBinder = new BeanValidationBinder<>(Person.class);

    public FormView() {
        textBinder.bind(asText, "email");
        textBinder.setBean(new Person());
        emailBinder.bind(asEmail, "email");
        emailBinder.setBean(new Person());
        add(asText, asEmail);
    }
}
