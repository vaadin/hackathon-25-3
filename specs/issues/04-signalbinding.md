REPO: vaadin/flow
TITLE: A signal bound text cannot be rebound and there is no way to release the binding
---
### Description

`HasText.bindText(Signal<String>)` returns a `SignalBinding`, and `SignalBinding` offers `onChange`, `hasCallbacks`, `fireOnChange` and `setInitialContext`. There is nothing that undoes the binding.

Binding a second time throws:

```
com.vaadin.flow.signals.BindingActiveException: Operation could not be performed because a binding is active.
	at com.vaadin.flow.component.SignalPropertySupport.bind(SignalPropertySupport.java:140)
	at com.vaadin.flow.component.button.Button.bindText(Button.java:270)
```

### Reproduction

Binding on attach is the natural place, because that is where the `UI` and its locale signal are available:

```java
component.whenAttached(ui -> {
    component.bindText(ui.localeSignal().map(locale -> component.getTranslation(locale, key)));
    return () -> { };
});
```

A `Dialog` attaches every time it opens, so the second open throws. In our application the product editor was one open away from it.

### Why it matters

The exception surfaces from inside an attach listener, with a stack full of framework frames and none of the application's, so it does not point at the line that bound the text.

### Expected

Either `SignalBinding` can be released, or `bindText` replaces an existing binding rather than refusing.

### Workaround

Bind once and let the binding outlive the detach, tracked with a flag in `ComponentUtil` data.

Found on 25.3.0-beta1.
