# Bakery 25.3

A rewrite of the Vaadin Bakery starter on Vaadin 25.3, built specification first, for the 25.3 hackathon.

The original starter is a museum piece: Lit templates with `@Id` injection, a presenter layer, an add on data provider, display DTOs full of preformatted strings, a runtime generator that fabricates twenty thousand orders on every boot, no images, no translations, seven trivial tests. This is a new application that does the same job and a good deal more, written to exercise what Vaadin 25.3 actually added.

Nothing is copied from the old code. It was read for behaviour only.

## What it is

A bakery that sells online and runs its counter. Visitors browse a catalogue and order as guests, choosing a pickup slot from a calendar that knows about closures, lead times and capacity. Counter staff take phone orders with an assistant that fills the form. Bakers work a board shared live between every screen in the kitchen. Administrators manage the catalogue, the people, the invoices and the metrics.

## Running it

```
./mvnw                     # the application on 8080, with the demo dataset
./mvnw verify              # unit and browserless tests: no licence, no browser, no network
./mvnw verify -Pit         # adds the browser tests
```

Sign in with `admin@bakery.test` / `admin`, `baker@bakery.test` / `baker` or `barista@bakery.test` / `barista`. They are demo credentials and they are in the source, on purpose.

The default build needs no commercial licence and no OpenAI key. Open `/about` to see exactly what is running on the machine in front of you.

| Profile | What it adds |
| --- | --- |
| none | Everything above. The commercial components are part of this build, see `specs/00-overview.md` |
| `ai` | Spring AI and a real model. It activates on its own when `OPENAI_API_KEY` is in the environment, so a machine with a key gets the assistant without asking for it. Without a key the assistant is off and says so: nothing replays a recording, there is no second implementation to fall back to |
| `observability` | Observability Kit 5, Prometheus and Grafana behind `docker compose up -d` |
| `postgres` | PostgreSQL instead of H2 |
| `production` | The production frontend bundle |

To run against a real model:

```
export OPENAI_API_KEY=...
./mvnw -Pai
```

The about page names the provider that is answering, so a demo never has to guess what it is talking to. The one test that really calls OpenAI is excluded by default:

```
./mvnw test -Pai -Dtest=LiveAssistantTest -Dsurefire.excludedGroups=
```

## Observability, end to end

Two halves, and the first one needs nothing at all.

**The diagnostics view is always there.** Sign in as the admin, open `/admin/diagnostics`, press **Reset the counters**, then go and use the application: open the order board, scroll the grid, filter the customer lookup on `/orders/new`, come back and press **Refresh**. Session locks, RPC traffic and data provider queries all move, and the per caller table names the component that fetched, `Grid` or `ComboBox (filtered)`. It is built on the 25.3 service event bus, so it costs no licence, no agent and no backend. The demonstration worth showing: hide the expensive column on the order board, fetch the same page again, and watch the query count not move.

The same view carries an **Observability Kit** panel, which is the other half's front door. With the kit running it is three links, the metrics, the insights and health, and they open in a new tab where the browser asks for a password. With the kit not running, which is the default build, it names which of the three switches is off and gives the command below, so nobody has to come back here to find it.

**The kit half needs the profile and, for the dashboard, Docker.**

```
./mvnw spring-boot:run -Pobservability -Dspring-boot.run.profiles=observability
```

The `-Dspring-boot.run.profiles` part matters: the Maven profile adds the dependencies and the Spring profile switches the properties on. Then, with the admin's credentials, because a scraper is not a person and the actuator has a security chain of its own:

```
curl -s -u admin@bakery.test:admin localhost:8080/actuator/prometheus | grep '^vaadin' | head
curl -s -u admin@bakery.test:admin localhost:8080/actuator/vaadin/observability
curl -s localhost:8080/actuator/health          # this one is public
```

The first is 22 `vaadin_*` metric families, every one labelled by route. The second is Interaction Insights, which answers `"instrumentation":"active"` and an empty list until an interaction is slow enough to earn an entry, so a demo of it has to stage a slow one. Metrics only exist once something has happened: a freshly started application publishes seven series and nothing per route until somebody navigates.

### The graphs

Three programs, and only one of them is this application.

| Piece | What it does | Where |
| --- | --- | --- |
| The application | Publishes its numbers at `/actuator/prometheus`. A snapshot: no history, no graphs | Your machine, port 8080 |
| Prometheus | Asks for that page every five seconds and remembers it. That is the history | In Docker, port 9090 |
| Grafana | Draws what Prometheus remembers | In Docker, port 3000 |

```
Grafana (3000)   ->   Prometheus (9090)   ->   application (8080)
    draws               asks every 5s            /actuator/prometheus
```

Prometheus does the asking, so the application has to be running first. Docker only runs the other two, and none of it is needed to read the numbers: `curl` or the links on the diagnostics screen are enough.

**How does it reach the application, from inside Docker?** Through `host.docker.internal`, which Docker resolves to the machine running it. Not `localhost`, which inside a container is the container itself. If the application runs on another machine, put that host and port in `ops/prometheus.yml`.

