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

[`10-devloop/`](https://github.com/vaadin/hackathon-25-3/tree/9cf6235d4bf170002c44ac69c0d51063376828ba/specs/issues/10-devloop), which has one stylesheet and three views. With a page open at `/first`, so that there is a browser to push to:

Edit only the colour in `src/main/resources/META-INF/resources/styles/app.css`, then apply:

```
change-set: 1 file(s): .../styles/app.css
resources: copied 1 to the classpath
frontend → Stable   (0.01s)
hmr: 1 resource(s) copied, pushed 1 stylesheet(s) in place
```

Now edit the colour and one Java file together, and apply:

```
change-set: 2 file(s): .../FirstView.java, .../styles/app.css
resources: copied 1 to the classpath
compiling → runtime → Stable   (0.58s)
hot-reload: redefineClasses(1); onHotswap completed=true
```

The push happened both times. Reading the open page after the second one gives the new colour, `rgb(173, 20, 87)`, with no reload. Only the reporting is missing.

Found on 25.3.0-beta1 with `flow-devloop-daemon` 25.3.0-beta1.

### Getting the project

```
git clone --branch manolo --depth 1 https://github.com/vaadin/hackathon-25-3
cd hackathon-25-3/specs/issues/10-devloop
mvn spring-boot:run
```

