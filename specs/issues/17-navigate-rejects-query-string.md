REPO: vaadin/flow
TITLE: UI.navigate(String) rejects a URL with a query string

---
### Description

```java
ui.navigate("track/ORD-1?t=abc");
```

throws

```
IllegalArgumentException: Base path can not contain query separator
```

`navigate(Class, RouteParameters)` covers route parameters, not query parameters, so the way to navigate with a query string is

```java
ui.navigate("track/ORD-1", QueryParameters.of("t", "abc"));
```

### Why it matters

The string form is what everybody writes first, because it is the URL they already have: a tracking link, a filtered list, anything copied out of the address bar. The message says what is wrong but not what to use instead, and the overload that works is not next to it in the completion list.

### Expected

Either accept a query string in the string form and split it, or name the overload in the exception message.

### Reproduce

`specs/issues/22-browserless-differences/`, then `mvn test`. `navigationCannotCarryAQueryString` passes, and what it asserts is this refusal.

Found on 25.3.0-beta1.
