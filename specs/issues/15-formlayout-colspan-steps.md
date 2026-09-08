REPO: vaadin/web-components
TITLE: A field's colspan cannot vary across FormLayout responsive steps

---
### Description

In responsive steps mode, the column count and the label position are per breakpoint, and `setColspan` is one static number for every breakpoint. The web component clamps it to the active step:

```js
colspan = Math.min(colspan, columnCount)   // responsive-steps-layout.js
```

So a field's effective span is `min(colspan, columns)` and there is no way to say "two columns wide when there are four, one when there are two".

### Why it matters

The ordinary responsive form is exactly that: four filters in a row when wide, one of them across the top of the others when medium, one column when narrow. Today you get two of the three, and the middle state is the one you lose.

Auto responsive mode does not help either: `setMaxColumns(4)` with four fields added directly to the layout renders **one** column, because `--_max-columns` is capped by the widest row the component finds in the content, and with `autoRows` off every direct child resets the counter. `setAutoRows(true)` or `addFormRow` unlocks it, and neither is mentioned where `setMaxColumns` is.

### Expected

A per step colspan. Something like `step.colspan(field, 2)`, or accepting a list on `setColspan` the way the steps themselves are a list.

For the auto responsive half: say in the `setMaxColumns` documentation that the content caps it, or do not cap it.

### Reproduce

```java
var layout = new FormLayout();
layout.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("40em", 2),
        new ResponsiveStep("60em", 4));
layout.add(search, category, without, sort);
layout.setColspan(search, 3);   // 3 when there are 4 columns, and 3 clamped to 2 when there are 2
```

Found on 25.3.0-beta1.
