REPO: vaadin/flow
TITLE: An AI turn that produces nothing leaves no trace on any surface

---
### Description

Ask a `GridAIController` a question whose query fails. What a person sees:

- the message list shows the question and no answer
- the grid stays empty
- the turn meter, fed from `ResponseMetadata`, stays at zero turns and zero tokens

That is the same as what they see when nobody has asked anything yet. The failure is only in the application log.

### Why it matters

The three surfaces a person can look at all report "nothing happened" for a turn that did happen and failed. Ours was a real failure underneath, and the only reason we found it was reading the log line by line.

Diagnosing it should be thirty seconds, and it was an afternoon.

### Expected

Two small things would be enough:

1. Count the turn even when it produces nothing, so the meter moves.
2. Deliver the error to the response listener, the way the finish reason already arrives, so an application can put a sentence on the screen.

### Reproduce

Any `GridAIController` over a `DatabaseProvider` whose `executeQuery` throws something that is not a `ToolException`, then one question. The grid stays empty and no listener hears anything.

Needs a real model, because the failure is inside a turn.

### What a live run showed

Reproduced with the project below, on two questions against `gpt-4o-mini`.

The first one succeeds as far as the model is concerned and fails in the grid, and this is what the three surfaces said:

| Surface | What it showed |
| --- | --- |
| Message list | "I have listed every appointment along with who it is for and its time. You can view the details in the grid." |
| Grid | Nothing. Zero columns |
| Response listener | Called once, `error: (none)` |

The failure, `UnsupportedOperationException` from `java.sql.Time.toInstant`, is in the log and nowhere else.

The second question makes the query fail. The listener was **not called at all**, the assistant's bubble was added and left empty, and the grid kept the previous question's columns. A person watching sees an empty answer and no reason.

One correction to the row above: `ResponseEvent` carries no metadata to read. The whole class is

```java
public String getResponse();
public Optional<Throwable> getError();
```

so a turn meter has nothing to count, and an application that wants token usage has to reach past the orchestrator to the provider.

### Getting the project

[`ai-live.zip`](https://github.com/vaadin/hackathon-25-3/raw/ea604a95b7f10720590dcd71c0e6955068f1d223/specs/issues/projects/ai-live.zip), 10 KB, sources only: four routes, one per finding, and the feature flag file the AI components need.

```
export OPENAI_API_KEY=...
mvn spring-boot:run
```

Then `http://localhost:8140/grid`. It really calls the model, so it costs a few cents a run.

Found on 25.3.0-beta1 with `vaadin-ai-core-flow`.
