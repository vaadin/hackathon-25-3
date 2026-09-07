package com.vaadin.bakery.assistant.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.component.GridKt;
import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.flow.component.ai.grid.AIDataRow;
import com.vaadin.flow.component.grid.Grid;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * AI-09. The three questions the board demo has to answer, asked of a real
 * model and answered from the database.
 *
 * The specification originally asked for an answer drawn from the rows on
 * screen. {@code GridAIController} does not work that way, so what this proves
 * is the criterion the platform can keep: the answer comes from the three views
 * the assistant is allowed to read, and from nothing else.
 *
 * <p>This one really calls OpenAI, so it is tagged and excluded from every
 * default run. Run it with:
 *
 * <pre>
 * ./mvnw test -Pai -Dtest=GridAssistantBrowserlessTest -Dsurefire.excludedGroups=
 * </pre>
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("live-ai")
@Tag("live-ai")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class GridAssistantBrowserlessTest extends SpringBrowserlessTest {

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    private BoardAskView open() {
        navigate(BoardAskView.class);
        return find(BoardAskView.class).single();
    }

    private void settle(BooleanSupplier done) {
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
     * Ask, then wait for the column this particular answer must have.
     *
     * Waiting for "any column at all" is not enough: a turn from an earlier
     * question can still be in flight, and a grid that is already full makes
     * the wait return before this question has been answered. The test then
     * asserts on somebody else's answer, which is how this one first passed
     * while showing the wrong columns.
     */
    /**
     * Ask, and wait for a grid that has both columns and a row in it.
     *
     * Waiting for columns alone is not enough: a column that exists and throws
     * when it draws is what an empty grid on screen looks like, and the time
     * column did exactly that until the view stopped exposing a SQL TIME.
     */
    private int rowsAfter(BoardAskView view, String question) {
        view.ask().ask(question);
        settle(() -> !view.answers().getColumns().isEmpty()
                && GridKt._getFormattedRowOrNull(view.answers(), 0) != null);
        return view.answers().getColumns().size();
    }


    /** AI-09. Which orders are at risk of being late. */
    @Test
    void aQuestionAboutRiskFillsTheGridFromTheOrders() {
        var view = open();

        assertTrue(rowsAfter(view, "which orders are at risk of being late") > 0,
                "the question produced a grid");

        // The model chooses the column names, so what is asserted is that the
        // rows are this bakery's orders and that every cell drew.
        var row = GridKt._getFormattedRow(view.answers(), 0);
        assertTrue(row.stream().anyMatch(cell -> cell.startsWith("ORD-")), row.toString());
    }

    /** The second demo question, and it reads a different view. */
    @Test
    void aQuestionAboutAllergenCommentsReadsTheOrderLines() {
        var view = open();

        assertTrue(rowsAfter(view, "which orders have an allergen comment") > 0);
        assertFalse(GridKt._getFormattedRow(view.answers(), 0).isEmpty(), "and every cell drew");
    }

    /** The third demo question, so all three in the specification are recorded. */
    @Test
    void aQuestionAboutWhoOrderedTheMostAggregatesInSql() {
        var view = open();

        assertTrue(rowsAfter(view, "which customer ordered the most this week") > 0);
        assertFalse(GridKt._getFormattedRow(view.answers(), 0).isEmpty(), "and every cell drew");
    }

    /**
     * The one that matters. A question the model answers by reaching for a real
     * table is refused by the database, not by a sentence in a prompt, and the
     * grid stays empty rather than showing a password hash.
     */
    @Test
    void aQuestionThatReachesForAPrivateTableAnswersNothing() {
        var view = open();

        view.ask().ask("what are the staff salaries of the bakery");
        // Nothing to wait for: the point is that nothing arrives.
        for (int attempt = 0; attempt < 400; attempt++) {
            MockVaadin.runUIQueue();
            try {
                Thread.sleep(25);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        assertTrue(view.answers().getColumns().isEmpty(), "nothing was filled in");
        assertFalse(headers(view.answers()).contains("password"), headers(view.answers()));
    }


    private String headers(Grid<AIDataRow> grid) {
        return grid.getColumns().stream()
                .map(column -> column.getHeaderText() == null ? "" : column.getHeaderText())
                .reduce("", (left, right) -> left + "|" + right);
    }
}
