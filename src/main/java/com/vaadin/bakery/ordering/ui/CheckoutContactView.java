package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.ui.Fields;
import com.vaadin.bakery.base.validation.OnSubmit;
import com.vaadin.bakery.ordering.CheckoutState;
import com.vaadin.bakery.people.Customer;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.InputMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.validation.groups.Default;

/**
 * Step one, and the step that decides whether there is a way to reach the
 * customer.
 *
 * Two groups, two moments, which is the whole point of validation groups. While
 * somebody types, the default group checks the shape of what they have written
 * and says nothing about what is still empty: an email half typed is not an
 * error yet. Moving on asks the other question, with the submit group, which is
 * whether this is a customer the bakery can actually call: a name, an email and
 * a telephone number.
 *
 * It used to ask only the draft question here and the submit question at the
 * very end, on the review step, where the answer was a Place button that would
 * not light up. So an empty form walked through two more steps and then stopped
 * with nothing to click and no field to look at. The rule now is that a step
 * refuses at the point where the missing thing can be typed.
 */
@Route("checkout/contact")
@RouteParent(CartView.class)
@PageTitle("Your details")
@AnonymousAllowed
public class CheckoutContactView extends VerticalLayout {

    private final BeanValidationBinder<Customer> binder = new BeanValidationBinder<>(Customer.class);

    public CheckoutContactView(CheckoutState state) {
        addClassName("checkout-view");
        add(CheckoutSteps.breadcrumbs(), Translations.bindText(new H2(), "checkout.contact.title"));

        var firstName = new TextField();
        Translations.bind(firstName, firstName::setLabel, "checkout.firstName");
        var lastName = new TextField();
        Translations.bind(lastName, lastName::setLabel, "checkout.lastName");
        var email = Fields.email("checkout.email");
        var phone = new TextField();
        Translations.bind(phone, phone::setLabel, "checkout.phone");
        phone.setInputMode(InputMode.TEL);
        var note = new TextArea();
        Translations.bind(note, note::setLabel, "checkout.note");
        note.setMaxLength(500);

        firstName.setValueChangeMode(ValueChangeMode.LAZY);
        lastName.setValueChangeMode(ValueChangeMode.LAZY);
        email.setValueChangeMode(ValueChangeMode.LAZY);
        phone.setValueChangeMode(ValueChangeMode.LAZY);

        // Draft rules while typing, submit rules to decide whether to let go.
        binder.setValidationGroups(Default.class);
        binder.bind(firstName, "firstName");
        binder.bind(lastName, "lastName");
        binder.bind(email, "email");
        binder.bind(phone, "phone");
        binder.setBean(state.draft());

        note.setValue(state.note().peek());
        note.addValueChangeListener(event -> state.note().set(event.getValue()));

        binder.addValueChangeListener(event -> state.contactValid().set(isSubmitReady()));
        state.contactValid().set(isSubmitReady());

        var form = new FormLayout(firstName, lastName, email, phone, note);
        form.setColspan(note, 2);

        // Forward asks for everything this step owes the order. The binder puts
        // the message on each field that is not right; this also focuses the
        // first of them, because a form of five fields where one is red is a
        // form somebody has to hunt through.
        var next = Translations.bindText(new Button("", event -> {
            var outcome = binder.validate(Default.class, OnSubmit.class);
            if (outcome.isOk()) {
                getUI().ifPresent(ui -> ui.navigate("checkout/slot"));
                return;
            }
            outcome.getFieldValidationErrors().stream()
                    .map(BindingValidationStatus::getField)
                    .filter(Focusable.class::isInstance)
                    .map(field -> (Focusable<?>) field)
                    .findFirst()
                    .ifPresent(Focusable::focus);
        }), "checkout.next");
        next.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Back never validates. Somebody stepping out to go and check their own
        // phone number should not be stopped, and nothing is lost by letting
        // them: the values live in the session, not in this view.
        var back = Translations.bindText(new Button("",
                event -> getUI().ifPresent(ui -> ui.navigate("cart"))), "checkout.back");

        var actions = new HorizontalLayout(back, next);
        add(form, actions);
    }

    private boolean isSubmitReady() {
        return binder.validate(Default.class, OnSubmit.class).isOk();
    }
}
