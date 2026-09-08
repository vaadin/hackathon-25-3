package com.vaadin.bakery.diagnostics;

import com.vaadin.bakery.ordering.OrderQueryCounter;
import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.Duration;
import java.time.Instant;

/**
 * What the platform is doing, from inside.
 *
 * Everything here comes from the 25.3 service event bus, which is free
 * platform, so this view works with no licence and no monitoring backend. It
 * answers the questions a generic tool cannot: which view is holding the
 * session lock, how chatty a screen is, and how many data provider queries a
 * page actually costs.
 */
@Route("admin/diagnostics")
@PageTitle("Diagnostics")
@Menu(order = 30, title = "Diagnostics", icon = "vaadin:dashboard")
@RolesAllowed(Role.ADMIN_NAME)
public class DiagnosticsView extends VerticalLayout {

    /**
     * How to start the kit half, in both places somebody would start it.
     *
     * Commands rather than prose, so they are constants rather than bundle
     * entries: a shell command is the same in every language, and a bundle is
     * the one place where somebody would helpfully translate a flag and break
     * it. The sentences around them are translated, as everything a person
     * reads should be.
     */
    private static final String DEV_COMMAND = "./mvnw spring-boot:run -Pobservability -Dspring-boot.run.profiles=observability";
    private static final String PROD_COMMAND = "./mvnw package -Pobservability -Pproduction\n"
            + "java -jar target/bakery-*.jar --spring.profiles.active=observability";
    private static final String DASHBOARD_COMMAND = "docker compose up -d prometheus grafana\n"
            + "open http://localhost:3000/d/bakery-vaadin";

    private final PlatformEventRecorder recorder;
    private final OrderQueryCounter orderCounter;
    private final ObservabilityStatus kit;
    private final Div panels = new Div();

    public DiagnosticsView(PlatformEventRecorder recorder, OrderQueryCounter orderCounter,
            ObservabilityStatus kit) {
        this.recorder = recorder;
        this.orderCounter = orderCounter;
        this.kit = kit;
        addClassName("diagnostics");

        panels.addClassName("diagnostics__panels");
        var refresh = Translations.bindText(new Button("", event -> render()), "diagnostics.refresh");
        var reset = Translations.bindText(new Button("", event -> {
            recorder.reset();
            orderCounter.reset();
            render();
        }), "diagnostics.reset");

        var deferred = Translations.bindText(new Button("", event -> scheduleDeferredCheck()),
                "diagnostics.deferred");

        add(Translations.bindText(new H2(), "diagnostics.title"),
                Translations.bindText(new Paragraph(), "diagnostics.subtitle"),
                new Div(refresh, reset, deferred), panels);
        // Every counter carries a translated label, so the panels are rebuilt
        // when the language changes as well as when somebody presses refresh.
        Translations.onLocale(this, this::render);
    }

    /**
     * A deferred callback with no push connection, which is what a background
     * job needs. If the UI has gone by the time it fires, the platform warns
     * about an undelivered invocation and the counter below shows it.
     */
    private void scheduleDeferredCheck() {
        getUI().ifPresent(ui -> {
            var scheduledAt = Instant.now();
            ui.triggerAfter(Duration.ofSeconds(2), () -> {
                var silence = Duration.between(ui.getLastUpdateSentTimestamp(), Instant.now());
                Notification.show(getTranslation("diagnostics.deferred.done",
                        Duration.between(scheduledAt, Instant.now()).toMillis(), silence.toMillis()));
                render();
            });
            Notification.show(getTranslation("diagnostics.deferred.scheduled"));
        });
    }

    private void render() {
        render(getLocale());
    }

    private void render(java.util.Locale locale) {
        panels.removeAll();
        panels.add(panel(getTranslation(locale, "diagnostics.locks"),
                row(getTranslation(locale, "diagnostics.locks.requested"), recorder.lockRequests()),
                row(getTranslation(locale, "diagnostics.locks.acquired"), recorder.lockAcquisitions()),
                row(getTranslation(locale, "diagnostics.locks.released"), recorder.lockReleases()),
                row(getTranslation(locale, "diagnostics.locks.longest"), recorder.longestLockMillis())));

        panels.add(panel(getTranslation(locale, "diagnostics.rpc"),
                row(getTranslation(locale, "diagnostics.rpc.invocations"), recorder.rpcInvocations()),
                row(getTranslation(locale, "diagnostics.rpc.undelivered"), recorder.undeliveredInvocations())));

        panels.add(panel(getTranslation(locale, "diagnostics.data"),
                row(getTranslation(locale, "diagnostics.data.count"), recorder.dataCountQueries()),
                row(getTranslation(locale, "diagnostics.data.fetch"), recorder.dataFetchQueries()),
                row(getTranslation(locale, "diagnostics.data.board"), orderCounter.queries()),
                row(getTranslation(locale, "diagnostics.data.expensiveColumn"),
                        orderCounter.expensiveColumnCalls())));

        var byCaller = new Table();
        byCaller.setCaptionText(getTranslation(locale, "diagnostics.data.byCaller"));
        byCaller.addHeaderRow(getTranslation(locale, "diagnostics.data.caller"),
                getTranslation(locale, "diagnostics.data.fetch"));
        recorder.fetchesByCaller().entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> byCaller.addRowWithHeader(shorten(entry.getKey()),
                        String.valueOf(entry.getValue())));
        var callerPanel = new Div(byCaller);
        callerPanel.addClassNames("panel", "diagnostics__panel");
        panels.add(callerPanel);

