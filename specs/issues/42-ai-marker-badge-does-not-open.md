REPO: vaadin/web-components
TITLE: The AI field marker's badge does not open its popover

---
### Description

Any `FormAIController` with `setFieldMarkerEnabled(true)`: fill a field through a turn, then click the "AI" badge beside it. Nothing happens.

What we checked before reporting:

- the popover's `for` resolves to the badge
- its trigger is `["click"]`
- the badge is the topmost element at that point and takes the click
- no error reaches the console
- setting `popover.opened = true` from the console renders the whole thing correctly, including our own content provider

So the content and the provider are fine, and only the trigger is dead. Reproduced from a fresh page with one trusted click and nothing else touched.

### Why it matters

The badge is the only way a person reaches the source snippet, the confidence and the revert control. All three exist and none of them can be opened, which makes the marker a decoration.

### Expected

The badge opens the popover.

### Reproduce

Needs a licence and a model: fill one field through a real turn, then click the badge.

Found on 25.3.0-beta1 with `vaadin-ai-extensions-flow`.
