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

[`38-dark-mode.zip`](https://github.com/vaadin/hackathon-25-3/raw/1e50ad1f28e0c668ef34f8a87d9cec8ad8ab4626/specs/issues/projects/38-dark-mode.zip). `mvn spring-boot:run`, then `http://localhost:8138/`: a button per mechanism and a button per theme, with no theme declared on the app shell so neither leaks into the other.

Load a theme, press a dark button, and read the computed style of the body. Measured:

| Theme | `theme="dark"` on the body | `Page.setColorScheme(DARK)` |
| --- | --- | --- |
| Lumo | Dark. Background `rgb(35, 51, 72)`, text `rgba(235, 243, 255, 0.9)` | Text goes light, background stays transparent |
| Aura | Nothing. Text stays `oklch(0.15 0.0038 248)` and `color-scheme` stays `normal` | Dark. Text `oklch(1 0.002 260)`, `color-scheme: dark` |

The attribute is set either way, and under Aura it changes nothing on the page.

The bottom right cell is worth a second look on its own: under Lumo the colour scheme moves the text and not the background, so an application that sets only the modern mechanism gets light text on a white page.

### Getting the project

[`38-dark-mode.zip`](https://github.com/vaadin/hackathon-25-3/raw/1e50ad1f28e0c668ef34f8a87d9cec8ad8ab4626/specs/issues/projects/38-dark-mode.zip), 4 KB, sources only. Unzip it and:

```
mvn spring-boot:run
```

Found on 25.3.0-beta1.
