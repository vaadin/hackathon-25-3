REPO: vaadin/flow
TITLE: Source tracking is enabled and no model ever reports a source

---
### Description

```java
controller.setSourceTrackingEnabled(true);
```

then a real turn against OpenAI, then `controller.getFieldSource(field)` for every field the model filled. Empty, every time, with `gpt-4o-mini` and with `gpt-4o`, over more than a dozen live runs.

The plumbing works. Hand the same controller a fill that carries `confidence` and `extracts` and the value comes back and the marker's popover renders it, so the parser and the API are fine. What never happens is the model producing that shape.

The `fill_form` parameter schema is

```json
{ "values": { "type": "object", "additionalProperties": true } }
```

which says nothing about a source, so whether one arrives rests entirely on prose in the tool description.

### Why it matters

Source tracking is the headline of the 25.3 form controller and the reason an application turns it on. Without it the field marker has a badge, an explanation and a revert control, and nothing to say about where the value came from.

We also asked for it in our own system prompt, which the documentation says outranks the controller's instructions, and it changed nothing.

### Expected

Put `confidence` and `extracts` in the `fill_form` schema, so the model is asked for them by the shape of the call rather than by a sentence.

### Reproduce

Any `FormAIController` with source tracking on, a real key, and one prompt that fills a field. Then `getFieldSource` on that field.

### What a live run showed

Reproduced with the project below, `setSourceTrackingEnabled(true)`, one clean turn against `gpt-4o-mini` that filled two fields:

```
name    value=Ana Torres    source=(none)
phone   value=600123456     source=(none)
```

The field marker renders and its popover opens, and what it has to say is "This field value was modified by AI." and a Revert Value button. Nothing about where the value came from, because there is nothing to say.

Found on 25.3.0-beta1 with `vaadin-ai-extensions-flow` and Spring AI 2.0.0.
