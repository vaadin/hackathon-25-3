REPO: vaadin/flow
TITLE: A theme loaded with Page.addStyleSheet does not reach component shadow roots, so screen reader only text is painted
---
### Description

An application that chooses its theme at runtime has to load it by hand:

```java
UI.getCurrent().getPage().addStyleSheet(Lumo.STYLESHEET);
```

with no `@Theme` anywhere. The page looks right, and the rules the components rely on **inside their own shadow roots** never arrive. Utility classes the components use for accessibility stop working, and the first place it shows is a `Grid`.

A `Grid` in `SelectionMode.MULTI` with a lazy data provider renders

```html
<span class="sr-only">Select All unavailable</span>
```

into the selection column's header. With a declared theme that span is `position: absolute; clip-path: inset(50%)`, one pixel square. With the theme added at runtime it is `position: static; clip-path: none`, **140 pixels wide**, and because a header sizes its column the selection column goes from 35 pixels to 201.

So a string written for a screen reader is painted on screen, in English, inside an application that may be in another language, and it takes a fifth of the table with it.

### Reproduction

Two projects in [`01-grid-selectall/`](https://github.com/vaadin/hackathon-25-3/tree/9cf6235d4bf170002c44ac69c0d51063376828ba/specs/issues/01-grid-selectall) and [`01b-test-runtime-theme/`](https://github.com/vaadin/hackathon-25-3/tree/9cf6235d4bf170002c44ac69c0d51063376828ba/specs/issues/01b-test-runtime-theme), identical except for one line. Run either with `mvn spring-boot:run`.

| | Declared theme (port 8090) | `addStyleSheet` at runtime (port 8091) |
| --- | --- | --- |
| `span.sr-only` position | `absolute` | `static` |
| `clip-path` | `inset(50%)` | `none` |
| span width | 1 px | 140 px |
| selection column | 35 px | **201 px** |

`declared-theme.png` and `runtime-theme.png` are those two headers.

Measure it with:

```js
const th = document.querySelector('vaadin-grid').shadowRoot.querySelector('thead th');
[th.getBoundingClientRect().width, getComputedStyle(th.querySelector('.sr-only')).position];
```

### Why it matters

Nothing warns. The page is themed, the colours are right, and the only visible symptom is one English sentence inside a table, which reads as a `Grid` bug and is not one. Anything else a component hides with a class from the theme layer has the same problem and has not been noticed yet.

### Expected

Either a theme added at runtime delivers its component level rules to shadow roots the way a declared theme does, or `addStyleSheet` says that it will not.

### Workarounds

Neither is good.

`i18n.setSelectAllUnavailable("")` empties the span, which fixes the column and removes an accessibility announcement.

Declaring `@Theme` fixes it properly and gives up choosing the theme at runtime.

### Getting the project

```
git clone --branch manolo --depth 1 https://github.com/vaadin/hackathon-25-3
cd hackathon-25-3/specs/issues/01-grid-selectall
mvn spring-boot:run
```

The second project, `01b-test-runtime-theme`, is beside it and differs by one line.

Found on 25.3.0-beta1.
