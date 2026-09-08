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

Found on 25.3.0-beta1 with `vaadin-ai-core-flow`.
