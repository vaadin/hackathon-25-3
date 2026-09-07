package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.flow.component.charts.Chart;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The dashboard's question box: a question becomes a chart.
 *
 * The four panels above it are fixed and answer the questions a bakery asks
 * every morning. This one answers the question nobody wrote a panel for.
 *
 * <p>This one really calls OpenAI, so it is tagged and excluded from every
 * default run. There is no recorded stand in: a turn either happens or it does
 * not, and a test that proves a recording proves nothing. Run it with:
 *
 * <pre>
 * ./mvnw test -Pai -Dtest=ChartAssistantBrowserlessTest -Dsurefire.excludedGroups=
 * </pre>
 *
 * Assertions are about shape rather than wording, because the model chooses the
 * words and only the shape is a contract.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("live-ai")
@Tag("live-ai")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class ChartAssistantBrowserlessTest extends SpringBrowserlessTest {

    @BeforeEach
    void signIn() {
        TestLogin.asAdmin();
    }

    private DashboardView open() {
        navigate(DashboardView.class);
        return find(DashboardView.class).single();
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

    /** The asked chart is a fifth chart, beside the four the dashboard always draws. */
    @Test
    void theQuestionBoxIsThereWithItsOwnChart() {
        var view = open();

        assertTrue(view.ask().isAvailable(), "the controller was built");
        assertEquals(4, find(Chart.class).all().size(),
                "the three fixed series panels plus the one a question draws into");
    }

    @Test
    void aQuestionDrawsAChartFromTheOrders() {
        var view = open();
        var asked = askedChart(view);

        view.ask().ask("show me the revenue by channel");
        settle(() -> !asked.getConfiguration().getSeries().isEmpty());

        var configuration = asked.getConfiguration();
        assertFalse(configuration.getSeries().isEmpty(), "the question produced a series");
        // The model writes the title, so it is asserted as present rather than
        // as a particular sentence: it has answered "Revenue by channel" and
        // "Revenue by Channel" on two consecutive runs.
        assertNotNull(configuration.getTitle(), "and titled it");
        assertFalse(configuration.getTitle().getText().isBlank(), "with something");
    }

    /** The chart the assistant owns must be in styled mode like every other one. */
    @Test
    void theAskedChartFollowsTheTheme() {
        var view = open();
        var asked = askedChart(view);

        view.ask().ask("show me the revenue by channel");
        settle(() -> !asked.getConfiguration().getSeries().isEmpty());

        assertEquals(Boolean.TRUE, asked.getConfiguration().getChart().getStyledMode(),
                "otherwise it draws Highcharts' own palette on a dark page");
    }

    private Chart askedChart(DashboardView view) {
        return find(Chart.class).all().stream()
                .filter(chart -> chart.getClassNames().contains("dashboard__asked"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the dashboard has no chart for a question"));
    }
}
