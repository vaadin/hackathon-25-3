package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

/** BOARD-05. Selection checkboxes and sorters have to be announced properly. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "barista@bakery.test", roles = { "BARISTA" })
class GridI18nBrowserlessTest extends SpringBrowserlessTest {

    @org.junit.jupiter.api.BeforeEach
    void signIn() {
        // After the browserless environment is up, see TestLogin for why.
        TestLogin.asBarista();
    }

    @Test
    void selectionAndSortersAreNamed() {
        navigate(OrderBoardView.class);
        var i18n = ((Grid<?>) find(Grid.class).single()).getI18n();

        assertNotNull(i18n, "the grid carries an i18n object");
        assertEquals("Select all orders", i18n.getSelectAll());
        assertEquals("Select this order", i18n.getSelectRow());
        assertEquals("Sort by this column", i18n.getSorter());
    }

    @Test
    void thoseNamesAreTranslated() {
        UI.getCurrent().setLocale(Locale.of("es"));
        navigate(OrderBoardView.class);
        var i18n = ((Grid<?>) find(Grid.class).single()).getI18n();

        assertEquals("Seleccionar todos los pedidos", i18n.getSelectAll());
    }
}
