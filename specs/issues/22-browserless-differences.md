REPO: vaadin/flow
TITLE: Four ways the browserless environment differs from a running application

---
### Description

All four cost an afternoon each, and each one is silent: the test passes, or finds nothing, rather than saying that the thing it needs is not there.

**1. Navigation cannot carry query parameters.** `navigate("track/ORD-1?t=abc", View.class)` throws "Base path can not contain query separator", and `navigate(Class, Map)` is route parameters, not query parameters. The way through is `UI.getCurrent().navigate(path, QueryParameters.of(...))` and then looking the view up.

**2. Session scoped Spring beans cannot be injected.** A `@VaadinSessionScope` bean autowired into a test class fails to resolve: there is no session at injection time. Reaching it through `context.getBean(...)` inside the test works.

**3. The security context does not reach the view.** `@WithMockUser` on the class is not enough, and the navigation the test sees is the anonymous one whoever it signed in as. A helper that signs in **after** the browserless environment is up is what works.

**4. A `PageTitleGenerator` bean is invisible.** The tier never calls it, so a test cannot assert the title an application computes.

### Why it matters

Each one reads as an application bug first. The security one is the worst: a test that signs in as an admin and sees the anonymous navigation looks like a broken security configuration, and the configuration is fine.

### Expected

Anything that turns a silent difference into a message. For 1, accept the query string or name the overload in the exception. For 2 and 3, a documented order of setup, or support for the annotations that already exist. For 4, call the generator.

A "how the browserless environment differs" page would cover all four.

### Reproduce

Each is two or three lines in a browserless test. The bakery's own tests carry the workaround for each, named in `specs/FEEDBACK-25.3.md`.

Found on 25.3.0-beta1 with browserless-test 1.2.0-alpha2.
