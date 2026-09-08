REPO: vaadin/flow
TITLE: A tool whose parameter schema is not valid JSON is dropped, and the turn continues without it

---
### Description

`LLMProvider.ToolSpec.getParametersSchema()` returns a JSON string. If that string is not valid JSON, every request logs

```
ERROR o.s.ai.openai.OpenAiChatModel : Failed to parse tool schema
com.fasterxml.jackson.core.JsonParseException: Unexpected character ('s' (code 115)):
was expecting comma to separate Object entries
 at [Source: REDACTED; line: 4, column: 93]
```

twice per turn, at ERROR, from a class the application never called. The `[Source: REDACTED]` and the line and column are the only clues, and they point into a string the application cannot see.

Ours was invalid because `\"soonest\"` in a Java text block emits an unescaped quote:

```java
return """
        {
          "type": "object",
          "properties": {
            "date": { "type": "string", "description": "Omit it, or send \"soonest\"." }
          }
        }
        """;
```

### Why it matters

The turn looks completely normal. Ours filled a customer's name, telephone and two order lines, then said "the pickup is set for 11 September at 07:30" and left the pickup empty, because the tool that writes it was never offered and the model invented an answer for the only part of the form it could not reach.

Nothing reaches the UI and nothing reaches the response listener. The log line names Jackson and a column number, not the tool, and it repeats on every request rather than once.

### Expected

Parse each tool's schema when the orchestrator is built and fail there. A schema is static, so a per request `ERROR` is a permanent misconfiguration reported in the wrong place at the wrong time.

Failing that: name the dropped tool in the log line, and report the drop on the response event so an application can tell the user its assistant is missing a hand.

### Reproduce

No project needed, but a real model is: the parse happens while the request is built.

1. Any `AIOrchestrator` with a `ToolSpec` of your own
2. Put an unescaped `"` inside a description in its schema
3. Send one prompt that should call it

### What a live run showed, and what it corrected

Reproduced with the project below, against `gpt-4o-mini`, with two tools: `set_name` with a valid schema and `set_pickup` with the broken one.

**The tool is not dropped.** Both were offered and both were called, once each, and the turn answered correctly: "I set the customer's name to Ana Torres and the pickup for tomorrow at 09:00." The counters on the page prove the executions.

What is real is the diagnosis. The error is logged twice per turn, it names Jackson and a column in a redacted source, and it never names the tool whose schema is broken. An application with a dozen tools has to bisect them to find out which one it is, while nothing appears to be wrong.

The first version of this report said the tool went missing from the request. That was inferred from a turn where the model did not call our pickup tool, and the reason was the model, not the schema.

### Getting the project

[`ai-live.zip`](https://github.com/vaadin/hackathon-25-3/raw/ea604a95b7f10720590dcd71c0e6955068f1d223/specs/issues/projects/ai-live.zip), 10 KB, sources only: four routes, one per finding, and the feature flag file the AI components need.

```
export OPENAI_API_KEY=...
mvn spring-boot:run
```

Then `http://localhost:8140/tools`. It really calls the model, so it costs a few cents a run.

Found on 25.3.0-beta1 with `vaadin-ai-core-flow` and Spring AI 2.0.0.
