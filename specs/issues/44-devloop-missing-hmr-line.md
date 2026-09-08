REPO: vaadin/flow
TITLE: The dev loop drops its hmr line when a change set mixes Java with a stylesheet

---
### Description

Edit a Java class and a stylesheet under `META-INF/resources` in one batch, then `apply`. The output has

```
resources: copied 1 to the classpath
hot-reload: redefineClasses(3); onHotswap completed=true
```

and no `hmr:` line at all. The push did happen: we verified it by reading the computed style of the open page.

The same stylesheet alone reports

```
hmr: 1 resource(s) copied, pushed 1 stylesheet(s) in place
```

### Why it matters

The documented vocabulary has a line for a push that happened and a line for "no browser connected". Silence is neither, so the reader cannot tell a push that happened from one that was skipped, and the only way to find out is to go and look at the page.

### Expected

Report both halves of a mixed change set.

### Reproduce

1. `.vaadin/vaadin-dev start`, open a page
2. Edit one Java file and one CSS file under `META-INF/resources`
3. `.vaadin/vaadin-dev apply`

Found on 25.3.0-beta1 with `flow-devloop-daemon` 25.3.0-beta1.
