# Feedback for the Vaadin teams: not specific to 25.3

Findings that are older than this release. They were hit while building this application, but the behaviour is the same on 24.x and 25.0 to 25.2, so they belong to a different conversation than the 25.3 release notes.

Same rule as the other feedback file: one row per finding, never delete a row when it is fixed, mark it instead, because the workaround still has to be removed.

## Framework behaviour

| Finding | What happens | Why it matters | What we did |
| --- | --- | --- | --- |
| Navigating to the route you are already on does nothing | `ui.navigate(SameView.class)` is a no operation, so the view is not rebuilt and anything computed in its constructor keeps the old value | It cost us two debugging rounds: once when a locale switch appeared not to translate, once when a cart page appeared not to notice that a product had gone out of stock. In both cases the code was right and the test was wrong | Navigate away and back in tests, and bind anything that must react to a signal rather than computing it once in a constructor |
| `Grid` has no `bindItems` | Every other component got signal bindings. Grid did not, so the idiom is `Signal.effect(grid, () -> grid.setItems(...))` | It is the one place where the reactive story breaks and you drop back to imperative code, in the component people use most | Used the documented effect idiom. A `bindItems` taking a `Signal<List<T>>` or a `ListSignal<T>` would close the gap |
| `HasUrlParameter.setParameter` takes `BeforeEvent`, not `BeforeEnterEvent` | The interface is implemented with the wrong parameter type more often than not, and the compiler error names an abstract method rather than the mistake | Small, but it is a first ten minutes error for everyone | Nothing to do, worth a note in the javadoc |
| A CSS grid inside a `VerticalLayout` collapses to one column | `VerticalLayout` aligns its children to the start, so a child is as wide as its contents. A `display: grid` child with `repeat(auto-fit, minmax(20rem, 1fr))` therefore has one column's worth of width to fit into, and lays every panel out under the one before it | The rule reads as if it responds to the window and it responds to nothing. It looks like a broken media query, and it cost us the same half hour twice, on the dashboard and on the diagnostics view, before we recognised it the second time | `width: 100%` on the grid, every time. A `VerticalLayout` that stretched full width children by default, or a note in its documentation saying children do not stretch, would stop this being a rite of passage |
| Lazy collections and detached entities in views | Reading `order.getItems()` outside a transaction throws `LazyInitializationException`, so every view has to know which repository method fetches which graph | Entity graphs solve it, but the failure mode is a runtime exception in a view rather than something the type system prevents | Named entity graphs on the aggregate and repository methods that use them, documented in the architecture spec |

## Spring integration

| Finding | Detail | Suggestion |
| --- | --- | --- |
| `spring.jpa.defer-datasource-initialization=true` and `ddl-auto=validate` are silently incompatible | With deferral on, Hibernate validates before `schema.sql` runs, so the application fails to boot with "missing table". The Boot documentation recommends deferral for `data.sql`, and the Vaadin starters inherit that advice | A line in the Vaadin persistence documentation saying that an explicit schema plus `validate` means deferral must be off would prevent it |
| A browserless test that cannot be rolled back forces a context reload, which needs an idempotent schema | A test that drives a real flow through the UI cannot sit in a test transaction, so the only way to protect later tests from its writes is `@DirtiesContext`. Reloading the context then re-runs `schema.sql` against an in memory database that outlived the reload, and every `create table` fails. The symptom is unhelpful: "ApplicationContext failure threshold (1) exceeded" on 53 unrelated tests | We put `drop all objects` at the top of the H2 schema. A note in the browserless testing documentation about this combination would save the diagnosis |
| Session scoped beans and the servlet lifecycle | Covered in the 25.3 file for browserless tests, but the same trap exists in any code that runs before a session exists, such as an `ApplicationRunner` | A clearer error naming the missing Vaadin session, rather than the generic Spring scope message |

## Spring Boot 4 migration friction

| Finding | Detail |
| --- | --- |
| Spring test client classes moved package | `org.springframework.boot.test.web.client.TestRestTemplate` and `org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc` are no longer where every existing example and every model's memory expects them. The compiler error is just "package does not exist", which does not hint that the class still exists elsewhere | We used `java.net.http.HttpClient` instead, which needs no guessing. A note in the Vaadin testing documentation, which is full of Boot examples, would help |

## Tooling and ecosystem

| Finding | Detail |
| --- | --- |
| Service workers outlive the application they were built for | A `@PWA` service worker registered by an earlier build kept serving a cached shell after the routes changed, producing a redirect loop that looked like a security misconfiguration. Nothing in the dev tools suggested clearing it | In development, a way to disable or auto invalidate the service worker between restarts would save real time |
| The old Bakery starter is still the most visible example | It is deprecated in its own README, yet it is what people find. Every pattern in it, from Lit templates with `@Id` to display DTOs of preformatted strings, is now the wrong advice | Retiring it, or putting a modern rewrite next to it, would move the whole community forward faster than any single API |
