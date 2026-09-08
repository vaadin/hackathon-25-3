REPO: vaadin/flow
TITLE: Four older behaviours that cost a real application time

---
### Description

None of these is new in 25.3. All four were hit while building one application, and all four are quiet.

**Navigating to the route you are already on does nothing.** `ui.navigate(SameView.class)` is a no operation, so the view is not rebuilt and anything computed in its constructor keeps the old value. It cost us two debugging rounds: once when a locale switch appeared not to translate, once when a cart page appeared not to notice that a product had gone out of stock. In both cases the code was right and the test was wrong.

**`Grid` has no `bindItems`.** Every other component got signal bindings. Grid did not, so the idiom is `Signal.effect(grid, () -> grid.setItems(...))`. It is the one place where the reactive story breaks and you drop back to imperative code, in the component people use most.

**A CSS grid inside a `VerticalLayout` collapses to one column.** `VerticalLayout` aligns children to the start, so a child is as wide as its contents, and a `display: grid` child with `repeat(auto-fit, minmax(20rem, 1fr))` has one column's worth of width to fit into. The rule reads as if it responds to the window and it responds to nothing. It cost us the same half hour twice, on two different views, before we recognised it.

**A `BeanValidationBinder` shows Hibernate Validator's English defaults.** A `@NotBlank` field reads "must not be blank", in English, in every language the application offers, because the message comes from the validation implementation's bundle and not from the Vaadin i18n provider. An application whose rule is that no user visible string is a literal still ships a screenful of them, and it is invisible until somebody submits an empty form in the second language.

### Expected

For the first: rebuild, or document that it does not. For the second: `bindItems` taking a `Signal<List<T>>`. For the third: a note in `VerticalLayout`'s documentation that children do not stretch, or stretch them. For the fourth: a line in the binder documentation saying where a constraint message comes from, and that `ValidationMessages.properties` is where to override it.

### Reproduce

[`45-older-behaviours/`](https://github.com/vaadin/hackathon-25-3/tree/9cf6235d4bf170002c44ac69c0d51063376828ba/specs/issues/45-older-behaviours), then `mvn test`. Two tests, both passing, and both passing is the finding: navigating twice builds the view once, and the constraint message is English.

Open `/stretch` in a browser for the third one: four panels in one column however wide the window is, with the one line fix commented out.

Found while building on 25.3.0-beta1, but none of them is specific to it.

### Getting the project

```
git clone --branch manolo --depth 1 https://github.com/vaadin/hackathon-25-3
cd hackathon-25-3/specs/issues/45-older-behaviours
mvn test
```

