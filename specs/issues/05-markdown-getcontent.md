REPO: vaadin/flow
TITLE: Markdown.getContent() throws when the content is bound to a signal
---
### Description

```java
var markdown = new Markdown(someSignal);
markdown.getContent();
```

throws `BindingActiveException` from `SignalPropertySupport`.

### Why it matters

It is a getter. Reading the current value should not be refused, and a test that wants to know what a bound component is showing has no way to ask the component.

### Expected

The getter returns the value the component is currently showing.

### Workaround

Read the signal, or the element property.

Found on 25.3.0-beta1.
