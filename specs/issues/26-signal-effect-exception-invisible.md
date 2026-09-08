REPO: vaadin/flow
TITLE: An exception inside Signal.effect is close to invisible

---
### Description

An effect that throws produces no stack trace where the developer is looking. The effect stops running, the UI stops updating, and the log has nothing that names the effect or the component that owns it.

### Why it matters

An effect is where a signals application puts the work that keeps the screen right, so a silent stop looks like a component that has gone dead. We chased one for half an hour before finding it by wrapping the body in a try and logging by hand.

### Expected

Log the exception, with the component the effect was registered on. Anything is better than silence: the effect has an owner, and the owner is what makes the message useful.

### Reproduce

```java
Signal.effect(component, () -> {
    var value = signal.get();
    throw new IllegalStateException("nobody will see this");
});
```

Found on 25.3.0-beta1.
