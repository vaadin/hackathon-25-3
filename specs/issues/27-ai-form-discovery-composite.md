REPO: vaadin/flow
TITLE: FormAIController does not walk into a Composite, so fields inside one are invisible to the model

---
### Description

`FormAIController` discovers fields by walking the container's tree, recursing into anything that implements `HasComponents`. A `Composite` does not: it holds its content and is not a `HasComponents` itself, so discovery stops there.

The result is that a field inside a `Composite` is not in the form state the model is given. Nothing says so: the model simply never mentions that field.

### Why it matters

A `Composite` is the recommended way to build a reusable piece of UI, so the fields most likely to be inside one are the ones an application wrote itself.

Ours was a pickup slot picker, a date and a time inside a `Composite`. The model filled the customer's name and the order lines and left the slot alone, and the reason took a while to find because an empty field looks like a model that chose not to answer.

### Expected

Walk into a `Composite` by taking its content. If that is deliberate, say so on the AI Form Filler page next to the paragraph about field discovery, so an application knows to pass the inner container instead.

### Workaround

We gave the model a tool of our own for the slot, which was the right shape anyway because a slot can be refused. For an ordinary field it would be an unnecessary detour.

### Reproduce

```java
public class NamePart extends Composite<VerticalLayout> {
    private final TextField name = new TextField("Name");
    protected VerticalLayout initContent() { return new VerticalLayout(name); }
}

var form = new FormLayout(new NamePart(), new EmailField("Email"));
new FormAIController(form);   // the model sees Email and not Name
```

### What a live run showed

Reproduced with the project below. A `FormLayout` with two `TextField` children and one `Composite` whose content holds a third field, source tracking on, one live turn against `gpt-4o-mini`:

```
name                         value=Ana Torres               source=(none)
phone                        value=600123456                source=(none)
note (inside the Composite)  value=(empty)                  source=(none)
```

Stronger than the row that started this: the nested field was passed to `describeField`, so the application named it explicitly, and it was still never offered to the model and never filled. Describing a field the walker did not find does not add it.

### Getting the project

[`ai-live.zip`](https://github.com/vaadin/hackathon-25-3/raw/ea604a95b7f10720590dcd71c0e6955068f1d223/specs/issues/projects/ai-live.zip), 10 KB, sources only: four routes, one per finding, and the feature flag file the AI components need.

```
export OPENAI_API_KEY=...
mvn spring-boot:run
```

Then `http://localhost:8140/form`. It really calls the model, so it costs a few cents a run.

Found on 25.3.0-beta1 with `vaadin-ai-extensions-flow`.
