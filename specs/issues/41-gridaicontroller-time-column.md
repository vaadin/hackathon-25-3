REPO: vaadin/flow
TITLE: GridAIController cannot render a SQL TIME column

---
### Description

Ask a `GridAIController` anything whose query selects a `TIME`:

```sql
select pickup_time as "Pickup time" from ai_orders
```

`GridFormatting.formatValue` calls `java.sql.Time.toInstant()`, which the JDK throws `UnsupportedOperationException` from unconditionally, by documented design, because a `java.sql.Time` has no date. The exception surfaces as

```
RuntimeException: Push failed
```

from the orchestrator.

### Why it matters

The grid renders nothing at all. Not a bad cell: the whole grid, because the failure happens while the push is being written, so the columns exist and the page shows an empty box.

For a bakery, any question about when something is due hits it, which is most of them.

### Expected

Format a `java.sql.Time` through `toLocalTime()` rather than `toInstant()`. One line in `GridFormatting`.

### Workaround

Expose the column as text in the database view: `cast(o.pickup_time as varchar)`. It sorts correctly zero padded, and the schema then tells the model that times are text.

### Reproduce

Any `GridAIController` over a view with a `TIME` column, and one question that selects it. Needs a licence and a model.

### What a live run showed

Reproduced with the project below: one table, `appointments(id INT, who VARCHAR, at TIME, on_day DATE)`, and the question "List every appointment with who it is for and its time".

The model wrote the obvious SQL, `SELECT who AS "For", at AS "Time" FROM appointments`, and the grid rendered nothing. In the log:

```
java.lang.UnsupportedOperationException
    at java.sql.Time.toInstant(Time.java:281)
    at com.vaadin.flow.component.ai.grid.GridFormatting.formatValue(GridFormatting.java:55)
    at com.vaadin.flow.component.ai.grid.GridRenderer.lambda$addColumn$d4d9cbbd$4(GridRenderer.java:121)
```

`java.sql.Time.toInstant` throws by contract: a TIME has no date, so it cannot be an instant. Any query that selects a TIME column reaches that line.

The application is told nothing. The response listener reports no error, the assistant says it listed the appointments, and the grid stays empty.

### Getting the project

[`ai-live.zip`](https://github.com/vaadin/hackathon-25-3/raw/f36086c0050c7fc2fc175ed169a155c3ba418413/specs/issues/projects/ai-live.zip), 10 KB, sources only: four routes, one per finding, and the feature flag file the AI components need.

```
export OPENAI_API_KEY=...
mvn spring-boot:run
```

Then `http://localhost:8140/grid`. It really calls the model, so it costs a few cents a run.

Found on 25.3.0-beta1.
