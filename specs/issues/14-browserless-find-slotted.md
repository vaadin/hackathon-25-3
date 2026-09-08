REPO: vaadin/flow
TITLE: Browserless find() cannot see a component handed to another component

---
### Description

A browserless test finds components in the view's tree. Anything a component renders or holds for another component is outside it, so `find()` returns nothing for it. Four cases, all in one small project:

| Where the component is | `find()` |
| --- | --- |
| Added to the view | Found |
| Rendered into a grid cell by `addComponentColumn` | Not found |
| Set as a column header with `setHeader(Component)` | Not found |
| An item in a `GridContextMenu` | Not found |

The test in the reproducer says it in numbers:

```
expected: <4> but was: <1>    five checkboxes on screen, one in the tree
expected: <1> but was: <0>    the button set as a column header
```

Context menu items have a second problem: they are not attached until a client opens the menu, so `test(component).click()` refuses with "is not usable because it is not attached".

### Why it matters

This is a whole class of UI, not a corner: cell renderers, column headers and menus are where applications put switches, filter fields and column choosers. Each one becomes untestable in the fast tier the moment it goes where it belongs, and the failure is an empty result rather than an error, so it reads as "the component was not created".

We moved four assertions to the browser tier for this, and drove one set of checkboxes through their value instead of clicking them.

### Expected

Any of these would help, in order of usefulness:

1. Make rendered and slotted components findable.
2. Give the testers a way in: `gridTester.getCellComponent(row, column)`, `menuTester.getItems()`.
3. Failing both, say so in the browserless documentation, next to `find()`. A rule of thumb is enough: the finder sees the component tree, and a component handed to another component is not in it.

### Reproduce

[`14-browserless-find/`](https://github.com/vaadin/hackathon-25-3/tree/9cf6235d4bf170002c44ac69c0d51063376828ba/specs/issues/14-browserless-find), then `mvn test`. Two tests, both failing, one per case.

Found on 25.3.0-beta1 with browserless-test 1.2.0-alpha2.

### Getting the project

```
git clone --branch manolo --depth 1 https://github.com/vaadin/hackathon-25-3
cd hackathon-25-3/specs/issues/14-browserless-find
mvn test
```

