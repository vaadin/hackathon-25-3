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

The re-add half is in `38-dark-mode/`, route `/swap`. `mvn spring-boot:run`, then `http://localhost:8138/swap` and press the buttons in order, listing `link[rel=stylesheet]` after each one:

| After | Sheets on the page |
| --- | --- |
| load | `lumo.css aura.css` |
| `remove()` and `addStyleSheet(same URL)` in one round trip | `aura.css` |
| `remove()` alone | `aura.css` |
| `addStyleSheet(same URL)` in its own round trip | `lumo.css aura.css` |

Row two is the bug: the sheet is gone and the add that was meant to bring it back did nothing. Row four is the same call, one round trip later, working. Nothing is logged in either case.

```java
sheet.remove();
sheet = page.addStyleSheet(Lumo.STYLESHEET);   // dropped as a duplicate
```

The order half is not reduced. It is real in an application that swaps two sheets, and it depends on which sheet happens to land last, so there is nothing here that reproduces it on demand. Treat it as the observation that the API says nothing about order.

Found on 25.3.0-beta1.
