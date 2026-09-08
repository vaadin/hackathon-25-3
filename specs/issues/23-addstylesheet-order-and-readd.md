REPO: vaadin/flow
TITLE: Page.addStyleSheet has two problems when sheets are swapped at runtime

---
### Description

An application that chooses its theme at runtime adds and removes stylesheets through `Page`. Two things go wrong.

**The order is not stable.** Two sheets added with `page.addStyleSheet`, then one of them replaced: the surviving sheet sometimes ends up after the new one and sometimes before. Nothing in the API orders them.

**Removing a sheet and adding the same URL back in one round trip loses it.** `registration.remove()` followed by `page.addStyleSheet(sameUrl)` in the same server visit leaves the page with no sheet at all. The two operations reach the browser together and the add is treated as a duplicate of a sheet that is still there. Nothing is logged.

### Why it matters

The first one produces colours that are right in one theme and wrong in the other, depending on which sheet happened to land last, which is a bug nobody can reproduce on demand.

The second one is worse: switching between two themes that share a sheet leaves the application unstyled, and the only clue is that it comes back after a reload.

### Expected

For the order: a documented order, or a way to say where a sheet goes.

For the re-add: treat a removal and an addition of the same URL in one round trip as a replacement, or log that the addition was dropped.

### Workaround

Win on specificity instead of order: `html:root` beats the plain `html` both themes use. And never touch what does not change: keep the shared sheet loaded and swap only the other one.

### Reproduce

```java
page.addStyleSheet("/styles/one.css");
var extra = page.addStyleSheet("/styles/two.css");
// later, in one round trip:
extra.remove();
page.addStyleSheet("/styles/two.css");   // the page ends up with neither
```

Found on 25.3.0-beta1.
