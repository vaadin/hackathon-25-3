REPO: vaadin/flow
TITLE: Four ways the browserless environment differs from a running application

---
### Description

All four cost an afternoon each, and each one is silent: the test passes, or finds nothing, rather than saying that the thing it needs is not there.

**1. Navigation cannot carry query parameters.** `navigate("track/ORD-1?t=abc", View.class)` throws "Base path can not contain query separator", and `navigate(Class, Map)` is route parameters, not query parameters. The way through is `UI.getCurrent().navigate(path, QueryParameters.of(...))` and then looking the view up.

**2. Session scoped Spring beans cannot be injected.** A `@VaadinSessionScope` bean autowired into a test class fails to resolve: there is no session at injection time. Reaching it through `context.getBean(...)` inside the test works.

**3. The security context does not reach the view.** `@WithMockUser` on the class is not enough, and the navigation the test sees is the anonymous one whoever it signed in as. A helper that signs in **after** the browserless environment is up is what works.

**4. A `PageTitleGenerator` registered only as a bean is never applied.** The title stays empty, so a test cannot assert what an application computes. With `@DynamicPageTitle(Generator.class)` on the view the tier does call it, so this is specifically the bean only path, which is the path an application falls into by accident: a generator annotated `@Component` becomes the whole application's title in a browser, and nothing at all in a test.

### Why it matters

Each one reads as an application bug first. The security one is the worst: a test that signs in as an admin and sees the anonymous navigation looks like a broken security configuration, and the configuration is fine.

### Expected

Anything that turns a silent difference into a message. For 1, accept the query string or name the overload in the exception. For 2 and 3, a documented order of setup, or support for the annotations that already exist. For 4, call the generator.

A "how the browserless environment differs" page would cover all four.

### Reproduce

`specs/issues/22-browserless-differences/`, then `mvn test`. Two tests, and they say different things:

- `navigationCannotCarryAQueryString` **passes**: it documents the refusal, and that the tier wraps what the router throws, so the message is on the cause.
- `aPageTitleGeneratorBeanReachesTheTitle` **fails**, `expected: <computed by the generator> but was: <>`. That failure is the finding.

Differences 2 and 3 are not in the project: the first needs a session scoped bean and the second needs security wiring, and both are described above with what worked instead.

Found on 25.3.0-beta1 with browserless-test 1.2.0-alpha2.
