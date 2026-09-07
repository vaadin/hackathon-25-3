package com.vaadin.bakery.ordering;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * DASH-04. The dashboard's query budget.
 *
 * This counts the statements Hibernate actually prepares, not the service calls
 * the view makes, because the two are not the same number and the difference is
 * the bug worth catching: {@code topProducts} used to walk the orders in the
 * range and fetch each one again by reference, which is a query per order on
 * the heaviest page in the application. Every service call looked innocent.
 *
 * The number the specification asks for is fewer than ten. There are five
 * questions on the page, so a query each leaves room and no room to hide an
 * N plus one.
 */
@SpringBootTest(classes = Application.class,
        properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
class DashboardQueryBudgetTest {

    @Autowired
    private DashboardService dashboard;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private Clock clock;

    private Statistics statistics;

    @BeforeEach
    void resetTheCounters() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    void awholeDashboardLoadStaysUnderTenQueries() {
        var to = LocalDate.now(clock);
        var from = to.minusDays(6);
        var comparisonTo = from.minusDays(1);
        var comparisonFrom = comparisonTo.minusDays(6);

        // Exactly what DashboardView asks for when it renders a range.
        dashboard.today();
        dashboard.revenue(from, to);
        dashboard.revenue(comparisonFrom, comparisonTo);
        dashboard.byState(from, to);
        dashboard.topProducts(from, to, 10);

        long statements = statistics.getPrepareStatementCount();
        assertTrue(statements < 10,
                "a dashboard load stayed under ten statements, issued " + statements);
    }

    /**
     * And the top products panel is one query on its own, whatever the range
     * holds. Without this, the panel can quietly go back to a query per order
     * and the budget above would still pass on a quiet week.
     */
    @Test
    void theTopProductsPanelIsOneQueryHoweverManyOrdersThereAre() {
        var to = LocalDate.now(clock).plusDays(30);
        var from = to.minusDays(120);

        dashboard.topProducts(from, to, 10);

        long statements = statistics.getPrepareStatementCount();
        assertTrue(statements == 1,
                "one query over four months of orders, issued " + statements);
    }
}
