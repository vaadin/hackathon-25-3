package com.vaadin.bakery.diagnostics;

import com.vaadin.bakery.ordering.OrderQueryCounter;
import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
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

    private final PlatformEventRecorder recorder;
    private final OrderQueryCounter orderCounter;
    private final Div panels = new Div();

    public DiagnosticsView(PlatformEventRecorder recorder, OrderQueryCounter orderCounter) {
        this.recorder = recorder;
        this.orderCounter = orderCounter;
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