**How does it know the URL?** Because `ops/prometheus.yml` says so, target and path. The application announces itself to nobody.

**Does it have to authenticate?** Yes. The metrics need an administrator, so the same file carries the credentials and Prometheus sends them with every request.

Both commands run **from the project root**, the directory holding `compose.yaml`. Compose walks up from wherever you are looking for that file, so a subdirectory works too and the paths inside it stay correct: they resolve against the file rather than against your shell. From anywhere outside the project it fails with `no configuration file provided: not found`. What it needs there is `compose.yaml` and the two mounts it names, `ops/prometheus.yml` and `ops/grafana/`, and nothing else: Docker itself needs no state in the project.

```
# 1. the application, with the kit switched on
./mvnw spring-boot:run -Pobservability -Dspring-boot.run.profiles=observability

# 2. the two graphing programs, in another terminal, from the project root
docker compose up -d prometheus grafana

# 3. the dashboard, provisioned from ops, so nothing to click
open http://localhost:3000/d/bakery-vaadin

# 4. when you are done
docker compose stop prometheus grafana
```

If something is empty, check it in the order the data flows: that the application publishes, with the curl above; that Prometheus arrives, at `http://localhost:9090/targets`, where the target has to be green; and only then Grafana. Panels stay flat until somebody uses the application, because a metric does not exist until something has happened.

What each row answers, and where the metric names came from, is in `specs/07-observability.md`.

## What to look at

| Route | Why |
| --- | --- |
| `/shop` | Filters as signals, mirrored into the URL. Forty eight real photos |
| `/checkout/slot` | Closed weekdays, closures, lead time and capacity, all in the calendar, with remaining places per day |
| `/orders` | Hidden columns that genuinely cost nothing, row details independent of selection, translated grid i18n |
| `/kitchen` | One shared signal, every screen in the kitchen |
| `/orders/new` | A counter order filled from what the customer said, or from a photograph of a scribbled note. With no key the form is still a form and an order taken by hand still lands |
| `/admin/diagnostics` | Session locks, RPC traffic and data provider queries, from the service event bus. No licence and no backend needed |
| `/about` | Version, profiles, flags, and which assistant is answering |

## The specifications

`specs/` is the contract. It was written before any code and it is still the source of truth.

| Document | Contents |
| --- | --- |
| `specs/00-overview.md` | Vision, actors, every use case, the phases, the feature coverage matrix |
| `specs/01-domain-model.md` | Entities, constraints, invariants, and what was deliberately not modelled |
| `specs/02-data-set.md` | The static dataset and how it stays alive relative to today |
| `specs/03-architecture.md` | Packages, signals rules, forms, data access, internationalisation |
| `specs/04-security.md` | Roles, the route table, action level rules, the tracking link |
| `specs/05-theming.md` | Aura, light and dark, responsive, mobile, PWA, accessibility |
| `specs/06-ai.md` | Providers, the phone order flow, the policy layer, what happens with no model |
| `specs/07-observability.md` | Observability Kit 5 and the diagnostics view |
| `specs/08-testing.md` | Tiers, the browserless and browser boundary, determinism |
| `specs/09-tooling.md` | Profiles, feature flags, the dev loop, MCP servers, CI |
| `specs/features/` | One document per epic, with acceptance criteria and named test cases |
| `specs/user-stories/` | Fourteen epics with their stories |

## Feedback for the Vaadin teams

The point of building a real application on a beta is what it finds. Everything is written down as it happened:

- `specs/FEEDBACK-25.3.md`: missing APIs, awkward ones, bugs and workarounds in this release. The headline: a lazy Grid renders a select all checkbox that selects nothing, an AI turn can loop on one tool until the process is killed, `peek()` on a computed signal throws, and static CSS imported from a stylesheet is redirected to the login view, which leaves the application unstyled with nothing in the log.
- `specs/FEEDBACK-PLATFORM.md`: the same, for things that predate this release.
- `specs/CHANGELOG-RISK.md`: every preview API in use, with its flag and its fallback.
- `REPORT.md`: how the build went, what it cost, and what is not done.

## The issues this build opened

Thirty nine reports, one per finding, each with the project that reproduces it attached as a zip under `specs/issues/projects/`. They carry the `Hackathon 25.3:` prefix, and the platform's tracking issue lists them under its own Hackathon heading: https://github.com/vaadin/platform/issues/9204.

**vaadin/flow**, 28

