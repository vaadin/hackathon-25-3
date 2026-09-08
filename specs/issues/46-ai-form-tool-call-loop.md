REPO: vaadin/flow
TITLE: A FormAIController turn can loop on get_form_state until the process is killed

---
### Description

One prompt, one form, and a request that mentions something the form has no field for. The model fills what it can, then calls `get_form_state` again, and again. There is no cap on tool call rounds, so the turn does not end.

Measured in the project below, against `gpt-4o-mini`:

```
150 calls to get_form_state
  3 calls to fill_form
2 minutes 11 seconds
```

It stopped because the application was killed. Nothing else was going to stop it.

The form has two fields the controller found and one inside a `Composite` that it did not, and the prompt names all three. So the model is asked for something it cannot see a field for, and re-reads the form looking for it.

While that happens, every field the controller touched keeps its `ai-working` attribute, and the platform's own stylesheet has

```css
[ai-working] > vaadin-ai-field-marker { display: none; }
```

so the field markers stay hidden for as long as the loop runs, which is to say for ever. A person sees the shimmer animation on the fields and no explanation.

### Why it matters

This is somebody's money. A loop that calls a model 150 times in two minutes on one button press is a cost incident, and the application cannot see it happening: no tool call counter, no round limit, no timeout, and the response listener is never called because the turn never ends.

The same shape reaches production the moment a user asks for a field the form does not have, which is not an unusual thing for a user to do.

### Expected

A cap on tool call rounds per turn, with a default, and an error to the response listener when it is reached. Anything that turns "for ever" into "gave up after N".

Clearing `ai-working` when a turn ends, whether it ended well or not, is worth doing regardless.

### Reproduce

The project below, route `/form`, and the second button, "fill: also the note the form does not offer". Watch the log:

```
grep -c "Executing tool call: get_form_state" target/run.log
```

The first button, which names only the fields the controller found, completes normally in one round and clears `ai-working`, so the two buttons are the same code and the difference is the sentence.

Found on 25.3.0-beta1, `vaadin-ai-extensions-flow`, with `gpt-4o-mini`.
