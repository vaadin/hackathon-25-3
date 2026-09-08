package com.vaadin.bakery.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.ui.OrderBoardView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.data.provider.Query;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

/**
 * OBS-02 and OBS-03. The diagnostics view is free platform, so it works in the
 * default build, and it is what makes the hidden column claim checkable rather
 * than merely stated.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "admin@bakery.test", roles = { "ADMIN" })
class DiagnosticsBrowserlessTest extends SpringBrowserlessTest {

    @org.junit.jupiter.api.BeforeEach
    void signIn() {
        // After the browserless environment is up, see TestLogin for why.
        TestLogin.asAdmin();
    }

    @Autowired
    private PlatformEventRecorder recorder;

    @Autowired
    private ObservabilityStatus kit;

    @Test
    void theViewRendersItsPanelsWithoutAnyLicence() {
        navigate(DiagnosticsView.class);

        var titles = find(H3.class).all().stream().map(H3::getText).toList();
        assertTrue(titles.contains("Session locks"), titles.toString());
        assertTrue(titles.contains("Client traffic"), titles.toString());
        assertTrue(titles.contains("Data provider queries"), titles.toString());
    }

    @Test
    void dataProviderQueriesAreCountedThroughTheServiceEventBus() {
        recorder.reset();
        navigate(OrderBoardView.class);

        @SuppressWarnings("unchecked")
        Grid<Order> grid = (Grid<Order>) find(Grid.class).single();
        grid.getDataProvider().fetch(new Query<>(0, 50, List.of(), null, null)).count();

        assertTrue(recorder.dataFetchQueries() > 0,
                "the platform reported the fetch on the event bus");
    }

    /**
     * The breakdown names the component that fetched, which is what the 25.3
     * fetch events carry and the reason they take a component at all.
     *
     * It used to assert that the caller was one of our own classes, and it
     * passed while being wrong: the name came from walking the stack for the
     * first `com.vaadin.bakery` frame, and the first such frame is the listener
     * doing the walking, so every fetch in the application was attributed to
     * `PlatformEventRecorder`. A test that asserts a prefix cannot tell that
     * apart from an answer, which is why this one now names the component.
     */
    @Test
    void theRecorderKnowsWhichComponentAskedForTheData() {
        recorder.reset();
        navigate(OrderBoardView.class);

        @SuppressWarnings("unchecked")
        Grid<Order> grid = (Grid<Order>) find(Grid.class).single();
        grid.getDataProvider().fetch(new Query<>(0, 50, List.of(), null, null)).count();

        assertTrue(recorder.fetchesByCaller().containsKey("Grid"),
                "the breakdown names the grid that fetched, got " + recorder.fetchesByCaller());
        assertTrue(recorder.fetchesByCaller().keySet().stream()
                .noneMatch(caller -> caller.contains("Recorder")),
                "and not the listener that recorded it, got " + recorder.fetchesByCaller());
    }

    /**
     * The kit half is not in the default build, so the panel that would link to
     * it explains how to start it instead.
     *
     * This is the one screen where saying so is useful: it is the free half of
     * observability, it works in every build, and its reader is the person
     * looking for the other half. Naming the switch matters as much as the
     * commands: "it does not work" sends somebody to the wrong one of three.
     */
    @Test
    void theKitPanelExplainsHowToStartWhatIsNotRunning() {
        navigate(OrderBoardView.class);
        navigate(DiagnosticsView.class);

        assertFalse(kit.isActive(), "the default build carries no kit");
        assertEquals(ObservabilityStatus.Reason.NOT_IN_BUILD, kit.reason(),
                "and the reason is the build rather than a property");

        var panel = find(com.vaadin.flow.component.html.Div.class).all().stream()
                .filter(div -> div.getClassNames().contains("diagnostics__kit"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the view has a kit panel"));
        var text = panel.getChildren()
                .flatMap(child -> child.getChildren().count() == 0
                        ? java.util.stream.Stream.of(child)
                        : java.util.stream.Stream.concat(java.util.stream.Stream.of(child), child.getChildren()))
                .map(child -> child.getElement().getText())
                .reduce("", (all, part) -> all + " " + part);

        assertTrue(text.contains("observability profile"), "it names the switch that is off: " + text);
        assertTrue(text.contains("-Pobservability"), "and the development command: " + text);
        assertTrue(text.contains("--spring.profiles.active=observability"),
                "and the production one, which is the Spring profile rather than the Maven one: " + text);
        assertTrue(panel.getChildren().noneMatch(child -> child instanceof com.vaadin.flow.component.html.Anchor),
                "and it links to nothing, because there is nothing to link to");
    }

    @Test
    void countersCanBeReset() {
        navigate(OrderBoardView.class);
        recorder.reset();

        assertEquals(0, recorder.dataFetchQueries());
        assertEquals(0, recorder.rpcInvocations());
    }
}