- [#25557](https://github.com/vaadin/flow/issues/25557) A theme loaded with Page.addStyleSheet does not reach component shadow roots, so screen reader only text is painted
- [#25556](https://github.com/vaadin/flow/issues/25556) Query with a null sort order list throws an NPE inside VaadinSpringDataHelpers
- [#25558](https://github.com/vaadin/flow/issues/25558) A PageTitleGenerator annotated @Component silently becomes the whole application's generator
- [#25559](https://github.com/vaadin/flow/issues/25559) Dev loop reports Stable for a new Spring bean, and the bean is not registered
- [#25560](https://github.com/vaadin/flow/issues/25560) The dev loop daemon compiles without the project's compiler flags
- [#25561](https://github.com/vaadin/flow/issues/25561) An invalid tool schema logs a Jackson error that never names the tool
- [#25562](https://github.com/vaadin/flow/issues/25562) Browserless find() cannot see a component handed to another component
- [#25563](https://github.com/vaadin/flow/issues/25563) UI.navigate(String) rejects a URL with a query string
- [#25564](https://github.com/vaadin/flow/issues/25564) SignalOperation.result() never completes for a shared signal in a browserless test
- [#25565](https://github.com/vaadin/flow/issues/25565) Four ways the browserless environment differs from a running application
- [#25566](https://github.com/vaadin/flow/issues/25566) Page.addStyleSheet has two problems when sheets are swapped at runtime
- [#25567](https://github.com/vaadin/flow/issues/25567) FeatureFlags.setEnabled rewrites the project's source file
- [#25568](https://github.com/vaadin/flow/issues/25568) FormAIController does not walk into a Composite, so fields inside one are invisible to the model
- [#25569](https://github.com/vaadin/flow/issues/25569) An AI turn that produces nothing leaves no trace on any surface
- [#25570](https://github.com/vaadin/flow/issues/25570) Source tracking is enabled and no model ever reports a source
- [#25571](https://github.com/vaadin/flow/issues/25571) Three deprecations that do not name a replacement
- [#25572](https://github.com/vaadin/flow/issues/25572) Four small things about the dev loop CLI
- [#25573](https://github.com/vaadin/flow/issues/25573) Three things the AI API cannot do that an application needs
- [#25574](https://github.com/vaadin/flow/issues/25574) Two server side gaps: an absolute route URL, and asking what is licensed
- [#25575](https://github.com/vaadin/flow/issues/25575) Dark mode has two mechanisms, and the older one fails silently under Aura
- [#25576](https://github.com/vaadin/flow/issues/25576) CSS files pulled in with @import from a stylesheet are blocked by the default security setup
- [#25577](https://github.com/vaadin/flow/issues/25577) GridAIController cannot render a SQL TIME column
- [#25578](https://github.com/vaadin/flow/issues/25578) MenuConfiguration.getPageHeader calls a title generator with no route parameters
- [#25579](https://github.com/vaadin/flow/issues/25579) The dev loop drops its hmr line when a change set mixes Java with a stylesheet
- [#25580](https://github.com/vaadin/flow/issues/25580) Four older behaviours that cost a real application time
- [#25581](https://github.com/vaadin/flow/issues/25581) A FormAIController turn can loop on get_form_state until the process is killed
- [#25582](https://github.com/vaadin/flow/issues/25582) withResponseListener is called without a session lock, and the failure is swallowed
- [#25583](https://github.com/vaadin/flow/issues/25583) fill_form blocks for ever when a provider calls it on the thread it was handed

**vaadin/flow-components**, 3

- [#10062](https://github.com/vaadin/flow-components/issues/10062) EmailField shows no message for a bean validation failure
- [#10063](https://github.com/vaadin/flow-components/issues/10063) A lazy Grid renders a select all checkbox that selects nothing
- [#10064](https://github.com/vaadin/flow-components/issues/10064) No non-deprecated way to learn that a file reached an Upload whose handler belongs to a library

**vaadin/web-components**, 3

- [#12699](https://github.com/vaadin/web-components/issues/12699) AppLayout paints its content at the full window width before reserving the drawer's space
- [#12700](https://github.com/vaadin/web-components/issues/12700) A Chart ignores the theme until styled mode is turned on, and the docs that list the style properties do not say so
- [#12701](https://github.com/vaadin/web-components/issues/12701) A field's colspan cannot vary across FormLayout responsive steps

**vaadin/docs**, 4

- [#5974](https://github.com/vaadin/docs/issues/5974) Say on the download pages that the callback holds no session lock
- [#5975](https://github.com/vaadin/docs/issues/5975) Seven documentation gaps found while building a real application on 25.3
- [#5976](https://github.com/vaadin/docs/issues/5976) Three things the Observability Kit pages do not say
- [#5977](https://github.com/vaadin/docs/issues/5977) The container background tokens are not interchangeable between Lumo and Aura

**vaadin/testbench**, 1

- [#2305](https://github.com/vaadin/testbench/issues/2305) Say that the driver has to be unwrapped before CDP can be reached

Two findings are not here. They are real in this application and did not reproduce in a minimal project, so they are not worth anybody else's time yet: a signal bound text that cannot be rebound, and a push reconnect logged as an application error during a restart. Both are described in `specs/FEEDBACK-25.3.md`.
## Hackathon

- Platform version: 25.3.0-beta1
- Slack: #hackathon-25-3
- Documentation: https://vaadin.com/docs/next/
