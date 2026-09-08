REPO: vaadin/flow
TITLE: SignalOperation.result() never completes for a shared signal in a browserless test

---
### Description

A write to a `SharedListSignal` is visible immediately and its operation never reports anything.

```java
var operation = tickets.queue().insertLast("a ticket");

tickets.queue().peek().size();      // grew by one, straight away
operation.result().isDone();        // false
operation.result().get(5, SECONDS); // TimeoutException
```

The same is true for a `SharedValueSignal`. The value is there, the future is not.

### Why it matters

`result()` is how code waits for a write, and a test is the first place anybody waits for one. A future that never completes gives a five second timeout per assertion and, worse, reads as the write having failed.

We spent a day on this and diagnosed it wrong. We awaited the result, it timed out, and we concluded that writes were being dropped: that produced a wrong explanation about the signal environment being replaced between test classes, a `@DirtiesContext` on every class that writes, and two rewrites of a board that was never broken. The write had landed every time.

### Expected

Complete the future, or fail it. Either answer is usable. A future that neither completes nor fails cannot be told from a write that vanished.

If it cannot complete outside a real client, say so where `result()` is documented.

### Reproduce

[`19-shared-signal-env/`](https://github.com/vaadin/hackathon-25-3/tree/9cf6235d4bf170002c44ac69c0d51063376828ba/specs/issues/19-shared-signal-env), then `mvn test`. Two tests, both passing, and the second one is the finding: it asserts that the list grew and that the operation has still reported nothing.

Measured in a browserless test. Not measured in a running application, where the same signals work.

Found on 25.3.0-beta1 with browserless-test 1.2.0-alpha2.

### Getting the project

```
git clone --branch manolo --depth 1 https://github.com/vaadin/hackathon-25-3
cd hackathon-25-3/specs/issues/19-shared-signal-env
mvn test
```

