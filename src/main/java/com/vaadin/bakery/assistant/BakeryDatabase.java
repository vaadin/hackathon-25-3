package com.vaadin.bakery.assistant;

import com.vaadin.flow.component.ai.provider.DatabaseProvider;
import com.vaadin.flow.component.ai.provider.ToolException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only database the assistant can see.
 *
 * The grid and chart controllers work by letting a model write SQL, which is a
 * different kind of trust from letting it fill a text field. Two things bound
 * it, and neither of them is a sentence in a prompt.
 *
 * The first is the schema below: three views, defined in
 * {@code schema-h2.sql}, holding what a question about orders, products and
 * revenue actually needs. No tracking token, no password hash, no internal
 * note, no address, no telephone number, no attachment. A query that succeeds
 * completely still cannot read any of them.
 *
 * The second is {@link #executeQuery(String)}: one statement, and that
 * statement a {@code select} over the three views, capped and timed. It refuses
 * rather than sanitising, because a refusal is something the model can read and
 * correct, and a silently rewritten query is not.
 *
 * What is deliberately not here is a database user with select only grants,
 * which is the answer that does not depend on this class being right. It needs
 * a second data source and a different story on H2 and on PostgreSQL, and it is
 * the first thing to add if this ever faces anything but a demo.
 */
@Component
public class BakeryDatabase implements DatabaseProvider {

    private static final Logger LOG = LoggerFactory.getLogger(BakeryDatabase.class);

    /** Every table name the model may write down. Anything else is refused. */
    private static final Set<String> ALLOWED = Set.of("ai_orders", "ai_order_lines", "ai_products");

    /** Rows one answer may carry back, whatever the query asked for. */
    private static final int MAX_ROWS = 500;

    /** Seconds one query may run, so a cartesian product is an error and not an outage. */
    private static final int TIMEOUT_SECONDS = 5;

    private static final Pattern COMMENTS = Pattern.compile("(--[^\\n]*)|(/\\*.*?\\*/)", Pattern.DOTALL);
    private static final Pattern SOURCES = Pattern.compile("\\b(?:from|join)\\s+([A-Za-z_][A-Za-z0-9_.\"]*)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Anything that writes, changes the shape of the database, reaches the file
     * system or calls out. H2 in particular has functions that read and write
     * files, and this list is why the view allowlist is not the only check.
     */
    private static final Set<String> FORBIDDEN = Set.of(
            "insert", "update", "delete", "merge", "upsert", "truncate", "drop", "alter", "create",
            "grant", "revoke", "commit", "rollback", "savepoint", "call", "execute", "script",
            "runscript", "shutdown", "csvwrite", "csvread", "file_read", "file_write", "link_schema",
            "trigger", "sleep", "pg_read_file", "pg_sleep", "lo_import", "lo_export", "copy", "dblink");

    private final JdbcTemplate jdbc;

    /** The database's own name, for the one sentence that has to name it. */
    private volatile String product;

    public BakeryDatabase(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * What the model is told exists. It is written by hand rather than read off
     * the database, because the description is part of the contract: naming a
     * column here is what makes it queryable, and a schema dumped from the
     * connection would quietly grow new columns nobody decided to expose.
     */
    @Override
    public String getSchema() {
        return """
                Three read-only views. Every question must be answered from these and nothing else.
                Money is in integer cents. Dates are SQL dates. Times are text in HH:MM:SS, which
                sorts correctly, so compare and order them as text.

                ai_orders(order_id, reference, state, channel, pickup_date, pickup_time, placed_at,
                          total_gross_cents, total_net_cents, pickup_location, customer_id, customer_name)
                  One row per order. state is one of NEW, CONFIRMED, IN_PREPARATION, READY, PICKED_UP,
                  PROBLEM, CANCELLED. channel is one of ONLINE, PHONE, COUNTER.

                ai_order_lines(order_id, reference, pickup_date, product, category, quantity,
                               unit_price_cents, comment)
                  One row per line of an order. comment is what the customer asked for on that line,
                  which is where allergy requests are written.

                ai_products(product_id, name, price_cents, available, lead_time_days, category)
                  One row per product in the catalogue.

                Write a single SELECT. Do not write a semicolon. Aggregate in SQL rather than asking
                for every row: at most 500 rows come back whatever you ask for.

                Quote the alias you invent, and never the column you are reading:
                  select reference as "Reference"          correct
                  select "reference" as "Reference"        wrong, and finds nothing
                An unquoted alias comes back upper cased and the name you chose is lost. A quoted
                column name is matched exactly, and the columns above are not stored in lower case.
                """ + dateArithmetic();
    }

    /**
     * The one sentence that has to name the database, because date arithmetic
     * is where the dialects part company and the model cannot guess which one
     * it is talking to.
     *
     * A model asked for "this week" writes {@code INTERVAL '7 DAYS'}, which is
     * PostgreSQL. H2 does not answer that with a syntax error: it throws
     * {@code NullPointerException: Cannot invoke "org.h2.value.TypeInfo.getValueType()"},
     * which reaches the model as a sentence about a null and tells it nothing,
     * so it retries the same query until it gives up. Naming the dialect costs
     * one line and removes the whole loop.
     */
    private String dateArithmetic() {
        var postgres = "postgresql".equalsIgnoreCase(product());
        return postgres
                ? """

                        The database is PostgreSQL. For date arithmetic write
                        CURRENT_DATE - INTERVAL '7 days'.
                        """
                : """

                        The database is H2. For date arithmetic write
                        DATEADD('DAY', -7, CURRENT_DATE), never INTERVAL '7 DAYS', which is not
                        valid here and fails with a message about a null rather than a syntax error.
                        """;
    }

    /** Asked once: the schema text is built for every question. */
    private String product() {
        if (product == null) {
            try {
                product = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>) connection ->
                        connection.getMetaData().getDatabaseProductName());
            } catch (RuntimeException unavailable) {
                LOG.debug("Could not read the database product name", unavailable);
                product = "";
            }
        }
        return product;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeQuery(String sql) {
        var query = refuseUnlessReadOnlySelect(sql);
        LOG.debug("Assistant query: {}", query);
        try {
            return read(query);
        } catch (org.springframework.dao.DataAccessException broken) {
            // The database's own complaint, turned into something the model can
            // act on. Watching a real turn, it quoted the source column names,
            // got a bare syntax error back and sent the same query four times.
            throw new ToolException("That query did not run: " + rootCause(broken)
                    + " Quote the alias you invent and not the column you are reading, and use only the "
                    + "columns listed in the schema.");
        }
    }

    private static String rootCause(Throwable failure) {
        var cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        var message = cause.getMessage();
        return message == null ? failure.toString() : message.split("\n")[0] + ".";
    }

    private List<Map<String, Object>> read(String query) {
        return jdbc.query(connection -> {
            var statement = connection.prepareStatement(query);
            statement.setMaxRows(MAX_ROWS);
            statement.setQueryTimeout(TIMEOUT_SECONDS);
            return statement;
        }, resultSet -> {
            var rows = new ArrayList<Map<String, Object>>();
            var metadata = resultSet.getMetaData();
            while (resultSet.next() && rows.size() < MAX_ROWS) {
                var row = new LinkedHashMap<String, Object>();
                for (int column = 1; column <= metadata.getColumnCount(); column++) {
                    // The label exactly as the database returned it. Both
                    // controllers key on it: the grid turns it into a column
                    // header and the chart looks for "category" and "value",
                    // so lowering or tidying it here silently renames what the
                    // model deliberately chose.
                    row.put(metadata.getColumnLabel(column), resultSet.getObject(column));
                }
                rows.add(row);
            }
            return rows;
        });
    }

    /**
     * The gate. It throws {@link ToolException} because everything it refuses
     * is something the model can rewrite, and telling it why is the difference
     * between one wasted turn and a loop.
     */
    String refuseUnlessReadOnlySelect(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new ToolException("Send a SELECT over " + String.join(", ", ALLOWED) + ".");
        }
        var stripped = COMMENTS.matcher(sql).replaceAll(" ").trim();
        while (stripped.endsWith(";")) {
            stripped = stripped.substring(0, stripped.length() - 1).trim();
        }
        if (stripped.contains(";")) {
            throw new ToolException("One statement per query. Send a single SELECT.");
        }

        var lower = stripped.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("select") && !lower.startsWith("with")) {
            throw new ToolException("Only SELECT is allowed here. This database is read only.");
        }
        for (String word : FORBIDDEN) {
            if (Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(lower).find()) {
                throw new ToolException("\"" + word + "\" is not allowed. Send a plain SELECT over "
                        + String.join(", ", ALLOWED) + ".");
            }
        }

        var sources = SOURCES.matcher(lower);
        var seen = false;
        while (sources.find()) {
            var source = sources.group(1).replace("\"", "");
            // A subquery reads "from (", which the pattern skips, and a common
            // table expression is resolved by name: neither is a table.
            if (source.startsWith("(")) {
                continue;
            }
            seen = true;
            if (!ALLOWED.contains(source) && !isCommonTableExpression(lower, source)) {
                throw new ToolException("There is no table called \"" + source + "\" here. The only ones are "
                        + String.join(", ", ALLOWED) + ".");
            }
        }
        if (!seen) {
            throw new ToolException("Read from one of " + String.join(", ", ALLOWED) + ".");
        }
        return stripped;
    }

    /** A name the query itself defined with WITH is not a table of ours. */
    private boolean isCommonTableExpression(String lower, String name) {
        return Pattern.compile("\\b" + Pattern.quote(name) + "\\s+as\\s*\\(").matcher(lower).find();
    }
}
