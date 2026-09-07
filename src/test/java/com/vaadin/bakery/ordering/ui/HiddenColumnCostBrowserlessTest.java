package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderQueryCounter;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.grid.Grid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

/**
 * BOARD-03 and OBS-03. The claim is that in 25.3 a hidden Grid column runs no
 * value provider and fetches nothing. This is the test that holds the release
 * to it, rather than a screenshot in a slide.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "barista@bakery.test", roles = { "BARISTA" })
class HiddenColumnCostBrowserlessTest extends SpringBrowserlessTest {

    @org.junit.jupiter.api.BeforeEach
    void signIn() {
        // After the browserless environment is up, see TestLogin for why.
        TestLogin.asBarista();
    }

    @Autowired
    private OrderQueryCounter counter;

    @SuppressWarnings("unchecked")
    private Grid<Order> grid() {
        return (Grid<Order>) find(Grid.class).single();
    }

    private void loadFirstPage() {
        // See specs/FEEDBACK-25.3.md: getLazyDataView().getItems() divides by
        // zero on a pageable backed grid in this beta.
        grid().getDataProvider()
                .fetch(new com.vaadin.flow.data.provider.Query<>(0, 50, java.util.List.of(), null, null))
                .count();
    }

    @Test
    void aHiddenColumnNeverRunsItsValueProvider() {
        var view = navigate(OrderBoardView.class);
        counter.reset();

        loadFirstPage();

        assertEquals(0, counter.expensiveColumnCalls(),
                "the summary column is hidden, so nothing should have computed it");
        assertTrue(counter.queries() > 0, "and the grid did fetch its rows");
    }

    @Test
    void showingTheColumnStartsCostingSomething() {
        var view = navigate(OrderBoardView.class);
        view.expensiveColumn().setVisible(true);
        counter.reset();

        loadFirstPage();

        assertTrue(counter.expensiveColumnCalls() > 0,
                "once the column is visible its value provider runs for every row");
    }

    @Test
    void hidingItAgainStopsTheCost() {
        var view = navigate(OrderBoardView.class);
        view.expensiveColumn().setVisible(true);
        loadFirstPage();
        view.expensiveColumn().setVisible(false);
        counter.reset();

        loadFirstPage();

        assertEquals(0, counter.expensiveColumnCalls(), "hiding it stops the work again");
    }
}
