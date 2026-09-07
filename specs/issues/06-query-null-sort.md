REPO: vaadin/flow
TITLE: Query with a null sort order list throws an NPE inside VaadinSpringDataHelpers
---
### Description

```java
dataProvider.fetch(new Query<>(0, 500, null, null, null));
```

against a grid populated with `setItemsPageable` throws

```
java.lang.NullPointerException: Cannot invoke "java.util.List.stream()" because the return value of
"com.vaadin.flow.data.provider.Query.getSortOrders()" is null
	at com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringDataSort(VaadinSpringDataHelpers.java:51)
	at com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest(VaadinSpringDataHelpers.java:69)
```

`Query` accepts the null happily and nothing fails until the helper streams it.

### Why it matters

The five argument `Query` constructor with nulls is what every example passes when the test does not care about sorting. The exception names a helper the caller never mentioned, so it reads as an application bug.

### Expected

Either `Query` defaults the sort orders to an empty list, or the helper checks.

### Workaround

Pass `List.of()`.

Found on 25.3.0-beta1.
