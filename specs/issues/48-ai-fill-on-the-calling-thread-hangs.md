REPO: vaadin/flow
TITLE: fill_form blocks for ever when a provider calls it on the thread it was handed

---
### Description

An `LLMProvider` is handed a request and returns a `Flux`. If it calls one of the request's tools synchronously, on that same thread, and the tool is `FormAIController`'s `fill_form`, the call never returns.

`fill_form` needs the session lock to write the fields. The thread it is running on is the one that called `orchestrator.prompt(...)`, which already holds that lock, so it waits for itself. There is no timeout: the wait ends when somebody interrupts the thread, and then the tool answers

```
Error: fill interrupted.
```

with the fields still empty.

### Why it matters

Writing a provider is a documented thing to do: the interface is one method, and a provider that answers from a local model, a cassette, or a rules engine has no reason to hop threads. Doing the obvious thing hangs the request, and in a real application that means a UI that never comes back and a thread that stays parked.

The failure gives nothing to work from either. No log line while it waits, and the message after an interrupt says the fill was interrupted rather than that it was waiting for a lock it could not get.

### Expected

Either take the lock in a way that tolerates already holding it, or fail fast with a message naming the problem: "fill_form was called on the thread that holds the session lock". A timeout with an explanation would be enough.

### Reproduce

The project below, and `mvn test -Dtest=FillOnTheUiThreadTest`. The provider is fifteen lines: it finds `fill_form` among `request.explicitTools()`, calls it with `{"values":{"name":"Ana Torres"}}`, and returns one string.

```
Tests run: 1, Failures: 0, Errors: 1
java.util.concurrent.TimeoutException: ... timed out after 20 seconds
  Suppressed: org.opentest4j.AssertionFailedError: the field was filled by the tool,
             and the result was: Error: fill interrupted. ==> expected: <Ana Torres> but was: <>
```

The `@Timeout` is the assertion: without it the test hangs and the build never ends.

One detail worth keeping if this is fixed: the form has to be attached first. `fill_form` on a detached form answers `Error: fill failed.` and logs `fill_form invoked on a controller whose form is not attached to a UI`, which is a good message and the reason this reproduction has an `UI.getCurrent().add(form)` in it.

### Getting the project

[`ai-live.zip`](https://github.com/vaadin/hackathon-25-3/raw/f36086c0050c7fc2fc175ed169a155c3ba418413/specs/issues/projects/ai-live.zip), 12 KB, sources only.

```
mvn test -Dtest=FillOnTheUiThreadTest
```

No key and no network: the provider in that test is the model.

Found on 25.3.0-beta1, `vaadin-ai-extensions-flow`, no network and no key needed.
