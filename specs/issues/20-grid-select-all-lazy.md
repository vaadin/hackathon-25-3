REPO: vaadin/flow-components
TITLE: A lazy Grid cannot offer a select all checkbox, and cannot be given one

---
### Description

With in memory items, multi select shows a select all checkbox. With a lazy data provider it does not, and there is no way to put one there. Every door is closed:

| Attempt | Result |
| --- | --- |
| `setSelectAllCheckboxVisibility(VISIBLE)` | Ignored for a lazy provider |
| The selection column's own switch | Not exposed: `GridSelectionColumn` extends `Component`, not `AbstractColumn` |
| Clicking the checkbox from the client | Nothing arrives, because the checkbox is not rendered |
| Substituting the data provider callbacks | The visibility decision is made from the provider's type, not from the callbacks |
| A header cell above the selection column | There is none. No header row has a cell for that column |
| A checkbox in the first data column's header | Renders in the wrong place, one column to the right of where a user expects it |

### Why it matters

"Select all, then confirm" is an ordinary bulk action, and a list long enough to need a lazy provider is exactly the list somebody wants it on. We wanted it for an order board and ended up with no select all at all.

The dead end is quiet: `setSelectAllCheckboxVisibility(VISIBLE)` returns without complaining.

### Expected

Either honour the visibility setting for a lazy provider, with the count coming from the same count callback the grid already has, or expose the selection column so an application can put its own control in it. Failing both, log or throw when the setting is ignored.

### Reproduce

```java
var grid = new Grid<Person>();
grid.setSelectionMode(Grid.SelectionMode.MULTI);
grid.setItems(query -> service.fetch(query.getOffset(), query.getLimit()),
        query -> service.count());
grid.setSelectAllCheckboxVisibility(SelectAllCheckboxVisibility.VISIBLE);
```

No checkbox appears. Swap `setItems` for an in memory list and it does.

Found on 25.3.0-beta1.
