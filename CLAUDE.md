# Bakery 25.3

A rewrite of the Vaadin Bakery starter on Vaadin 25.3, built specification first. Read `specs/` before writing code. The specifications are the source of truth, not this file and not the existing code.

Start at `NEXT.md`: where the work stopped, what to do next, and the traps that are already known.

## Stack

| Thing | Version |
| --- | --- |
| Vaadin | 25.3.0-beta1 |
| Spring Boot | 4.0.x |
| Java | 21, JetBrains Runtime recommended |
| Database | H2 by default, PostgreSQL under the `postgres` profile |
| Tests | JUnit 6, `browserless-test-junit6`, TestBench only where a browser is required |

## Where things are

```
specs/                 the source of truth, start at 00-overview.md
  features/NN-*.md     behaviour, acceptance criteria and the natural language test cases
  user-stories/        epics with US-N.M stories and tasks
src/main/java/com/vaadin/bakery/
  base/                shell, signals helpers, i18n, security, error handling
  catalogue/ ordering/ billing/ people/ assistant/ diagnostics/
src/main/resources/META-INF/resources/   all CSS and images
scripts/               dataset and image generators
```

## Hard rules

1. Never copy from the old Bakery at `/Users/manolo/Github/starters/bakery-app-starter-flow-spring`. Read it for behaviour, write new code.
2. No Lit or Polymer templates. Views are Java.
3. Signals first. Cross component state is a signal, not a listener chain.
4. No user visible string literal in Java. Everything goes through the translation bundles, English and Spanish. The one exemption is a view Copilot generates and regenerates under a `__copilot/` route: it is developer tooling, it is replaced on every regeneration, and `NoHardcodedStringsTest` prints the files it skips for that reason.
5. CSS lives in `src/main/resources/META-INF/resources`, imported from one `styles.css`. No `themes/` folder, no `@CssImport`.
6. Views never touch repositories. Services are the only transaction boundary.
7. Subscriptions register in `Component.whenAttached` and release through the returned registration. Do not override `onAttach` or `onDetach`. Same exemption as rule 4 for a generated `__copilot/` view.
8. The default build must boot and pass tests with no commercial licence, no OpenAI key and no network.

## Using the Vaadin MCP

Always query the docs server before writing against a 25.3 API. It is a beta and knowledge of 25.2 is not good enough.

- Endpoint: `https://mcp.vaadin.com/docs-java/docs`
- Always pass `ui_language: "java"` and `vaadin_version: "25.3"`
- This is a Vaadin Flow project. It is not React or Hilla

## Commands

| Command | Result |
| --- | --- |
| `./mvnw` | Runs the application on 8080 |
| `./mvnw verify` | Unit plus browserless tests, default profile. This is the gate |
| `./mvnw verify -Pit` | Adds the TestBench integration tests |
| `mvn flow:install-dev-cli` | Installs the dev loop. Note the prefix: in beta1 `vaadin:install-dev-cli` does not exist |
| `.vaadin/vaadin-dev status` `start` `apply` `restart` | The dev loop |

Never run the application through Maven and through the dev loop daemon at the same time.

## Dev loop

Installed and measured. The cycle, the command set and what each kind of edit costs are in `specs/10-dev-loop.md`, and the instructions the loop installed for every agent are in `.agents/skills/vaadin-devloop/SKILL.md`. Read that skill before editing a view, a component or a stylesheet.

The three things worth knowing up front:

- Open the browser **before** the first `apply`. A CSS push needs a page already connected.
- Do not batch a translation bundle with a Java edit while iterating: one resource turns a 0.8 second hot swap into a 7 second restart.
- It compiles Java only. The Kotlin about view is invisible to it and needs a restart.

## Definition of Done

A story is done when all of these hold:

1. `./mvnw verify` is green on the default profile, with no licence and no network.
2. Every acceptance criterion of the story's feature document is checked.
3. Every test case in that document's table exists, in the tier the table names.
4. The application boots and the touched view works, verified with Playwright.
5. No new user visible string literal, and both translation bundles have the same keys.

## Record every workaround

Whenever you cannot do something the obvious way, write it down. This is not optional bookkeeping: the point of building a real application on a beta is to find these.

| What you hit | Where it goes |
| --- | --- |
| A 25.3 API that is missing, awkward, or documented but absent | `specs/FEEDBACK-25.3.md` |
| Something that behaves the same on 24.x and earlier 25.x | `specs/FEEDBACK-PLATFORM.md` |
| Something that cannot be done at all | The same file, under missing APIs, saying what was impossible |
| A bug you routed around | The same file, naming the workaround and the file it lives in |

Say what you expected, what happened, what you did instead, and where the workaround lives. Never delete an entry when it gets fixed: mark it fixed, because the workaround still has to be removed.

Writing the row is half of it. Each finding becomes an issue in the repository that owns the thing, and the whole file feeds the hackathon report at the end. See **What happens to these** in `specs/FEEDBACK-25.3.md` for which repository, what an issue needs that a row does not, and where the link goes once it exists.

## Preview APIs

`specs/CHANGELOG-RISK.md` lists every preview, experimental or beta API in use, with its flag and its fallback. Read it before using a 25.3 component that is not in the stable set, and add to it if you introduce another one.
