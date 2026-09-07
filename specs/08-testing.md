# 08 Testing

The old app had seven unit tests about date formatting and five TestBench classes. This one treats tests as the definition of done: every acceptance criterion in a feature document is a real test, and every feature document names the class that proves it.

## Tiers

| Tier | Artifact | Suffix | Runs with | Owns |
| --- | --- | --- | --- | --- |
| Unit | JUnit 6 | `*Test` | `mvn test` | Money and VAT arithmetic, state machine, slot computation, invoice numbering, sanitization, the mock provider |
| Browserless | `browserless-test-junit6`, version from the BOM | `*BrowserlessTest` | `mvn test` | Everything reachable through the component API: routing, security, binder and validation, signals, grid contents, multi user state |
| End to end | TestBench, profile `it` | `*IT` | `mvn verify -Pit` | Only what needs a real browser. The profile starts the application on port 8081, runs every `*IT` against a local Chrome and stops it again. Surefire never sees these, because `*IT` does not match its default includes, so the ordinary gate needs no browser |
| Live model | JUnit tag `live-ai` | `*Test` | `mvn test -Pai -Dsurefire.excludedGroups=` | The one test that leaves the machine. `LiveAssistantTest` proves the assistant really reaches OpenAI, which no mock can. Excluded from every default run |

Browserless testing is free since 25.1 and needs neither a browser nor a servlet container, which is why it carries the bulk of the suite. `browserless-test` is versioned independently of the platform and is never hand pinned.

## The boundary

Browserless owns routing, security annotations, form validation, signal propagation, service and data logic, grid contents through the data view, dialogs, and multi user or multi window state.

TestBench owns, and this list is closed at about ten classes:

| IT class | Why it needs a browser |
| --- | --- |
| `UploadDropZoneIT` | Real drop events on the drop zone |
| `ClipboardPasteIT` | Real clipboard paste of an image |
| `GridProEditIT` | Cell editing behaviour |
| `DatePickerMetadataIT` | Rendering of disabled days and custom part names in the calendar overlay |
| `InvoicePrintIT` | Print stylesheet, checked with an emulated print media |
| `PwaInstallIT` | Service worker registration and the offline page |
| `ChartsRenderIT` | Charts actually draw |
| `AuraDarkModeIT` | Both colour schemes render and contrast holds |
| `KitchenBoardPushIT` | Two real browsers watching one shared signal |
| `PageTitleIT` | That the tab and the breadcrumb trail read the route's own title. A `PageTitleGenerator` bean overrides every route, and the browserless tier cannot see it: it resolved the right titles while the browser showed the application name on every page |
| `SmokeIT` | Login, storefront, order, board, in one pass |
| `KitchenSummaryOverlayIT` | Whether opening the production summary takes width from the board. Only a real layout can answer it |
| `OrderDetailsBandIT` | Whether the item tiles in an expanded row reflow to the width of the table rather than the window. A layout question, not a component one |
| `ColdLoginThemeIT` | Whether the first page anybody sees has a theme. The stylesheet is added at runtime, so only a cold load in a browser can answer it |
| `InvoiceCsvExportIT` | Whether a file is actually downloaded, which is a response the server never sees the end of |
| `DashboardLayoutIT` | Whether a widget spans the columns it should, and stops spanning when the screen narrows |
| `DiagnosticsLayoutIT` | Whether panels share a row and a height |
| `UserAvatarIT` | Whether the header shows the person. The avatar sits inside a MenuBar item, where browserless `find` cannot see it |

Twelve of the eighteen are written. The six that are not are `ClipboardPasteIT`, `DatePickerMetadataIT`, `GridProEditIT`, `KitchenBoardPushIT`, `PwaInstallIT` and `UploadDropZoneIT`, and they are the six that need a gesture rather than an assertion: a real drop, a real paste, a double click that a synthesised event does not reproduce, a second browser, and the network turned off. `SpecConsistencyTest` holds those six by name and insists every other named class exists.

If a proposed IT is not on this list, it belongs in the browserless tier or the list changes deliberately in this document.

None of them is written. The list is a design decision about where the boundary sits, and it has held: everything else landed in the browserless tier, which is why that tier carries 218 tests. What is missing is the profile and the twelve classes, and until they exist the boundary is a claim rather than a result. `SpecConsistencyTest` knows this and keeps the list honest, so a class named here cannot be quietly forgotten.

## Multi user tests

The kitchen board and the order conversation are the reason `BrowserlessApplicationContext` exists. The pattern, from the showcase:

```java
try (BrowserlessApplicationContext app = SpringBrowserlessApplicationContext.createSecured(ctx, "com.vaadin.bakery");
     BrowserlessUserContext ana = app.newUser("ana@bakery.test", "BAKER");
     BrowserlessUserContext luis = app.newUser("luis@bakery.test", "BAKER");
     BrowserlessUIContext anaWindow = ana.newWindow();
     BrowserlessUIContext luisWindow = luis.newWindow()) {
    ...
}
```

Two rules learned the hard way: close windows before users and users before the application context, and call `runPendingSignalsTasks()` after mutating a shared signal from another session before asserting on the observing side.

## Determinism

| Source of flakiness | Rule |
| --- | --- |
| Wall clock | A `Clock` bean everywhere. `demo.shift-dates=false` and a fixed `demo.today` in the test profile |
| Random data | The dataset is seeded and static. Tests never generate their own orders unless the test owns them |
| The assistant | There is one provider and it is real, so every default run has no model at all. What the default run asserts is the half that needs none: the policy, the meter, the tools and their guardrails, the read only database. The turns that need a model are tagged `live-ai` and excluded |
| Sleeps | Forbidden. No `Thread.sleep` anywhere in the suite |
| Test order | Each test creates and cleans its own data. `@DirtiesContext` only where a shared signal makes it necessary |

## Natural language test cases

Every feature document ends with a table in this shape, and it is the contract between the specification and the code:

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| CART-03 | A cart with two croissants | The visitor sets the quantity to zero | The line disappears and the badge shows the remaining count | browserless | `CartBrowserlessTest` |

The `Verified by` column names a real class. A CI check parses every feature document and fails the build when a named class does not exist. That single check is what keeps agent written code and specifications from drifting apart.

## Coverage

The interesting number is that every acceptance criterion has a test, and that is what `SpecConsistencyTest` enforces: every class named in a feature document exists, and the ones that do not are listed by name rather than passed over.

JaCoCo holds a floor rather than a vanity number: 70 percent of lines, failing the build, measured over everything except `**/ui/**`. The views are excluded because a view is exercised by a browser, and counting them would measure how much of the application has a browser test, which is the question the `IT` list answers. The suite sits at 77 percent.

## What we run when

| Command | Contents |
| --- | --- |
| `./mvnw verify` | Unit plus browserless, default profile, no licence, no browser, no network. This is the gate for every story |
| `./mvnw test -Pai -Dtest=LiveAssistantTest -Dsurefire.excludedGroups=` | The live model check, run by hand before a demo |
| `./mvnw verify -Pit` | Not available yet, the profile is not written |
| CI on push | The first command only. The licence free path is the one CI protects |
| CI nightly | Not written yet. It was to run the first command against the newest 25.3 prerelease, to catch API drift |
