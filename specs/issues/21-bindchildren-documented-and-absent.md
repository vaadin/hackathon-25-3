REPO: vaadin/docs
TITLE: bindChildren is documented and does not exist in 25.3.0-beta1

---
### Description

The signals documentation describes binding a container's children to a `ListSignal`:

```java
container.bindChildren(listSignal, item -> new ItemComponent(item));
```

There is no such method on `HasComponents` or on `Component` in 25.3.0-beta1. The code does not compile.

### Why it matters

It is the documented way to render a list that changes, and it is the first thing anybody with a `ListSignal` looks for. Finding out costs a compile error and then a search through the javadoc for something that is not there.

Either the documentation is ahead of the artifact or the method was dropped late. Whichever it is, the page should match what ships.

### Workaround

An effect over the signal, rebuilding what changed:

```java
Signal.effect(container, () -> {
    var items = listSignal.get();
    // add, remove and reorder children to match
});
```

Ours is about forty lines and lives in one place, `base/signals/Children.java` in the bakery.

### Expected

Ship the method, or take the example off the page and put the effect idiom there instead.

Found on 25.3.0-beta1.
