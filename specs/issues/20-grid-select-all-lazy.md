REPO: vaadin/flow-components
TITLE: A lazy Grid renders a select all checkbox that selects nothing

---
### Description

A `Grid` in multi select mode with a lazy data provider, and the select all visibility left where it comes from the factory. The framework's own rule for `SelectAllCheckboxVisibility.DEFAULT` is that select all is offered for in memory data and not for a lazy provider, so this grid is one where select all is not supported.

The checkbox is rendered in the header anyway, 18 by 20 pixels, in the selection column, looking exactly like the working one. Click it and it ticks. The selection stays empty: no selection event, no row selected, nothing logged.

### Why it matters

A control that is drawn and does nothing is worse than no control. The user ticks it, sees the tick, and acts as if five hundred rows were selected. The next button they press works on nothing.

It is also the kind of thing a test suite does not catch, because the server never hears about the click.

### Expected

Either do not render the checkbox when the visibility rule says select all is not offered, or let the click do what it appears to do.

### Reproduce

`20-grid-selectall-lazy/` in this directory. `mvn spring-boot:run`, then two routes.

`http://localhost:8120/only-default` is the bug on its own page: one lazy grid, multi select, nothing else. The measurement, clicking the header checkbox once on a freshly loaded page:

```
checkbox size          18x20
checked after click    true
selection on server    0, before and after
rows ticked            0
```

`http://localhost:8120/` is the comparison: the same grid three times, each with a line printing the selection size.

| Grid | Header checkbox | Clicking it selects |
| --- | --- | --- |
| In memory, `setSelectAllCheckboxVisibility(VISIBLE)` | Yes | 500 of 500 |
| Lazy, `setSelectAllCheckboxVisibility(VISIBLE)` | Yes | 500 of 500 |
| Lazy, visibility left at its default | Yes | nothing |
| `setItemsPageable`, the Spring Data path, with `VISIBLE` | Yes | 500 of 500, on `/pageable` |

### A correction to an earlier version of this report

This was first written up as "a lazy Grid cannot offer a select all checkbox, and cannot be given one", with a table of six ways to force it. That was wrong, and the project is what showed it: `setSelectAllCheckboxVisibility(VISIBLE)` is honoured for a lazy provider and selects the whole set, five hundred rows through the count callback. It works through `setItemsPageable` too, which is the path the original investigation used. Only the default case is broken, and it is broken in the other direction: the checkbox is there and inert.

The enum's own documentation is part of why nobody tried: `VISIBLE` says it shows the checkbox "if in-memory data is used". It shows it either way, and that sentence is worth correcting whatever happens to the rest.

Found on 25.3.0-beta1.
