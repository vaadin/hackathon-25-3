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
| `ai` | Spring AI and a real model, needs `OPENAI_API_KEY`. Without it the assistant replays recorded answers |
| `observability` | Observability Kit 5, Prometheus and Grafana behind `docker compose up -d` |
| `postgres` | PostgreSQL instead of H2 |
| `production` | The production frontend bundle |

To run against a real model:

```
export OPENAI_API_KEY=...
./mvnw -Pai
```

The about page and the badge over the assistant panel both name the provider that is answering, so a demo never has to guess whether it is talking to OpenAI or replaying a cassette. The one test that really calls OpenAI is excluded by default:

```
./mvnw test -Pai -Dtest=LiveAssistantTest -Dsurefire.excludedGroups=
```

## What to look at

| Route | Why |
| --- | --- |
| `/shop` | Filters as signals, mirrored into the URL. Forty eight real photos |
| `/checkout/slot` | Closed weekdays, closures, lead time and capacity, all in the calendar, with remaining places per day |
| `/orders` | Hidden columns that genuinely cost nothing, row details independent of selection, translated grid i18n |
| `/kitchen` | One shared signal, every screen in the kitchen |
| `/orders/new` | The assistant filling a phone order, and the parser that works with no key and no network |
| `/admin/diagnostics` | Session locks, RPC traffic and data provider queries, from the service event bus |
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

- `specs/FEEDBACK-25.3.md`: missing APIs, awkward ones, bugs and workarounds in this release. The headline: `bindChildren` is documented for 25.3 and absent from the artifact, `peek()` on a computed signal throws, `LazyDataView.getItems()` divides by zero on a pageable grid, and static CSS imported from a stylesheet is redirected to the login view, which leaves the application unstyled with nothing in the log.
- `specs/FEEDBACK-PLATFORM.md`: the same, for things that predate this release.
- `specs/CHANGELOG-RISK.md`: every preview API in use, with its flag and its fallback.
- `REPORT.md`: how the build went, what it cost, and what is not done.

## Hackathon

- Platform version: 25.3.0-beta1
- Slack: #hackathon-25-3
- Documentation: https://vaadin.com/docs/next/
