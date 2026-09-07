package com.vaadin.bakery.base.ui;

import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.ordering.PickupClosureRepository;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * When each shop is open, and the days it is not. V16 in the use case list, and
 * the last thing a visitor checks before walking over.
 */
@Route("hours")
@PageTitle("Opening hours")
@Menu(order = 2, title = "Hours", icon = "vaadin:clock")
@AnonymousAllowed
public class OpeningHoursView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    /**
     * Short, because this one is a cell in a table rather than a sentence.
     * "Thursday 8 September" is the right shape for "pick it up at Bakery,
     * Thursday 8 September at 07:30", and the wrong one for a column.
     */
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, d MMM");

    public OpeningHoursView(PickupLocationRepository locations, PickupClosureRepository closures, Clock clock) {
        addClassName("hours-view");

        // Two panels side by side, not two bare tables stacked in a column.
        var openPanel = new Div();
        openPanel.addClassName("panel");
        var closedPanel = new Div();
        closedPanel.addClassName("panel");

        var grid = new Div(openPanel, closedPanel);
        grid.addClassName("page-grid");

        var title = Translations.bindText(new H2(), "hours.title");
        title.addClassName("page-block");
        add(title, Translations.bindText(new Paragraph(), "hours.lead"), grid);

        // Both tables are locale dependent the whole way down: the headings, the
        // weekday names and the closure dates. Rebuilding them costs less than
        // binding every cell, and they are small.
        var today = LocalDate.now(clock);
        Translations.onLocale(this, locale -> {
            openPanel.removeAll();
            openPanel.add(new H3(getTranslation(locale, "hours.open")), openingHours(locations, locale));
            closedPanel.removeAll();
            closedPanel.add(new H3(getTranslation(locale, "hours.closures")), closures(closures, today, locale));
        });
    }

    private Table openingHours(PickupLocationRepository locations, Locale locale) {
        var hours = new Table();
        hours.addHeaderRow(getTranslation(locale, "ordering.slot.location"), getTranslation(locale, "hours.from"),
                getTranslation(locale, "hours.to"), getTranslation(locale, "hours.closed"));
        locations.findByActiveTrueOrderByNameAsc().forEach(location -> hours.addRowWithHeader(location.getName(),
                location.getOpensAt().format(TIME), location.getClosesAt().format(TIME),
                location.getClosedWeekdays().stream()
                        .map(day -> day.getDisplayName(TextStyle.FULL, locale))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("-")));
        return hours;
    }

    private Table closures(PickupClosureRepository closures, LocalDate today, Locale locale) {
        var closed = new Table();
        closed.addHeaderRow(getTranslation(locale, "admin.closure.date"),
                getTranslation(locale, "admin.closure.reason"), getTranslation(locale, "ordering.slot.location"));
        closures.findByDateBetweenOrderByDateAsc(today, today.plusMonths(3)).forEach(closure ->
                closed.addRowWithHeader(closure.getDate().format(DAY.withLocale(locale)),
                        closure.getReason(),
                        closure.getLocation() == null ? getTranslation(locale, "admin.closure.everywhere")
                                : closure.getLocation().getName()));
        return closed;
    }
}
