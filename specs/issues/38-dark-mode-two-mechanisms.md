REPO: vaadin/flow
TITLE: Dark mode has two mechanisms, and the older one fails silently under Aura

---
### Description

`ui.getElement().getThemeList().bind("dark", signal)` sets `theme="dark"` on the body. That is what the 25.2 showcase does and what every Lumo era example shows. Aura does not read it: Aura follows the CSS `color-scheme` property, set through `Page.setColorScheme`.

So the same code makes Lumo dark and leaves Aura light, with nothing logged and nothing to notice except the colour.

A second, smaller trap sits beside it: `element.getThemeList()` returns an object that looks live, and if the `theme` attribute is unset when it is obtained, later changes are invisible to that instance. A test asserting on a captured `ThemeList` passes in one order and fails in another.

### Why it matters

An application that offers both themes has to set both mechanisms, and there is no page that says so. Ours sets the colour scheme and the theme attribute together, and the second one is only there for Lumo.

### Expected

One mechanism, or a page that says there are two and which theme reads which. If `getThemeList().bind` is the older path, deprecating it for this purpose would be clearer than leaving it working in one theme.

### Reproduce

```java
ui.getElement().getThemeList().bind("dark", darkSignal);   // Lumo goes dark, Aura does not
ui.getPage().setColorScheme(ColorScheme.DARK);             // both go dark
```

Found on 25.3.0-beta1.
