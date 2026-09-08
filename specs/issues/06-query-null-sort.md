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

### Reproduce

[`06-query-null-sort/`](https://github.com/vaadin/hackathon-25-3/tree/9cf6235d4bf170002c44ac69c0d51063376828ba/specs/issues/06-query-null-sort): a grid filled with `setItemsPageable` and two buttons, one passing a null sort order list and one passing an empty one. The first prints the stack trace above, the second answers with the rows.

### Getting the project

```
git clone --branch manolo --depth 1 https://github.com/vaadin/hackathon-25-3
cd hackathon-25-3/specs/issues/06-query-null-sort
mvn spring-boot:run
```

Found on 25.3.0-beta1.
