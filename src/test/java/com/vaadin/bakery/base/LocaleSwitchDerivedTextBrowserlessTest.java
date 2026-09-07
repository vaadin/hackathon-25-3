package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.ui.OrderBoardView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.browserless.component.GridKt;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * FND-10. The cells a value provider computes follow the language too.
 *
 * A translated header is the easy half. The hard half is a cell whose text is
 * made rather than looked up: a date formatted for a locale, an amount with its
 * currency, a state name that came out of the bundle inside a value provider.
 * Those are computed when the row is drawn, so nothing retranslates them unless
 * the grid is asked for them again, and a test that navigates after switching
 * never notices.
 *
 * This one reads the rendered row, which is the only thing that runs a value
 * provider, and it never navigates after the switch.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class LocaleSwitchDerivedTextBrowserlessTest extends SpringBrowserlessTest {

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    @SuppressWarnings("unchecked")
    private Grid<Order> board() {
        return (Grid<Order>) find(Grid.class).single();
    }

    /** Everything the first row actually renders, joined. */
    private String firstRow() {
        return GridKt._getFormattedRow(board(), 0).stream().collect(Collectors.joining(" | "));
    }

    @Test
    void aDerivedCellIsRetranslatedWithoutNavigating() {
        UI.getCurrent().setLocale(Locale.ENGLISH);
        navigate(OrderBoardView.class);
        var inEnglish = firstRow();
        assertTrue(inEnglish.contains("Sep") || inEnglish.contains("Jan")
                        || inEnglish.matches(".*\\b(Mon|Tue|Wed|Thu|Fri|Sat|Sun)\\b.*"),
                "the slot cell is an English date to begin with: " + inEnglish);

        UI.getCurrent().setLocale(Locale.of("es"));

        var inSpanish = firstRow();
        assertFalse(inSpanish.equals(inEnglish),
                "the rendered row changed: it was " + inEnglish);
        assertTrue(inSpanish.matches(".*\\b(lun|mar|mié|mie|jue|vie|sáb|sab|dom)\\b.*"),
                "and the day name is Spanish now: " + inSpanish);
    }

    /**
     * The state badge is a component column whose text comes from the bundle,
     * which is a third way for a cell to be made rather than looked up.
     */
    @Test
    void aTranslatedStateInAComponentColumnFollowsToo() {
        UI.getCurrent().setLocale(Locale.ENGLISH);
        navigate(OrderBoardView.class);
        var inEnglish = firstRow();

        UI.getCurrent().setLocale(Locale.of("es"));

        var inSpanish = firstRow();
        assertFalse(inEnglish.equals(inSpanish), "the row is not the same text in both languages");
    }
}
