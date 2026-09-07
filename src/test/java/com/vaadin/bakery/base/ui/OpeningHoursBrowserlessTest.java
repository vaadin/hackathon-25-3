package com.vaadin.bakery.base.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.html.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * V16: when we are open, and the days we are not. The last thing a visitor
 * checks before walking over, and the one page that has to be right even when
 * everything else is closed.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class OpeningHoursBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private PickupLocationRepository locations;

    @Test
    void everyActiveLocationIsListedWithItsHours() {
        navigate(OpeningHoursView.class);

        var tables = find(Table.class).all();
        assertEquals(2, tables.size(), "one table for the hours, one for the closures");

        var text = tables.getFirst().getElement().getTextRecursively();
        locations.findByActiveTrueOrderByNameAsc()
                .forEach(location -> assertTrue(text.contains(location.getName()),
                        location.getName() + " is missing from the hours table: " + text));
    }

    @Test
    void theClosureTableNamesTheReason() {
        navigate(OpeningHoursView.class);

        var closures = find(Table.class).all().get(1).getElement().getTextRecursively();
        assertTrue(closures.contains("holiday") || closures.contains("Holiday")
                || closures.contains("maintenance") || closures.contains("Market")
                || closures.contains("Station") || closures.contains("Oven"),
                "a visitor should see why we are closed, got: " + closures);
    }
}
