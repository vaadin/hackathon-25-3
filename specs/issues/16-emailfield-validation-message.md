REPO: vaadin/flow-components
TITLE: EmailField shows no message for a bean validation failure

---
### Description

Bind the same property to a `TextField` and to an `EmailField`, with `@Email` on it, and read the error message off each after a failed validation.

```java
binder.bind(textField, "email");    // getErrorMessage() -> "must be a well-formed email address"
binder.bind(emailField, "email");   // getErrorMessage() -> ""
```

The field's own address check wins and its message is empty, so the field turns red and says nothing.

### Why it matters

Any application with an `EmailField` and bean validation has a silently failing field, and nothing in the API hints at it. A red box with no text is a puzzle, not a refusal.

### Expected

Keep the message the binder produced when the field has no message of its own.

### Workaround

Build the field through a factory that sets an error message explicitly.

### Reproduce

[`16-emailfield-message/`](https://github.com/vaadin/hackathon-25-3/tree/9cf6235d4bf170002c44ac69c0d51063376828ba/specs/issues/16-emailfield-message), then `mvn test`. One test, and it passes: the text field reads `must be a well-formed email address` and the email field reads the empty string.

### Getting the project

```
git clone --branch manolo --depth 1 https://github.com/vaadin/hackathon-25-3
cd hackathon-25-3/specs/issues/16-emailfield-message
mvn test
```

Found on 25.3.0-beta1.
