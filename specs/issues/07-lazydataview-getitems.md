REPO: vaadin/flow
TITLE: LazyDataView.getItems() throws ArithmeticException when the grid was populated with setItemsPageable
---
### Description

```java
grid.setItemsPageable(fetch, count);
grid.getLazyDataView().getItems();
```

throws `ArithmeticException: / by zero`.

`AbstractLazyDataView.getItems` builds a `Query` with no page size, `VaadinSpringDataHelpers.toSpringPageRequest` calls `query.getPage()`, and `Query.getPage()` divides by a page size of zero.

### Why it matters

These are the two documented ways to do the two most common things: populate a lazy grid from Spring Data, and read a lazy grid's contents in a test. They do not work together.

### Expected

`getItems()` works on a grid populated through `setItemsPageable`.

### Workaround

Fetch through the data provider with an explicit query: `grid.getDataProvider().fetch(new Query<>(0, 100, List.of(), null, null))`.

Found on 25.3.0-beta1.
