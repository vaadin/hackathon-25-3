package com.vaadin.bakery.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * What the assistant can and cannot reach.
 *
 * This is the test that matters most in the AI epic. Everywhere else a mistake
 * shows a wrong answer, and here a mistake hands a language model the tracking
 * token of every order in the bakery.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class BakeryDatabaseTest {

    @Autowired
    private BakeryDatabase database;

    @Test
    void aPlainQuestionIsAnswered() {
        var rows = database.executeQuery(
                "select state, count(*) as orders from ai_orders group by state");

        assertFalse(rows.isEmpty(), "the seeded orders are there");
        // The key is the label the database returned, untouched. H2 upper cases
        // an unquoted alias, which is exactly why the schema tells the model to
        // quote every one it cares about.
        var columns = rows.getFirst().keySet();
        assertTrue(columns.contains("STATE"), columns.toString());
        assertTrue(columns.contains("ORDERS"), columns.toString());
    }

    @Test
    void aJoinAcrossTheViewsWorks() {
        var rows = database.executeQuery("""
                select l.product, sum(l.quantity) as units
                from ai_order_lines l join ai_orders o on o.order_id = l.order_id
                where o.state = 'PICKED_UP'
                group by l.product order by units desc
                """);

        assertFalse(rows.isEmpty(), "a real question about what sells");
    }

    /** The whole point of the views: the dangerous columns are not in them. */
    @Test
    void theViewsDoNotCarryAnythingPrivate() {
        var columns = database.executeQuery("select * from ai_orders").getFirst().keySet();

        var named = columns.stream().map(name -> name.toLowerCase(java.util.Locale.ROOT)).toList();
        assertFalse(named.contains("tracking_token"), columns.toString());
        assertFalse(named.contains("internal_note"), columns.toString());
        assertFalse(named.contains("customer_note"), columns.toString());
        assertFalse(named.contains("password_hash"), columns.toString());
        assertTrue(named.contains("reference"), "while what a question needs is: " + columns);
    }

    /** And the tables that hold them cannot be named at all. */
    @ParameterizedTest
    @ValueSource(strings = {
            "select tracking_token from orders",
            "select password_hash from app_user",
            "select email, phone from customer",
            "select data from order_message_attachment",
            "select o.reference from ai_orders o join app_user u on u.id = o.customer_id" })
    void aQueryAgainstARealTableIsRefused(String sql) {
        var refusal = assertThrows(RuntimeException.class, () -> database.executeQuery(sql));

        assertTrue(refusal.getMessage().contains("ai_orders"),
                "and the refusal says what it could have read instead: " + refusal.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "delete from ai_orders",
            "update ai_orders set state = 'CANCELLED'",
            "drop view ai_orders",
            "select 1 from ai_orders; drop view ai_orders",
            "select * from ai_orders -- ; drop view ai_orders\n; drop view ai_orders",
            "insert into ai_orders values (1)",
            "call csvwrite('/tmp/out.csv', 'select 1')",
            "script to '/tmp/dump.sql'",
            "grant select on ai_orders to public" })
    void anythingThatIsNotAReadIsRefused(String sql) {
        assertThrows(RuntimeException.class, () -> database.executeQuery(sql),
                "this should not have been allowed: " + sql);
    }

    /** A comment must not be able to hide a second statement. */
    @Test
    void aCommentCannotSmuggleAStatement() {
        assertThrows(RuntimeException.class, () -> database.executeQuery(
                "select reference from ai_orders /* harmless */ ; delete from orders"));
    }

    @Test
    void aQueryReadingNoTableAtAllIsRefused() {
        assertThrows(RuntimeException.class, () -> database.executeQuery("select 1"));
    }

    /** A common table expression is a name the query made up, not a table of ours. */
    @Test
    void aCommonTableExpressionOverTheViewsIsAllowed() {
        var rows = database.executeQuery("""
                with busy as (select pickup_date, count(*) as orders from ai_orders group by pickup_date)
                select pickup_date, orders from busy order by orders desc
                """);

        assertFalse(rows.isEmpty());
    }

    /** And a quoted alias arrives exactly as the model wrote it. */
    @Test
    void aQuotedAliasKeepsItsName() {
        var rows = database.executeQuery(
                "select reference as \"Reference\", state as \"State\" from ai_orders");

        assertTrue(rows.getFirst().containsKey("Reference"), rows.getFirst().keySet().toString());
        assertTrue(rows.getFirst().containsKey("State"), rows.getFirst().keySet().toString());
    }

    @Test
    void theAnswerIsCapped() {
        var rows = database.executeQuery("select reference from ai_orders");

        assertTrue(rows.size() <= 500, "however many orders there are, only 500 come back: " + rows.size());
    }

    @Test
    void theSchemaNamesOnlyTheViews() {
        var schema = database.getSchema();

        assertTrue(schema.contains("ai_orders"));
        assertTrue(schema.contains("ai_order_lines"));
        assertTrue(schema.contains("ai_products"));
        // Nothing private is named, and nothing named is then refused: what the
        // description offers and what the gate allows have to be the same list,
        // or the model spends its turns being told no.
        assertFalse(schema.contains("tracking_token"), schema);
        assertFalse(schema.contains("password_hash"), schema);
        assertFalse(schema.contains("app_user"), schema);
        assertFalse(schema.contains("internal_note"), schema);

        for (String view : java.util.List.of("ai_orders", "ai_order_lines", "ai_products")) {
            assertEquals("select 1 from " + view,
                    database.refuseUnlessReadOnlySelect("select 1 from " + view),
                    view + " is offered, so it has to be allowed");
        }
    }

    /**
     * The query a real model wrote on CI, in PostgreSQL date syntax. H2 answers
     * it with a NullPointerException about a TypeInfo, which told the model
     * nothing, so it sent the same query three times and gave up with an empty
     * grid. What the refusal has to carry is the dialect, so that a retry is a
     * different query.
     */
    @Test
    void aQueryInTheWrongDialectIsRefusedWithTheDialectNamed() {
        var sql = "select customer_name as \"Customer\", sum(total_net_cents) as \"Total\" "
                + "from ai_orders where pickup_date >= CURRENT_DATE - INTERVAL '7 DAYS' "
                + "group by customer_name";

        var refusal = assertThrows(RuntimeException.class, () -> database.executeQuery(sql));

        assertTrue(refusal.getMessage().contains("H2"),
                "the refusal names the database: " + refusal.getMessage());
        assertTrue(refusal.getMessage().contains("DATEADD"),
                "and the idiom that works here: " + refusal.getMessage());
    }

    /**
     * The same shape in H2's own syntax runs. The window is five years wide on
     * purpose: what is asserted is that the dialect parses, not where this
     * dataset's pickup dates happen to fall.
     */
    @Test
    void theSameQuestionInThisDialectIsAnswered() {
        var rows = database.executeQuery(
                "select customer_name as \"Customer\", sum(total_net_cents) as \"Total\" "
                        + "from ai_orders where pickup_date >= DATEADD('YEAR', -5, CURRENT_DATE) "
                        + "group by customer_name");

        assertFalse(rows.isEmpty(), "DATEADD parsed and the orders came back");
    }
}
