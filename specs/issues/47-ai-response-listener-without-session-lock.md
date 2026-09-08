REPO: vaadin/flow
TITLE: withResponseListener is called without a session lock, and the failure is swallowed

---
### Description

```java
AIOrchestrator.builder(provider, prompt)
        .withResponseListener(event -> label.setText(summarise(event)))
        .build();
```

That listener runs on the provider's thread with no session lock, so touching any component from it throws

```
java.lang.IllegalStateException: Cannot access state in VaadinSession or UI without locking the session.
```

and the orchestrator logs

```
ERROR c.v.f.c.ai.orchestrator.AIOrchestrator : Error in response listener
```

and carries on. The application sees nothing: the label never changes, no exception reaches the view, and the turn looks like it produced no response at all.

### Why it matters

Updating something on screen when a turn ends is the whole reason to register a response listener, so the first thing anybody writes inside it is the thing that cannot be done there. The symptom is "my listener never ran", which sends the reader to the orchestrator rather than to a lock.

We lost an afternoon to a turn meter that stayed at zero, and it was this.

### Expected

Either call the listener with the session locked, since the orchestrator was built from a component and knows the UI, or say in the `withResponseListener` documentation that the callback holds no lock and needs `ui.access`.

Not swallowing it would help too: "Error in response listener" without the throwable at the same level is a line nobody reads twice.

### Workaround

Capture the UI when the component attaches and wrap the body:

```java
.withResponseListener(event -> ui.access(() -> label.setText(summarise(event))))
```

### Reproduce

The project below, route `/grid`. The listener writes to a `Pre`. Without `ui.access` the block keeps saying "no turn yet" and the log carries the exception above; with it, the same listener prints the turn.

### Getting the project

[`ai-live.zip`](https://github.com/vaadin/hackathon-25-3/raw/ea604a95b7f10720590dcd71c0e6955068f1d223/specs/issues/projects/ai-live.zip), 10 KB, sources only: four routes, one per finding, and the feature flag file the AI components need.

```
export OPENAI_API_KEY=...
mvn spring-boot:run
```

Then `http://localhost:8140/grid`. It really calls the model, so it costs a few cents a run.

Found on 25.3.0-beta1.
