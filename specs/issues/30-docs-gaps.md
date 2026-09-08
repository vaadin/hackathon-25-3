REPO: vaadin/docs
TITLE: Seven documentation gaps found while building a real application on 25.3

---
### Description

Each of these cost between twenty minutes and an afternoon, and each is a page that exists and does not say the thing.

**Signals outside a reactive context.** The rules are learnable and not written down in one place: `get()` only inside `computed` or `effect`, `peek()` anywhere, `untracked` when you mean it. `MissingSignalUsageException` teaches the first one the hard way.

**`@DynamicPageTitle`.** The annotation is named and the shape of the generator it needs is not: what interface, what the context carries, when it is called.

**Grid selection with lazy data.** Nothing says that the select all checkbox is unavailable, so an application discovers it by setting the visibility and watching nothing happen. See the separate issue for the API side.

**Charts style properties.** The page lists the CSS properties a chart honours and does not say that it honours none of them until `setStyledMode(true)`. See the separate issue.

**The dev loop CLI.** The reference table is pessimistic about what enhanced class redefinition absorbs: a new field plus a new method hot swapped in under a second, where the table says restart. Being told to expect a restart makes people batch edits to save restarts that were never going to happen.

**`MessageList` and markdown.** Every AI example passes a bare `new MessageList()`, and every model writes markdown, so the first live answer arrives with asterisks in it. `setMarkdown(true)` fixes it and no example calls it.

**TestBench's base class shadows `assertEquals`.** `TestBenchTestCase` brings its own, with the arguments the other way round, so a JUnit assertion silently compares the wrong way. The page that introduces the base class does not mention it.

### Why it matters

None of these is a defect. Together they are most of the time this application lost to the platform, which is the sort of thing a release wants to know.

### Expected

A sentence or a paragraph on each page. The signals one is worth a page of its own.

Found on 25.3.0-beta1.
