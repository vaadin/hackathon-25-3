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

### State of this report

Seen in a running application with a real provider, and not reduced to a minimal project: reproducing it needs an OpenAI key and a Grid Pro licence. Everything above is what the application did, not what the API suggests it would do.

Found on 25.3.0-beta1.
