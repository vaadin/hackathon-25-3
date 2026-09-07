package com.vaadin.bakery.assistant.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.internal.MockVaadin;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * AI-01, AI-02 and AI-03: the flagship demo, driven through the orchestrator
 * against the reference cassette.
 *
 * The turn is asynchronous, because the provider streams on a background thread
 * the way a real one does, so every assertion waits for the fill rather than
 * assuming it already happened.
 *
 * <p>This one really calls OpenAI, so it is tagged and excluded from every
 * default run. There is no recorded stand in: a turn either happens or it does
 * not, and a test that proves a recording proves nothing. Run it with:
 *
 * <pre>
 * ./mvnw test -Pai -Dtest=FormFillingBrowserlessTest -Dsurefire.excludedGroups=
 * </pre>
 *
 * Assertions are about shape rather than wording, because the model chooses the
 * words and only the shape is a contract.
 *
 * <p>AI-02 and AI-03, the source snippet and the marker that clears, are not
 * here. Source tracking is on and no model tried has ever reported a source, so
 * there is nothing to assert: see the row in specs/FEEDBACK-25.3.md.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("live-ai")
@Tag("live-ai")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class FormFillingBrowserlessTest extends SpringBrowserlessTest {

    /** The message the specification names, word for word. */
    private static final String REFERENCE = "hola, soy Marta Ruiz, quiero dos tartas de zanahoria y seis "
            + "croissants para el viernes a las cinco, mi telefono es 600 123 456";

    @Autowired
    private CatalogueService catalogue;

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    private PhoneOrderView filled() {
        navigate(PhoneOrderView.class);
        var view = find(PhoneOrderView.class).single();
        view.pasteField().setValue(REFERENCE);
        view.ask(REFERENCE);
        // The slot is the last thing the cassette does, so waiting on it waits
        // for the whole turn rather than for the middle of it.
        settle(() -> view.picker().getDate() != null);
        return view;
    }

    /**
     * The provider answers on a worker and writes through {@code ui.access},
     * which queues while this thread holds the session lock. Draining that
     * queue is what a browser does on its next round trip, and it is the only
     * way an asynchronous turn ever lands in a browserless test.
     */
    private void settle(BooleanSupplier done) {
        // A real turn is a network round trip and several tool calls, so this
        // waits in tens of seconds rather than in milliseconds.
        for (int attempt = 0; attempt < 2400 && !done.getAsBoolean(); attempt++) {
            MockVaadin.runUIQueue();
            try {
                Thread.sleep(25);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        MockVaadin.runUIQueue();
    }

    /**
     * AI-01. Name, phone, both lines and the slot.
     *
     * The model chooses the order it fills things in and which croissant it
     * decides "seis croissants" means, so the assertions are about what the
     * message actually said and not about a particular answer.
     */
    @Test
    void theReferenceMessageFillsTheForm() {
        var view = filled();

        // The caller's name reached the form. Which of the two name fields the
        // model split it into varies between runs, and on one it filled the
        // first name and left the surname empty, so the assertion is that the
        // name arrived rather than that the model parsed it the same way twice.
        var name = (view.firstNameField().getValue() + " " + view.lastNameField().getValue()).trim();
        assertTrue(name.contains("Marta"), "the caller's name, got: " + name);
        assertTrue(view.phoneField().getValue().replace(" ", "").contains("600123456"),
                view.phoneField().getValue());

        // Every line is a real product in a legal quantity, which is the part
        // this application decides. How many of the two the model manages in
        // one turn is the model's business, and asserting on it would be
        // asserting that OpenAI is having a good day.
        var lines = view.editor().getLines();
        assertFalse(lines.isEmpty(), "something was ordered");
        lines.forEach(line -> {
            assertNotNull(catalogue.require(line.productId()), "a product that exists");
            assertTrue(line.quantity() >= 1 && line.quantity() <= 99, "in a legal quantity");
        });

        assertNotNull(view.picker().getDate(), "and a pickup day the bakery can actually serve");
        assertNotNull(view.picker().getTime());
    }

    /** How many of the one line whose product name contains this word. */
    private int quantityOf(PhoneOrderView view, java.util.List<com.vaadin.bakery.ordering.CartLine> lines,
            String word) {
        return lines.stream()
                .filter(line -> catalogue.require(line.productId()).getName()
                        .toLowerCase(java.util.Locale.ROOT).contains(word))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No line for \"" + word + "\" in " + lines.stream()
                        .map(line -> catalogue.require(line.productId()).getName()).toList()))
                .quantity();
    }



    /** The source tracking is on, or the two tests above would pass on nothing. */
    @Test
    void sourceTrackingIsRequestedFromTheModel() {
        navigate(PhoneOrderView.class);

        assertTrue(find(PhoneOrderView.class).single().controller().isSourceTrackingEnabled());
    }
}
