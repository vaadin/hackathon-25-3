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

`15-formlayout-colspan/` in this directory. `mvn spring-boot:run`, then two routes.

`/steps` is the form above: four fields, steps at 1, 2 and 4 columns, and `setColspan(search, 3)`. The component lays it out with inline percentage widths, so the spans are readable as widths. Measured at three container widths:

| Container | Search | The other three | Rows used |
| --- | --- | --- | --- |
| 1200 px, 4 columns | `calc(75% - 4px)`, 896 px | 25% each, 288 px | 2 |
| 700 px, 2 columns | `calc(100% + 0px)`, 700 px | 50% each, 342 px | 3 |
| 400 px, 1 column | 100% | 100% each | 4 |

The middle row is the one to look at. Three columns clamps to two, which is the whole row, so Search takes the full width and the other three drop below it. There is no way to ask for one column there.

`/auto` is the auto responsive half: the same four fields, `setMaxColumns(4)`, twice, differing in one line. With `setAutoRows(false)` the computed `--_max-columns` is **1** and the fields land on four rows. With `setAutoRows(true)` it is **4** and they land on one. Nothing in the call says the content will cap it.

Found on 25.3.0-beta1.
