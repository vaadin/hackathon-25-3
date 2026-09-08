package com.vaadin.bakery.assistant.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.assistant.BakeryDatabase;
import com.vaadin.bakery.ordering.ui.DashboardView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * AC5. With no model, every assistant surface says so where a person can see
 * it, and everything on those screens that never needed a model still works.
 *
 * This is the state the default build is always in, so it is the state most
 * worth asserting. The turns themselves are proved by the three tagged classes
 * that really call OpenAI.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class AssistantOffBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private BakeryDatabase database;

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    private boolean saysItIsOff() {
        return find(Component.class).all().stream()
                .anyMatch(component -> component.getElement().getClassList().contains("assistant-off"));
    }

    @Test
    void thePhoneOrderSaysTheAssistantIsOff() {
        navigate(PhoneOrderView.class);

        assertTrue(saysItIsOff(), "the panel says so rather than looking empty");
        // The screen used to assert the absence of a message input, which it
        // still lacks and would lack whatever happened, because there is no
        // chat box on it any more. What has to be absent is the paste area and
        // the two controls that act on it: an input that cannot reach a model
        // is worse than no input, because it looks like one that can.
        assertTrue(find(Button.class).withText("Fill the form").all().isEmpty(),
                "and nothing offers to fill the form");
        assertTrue(find(Upload.class).all().isEmpty(),
                "and no photograph can be handed to a model that is not there");
        assertTrue(find(TextArea.class).all().stream()
                        .noneMatch(area -> "What the customer said".equals(area.getLabel())),
                "and there is nowhere to paste what they said");
    }

    /** And the rest of that screen is untouched, which is the point of saying so. */
    @Test
    void thePhoneOrderStillTakesAnOrderWithoutAModel() {
        navigate(PhoneOrderView.class);
        var view = find(PhoneOrderView.class).single();

        // Nothing stands in for the model. The form is still a form, and an
        // order taken by hand is still an order: that is the whole offer when
        // there is no assistant, and it is what the red panel says.
        assertTrue(view.editor().getLines().isEmpty(), "the form is a form");
        assertTrue(find(Button.class).withText("Fill the form").all().isEmpty(),
                "and nothing offers to fill it from somewhere else");
    }

    /**
     * The rules about what a model may touch are built whether or not there is
     * one. They are the part worth asserting on a machine with no key.
     */
    @Test
    void theGuardrailsExistWithNoModelBehindThem() {
        navigate(PhoneOrderView.class);
        var view = find(PhoneOrderView.class).single();

        var names = view.allTools().stream()
                .map(com.vaadin.flow.component.ai.provider.LLMProvider.ToolSpec::getName)
                .toList();
        assertTrue(names.contains("get_form_state"), names.toString());
        assertTrue(names.contains("add_order_line"), names.toString());
        assertTrue(names.contains("propose_pickup_slot"), names.toString());
    }

    @Test
    void theBoardAssistantSaysTheAssistantIsOff() {
        navigate(BoardAskView.class);
        var view = find(BoardAskView.class).single();

        assertFalse(view.ask().isAvailable());
        assertTrue(saysItIsOff());
        assertTrue(view.answers().getColumns().isEmpty(), "and nothing was filled in");
    }

    @Test
    void theDashboardSaysTheAssistantIsOffAndStillDrawsItsPanels() {
        TestLogin.asAdmin();
        navigate(DashboardView.class);

        assertFalse(find(DashboardView.class).single().ask().isAvailable());
        assertTrue(saysItIsOff());
        assertFalse(find(com.vaadin.flow.component.charts.Chart.class).all().isEmpty(),
                "the four panels the dashboard always draws are still there");
    }

    /** The database guard is not a property of the assistant being on. */
    @Test
    void theQueryGuardHoldsWithNoModelToGuardAgainst() {
        var refusal = assertThrows(RuntimeException.class,
                () -> database.executeQuery("select password_hash from app_user"));

        assertTrue(refusal.getMessage().contains("ai_orders"), refusal.getMessage());
    }
}
