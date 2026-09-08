package com.vaadin.bakery.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void countersCanBeReset() {
        navigate(OrderBoardView.class);
        recorder.reset();

        assertEquals(0, recorder.dataFetchQueries());
        assertEquals(0, recorder.rpcInvocations());
    }
}
