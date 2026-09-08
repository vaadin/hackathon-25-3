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

and then goes to the model with the other tools and without that one.

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

Found on 25.3.0-beta1 with `vaadin-ai-core-flow` and Spring AI 2.0.0.