        panels.add(kitPanel(locale));
    }

    /**
     * The kit half: where its endpoints are, or how to start it.
     *
     * This view is the free half and it works in every build, so the reader is
     * standing in the one place where the other half's absence is worth
     * explaining. When the kit is running these are three links; when it is not
     * they are the two commands that start it, and the switch that is off.
     *
     * The links open in a new tab and ask for credentials. The actuator has a
     * security chain of its own with HTTP Basic, because a scraper cannot use a
     * login form, and a browser asked for Basic shows its own prompt: this
     * screen's session does not carry into it.
     */
    private Div kitPanel(java.util.Locale locale) {
        var panel = new Div();
        panel.addClassNames("panel", "diagnostics__panel", "diagnostics__kit");
        panel.add(new H3(getTranslation(locale, "diagnostics.kit")));

        if (kit.isActive()) {
            panel.add(new Paragraph(getTranslation(locale, "diagnostics.kit.on")));
            panel.add(link(ObservabilityStatus.METRICS_PATH, getTranslation(locale, "diagnostics.kit.metrics")),
                    link(ObservabilityStatus.INSIGHTS_PATH, getTranslation(locale, "diagnostics.kit.insights")),
                    link(ObservabilityStatus.HEALTH_PATH, getTranslation(locale, "diagnostics.kit.health")));
            panel.add(note(getTranslation(locale, "diagnostics.kit.credentials")));
        } else {
            panel.add(new Paragraph(getTranslation(locale, "diagnostics.kit.off",
                    getTranslation(locale, kit.reason().translationKey()))));
            panel.add(new Span(getTranslation(locale, "diagnostics.kit.dev")), new Pre(DEV_COMMAND),
                    new Span(getTranslation(locale, "diagnostics.kit.prod")), new Pre(PROD_COMMAND));
            panel.add(note(getTranslation(locale, "diagnostics.kit.profileNote")));
        }

        // The graphs, in both states, because the endpoints being reachable is
        // not the same as anybody having drawn them, and this is the screen
        // where somebody is looking for that.
        panel.add(new H3(getTranslation(locale, "diagnostics.kit.dashboard")), new Pre(DASHBOARD_COMMAND),
                note(getTranslation(locale, "diagnostics.kit.dashboardNote")));

        // What the three programs are, and then the three questions somebody
        // actually asks about the middle one: how a container reaches an
        // application that is not in it, how it knows where to look, and
        // whether it has to identify itself. Four short lines, because a
        // paragraph on a screen is a paragraph nobody reads.
        panel.add(note(getTranslation(locale, "diagnostics.kit.pieces")),
                note(getTranslation(locale, "diagnostics.kit.reach")),
                note(getTranslation(locale, "diagnostics.kit.url")),
                note(getTranslation(locale, "diagnostics.kit.auth")));
        return panel;
    }

    private static Paragraph note(String text) {
        var note = new Paragraph(text);
        note.addClassName("diagnostics__kit-note");
        return note;
    }

    /** A link out of the application, so it opens beside it rather than over it. */
    private static Anchor link(String href, String text) {
        var anchor = new Anchor(href, text);
        anchor.setTarget("_blank");
        anchor.addClassName("diagnostics__kit-link");
        return anchor;
    }

    private static String shorten(String className) {
        int last = className.lastIndexOf('.');
        return last < 0 ? className : className.substring(last + 1);
    }

    private Div panel(String title, Div... rows) {
        var panel = new Div();
        panel.addClassNames("panel", "diagnostics__panel");
        panel.add(new H3(title));
        for (Div row : rows) {
            panel.add(row);
        }
        return panel;
    }

    private Div row(String label, long value) {
        var row = new Div(new Span(label), new Span(String.valueOf(value)));
        row.addClassName("diagnostics__row");
        return row;
    }
}
