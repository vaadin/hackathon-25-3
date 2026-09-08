# 07 Observability

Two halves. Observability Kit 5 gives the production picture, and a diagnostics view built on the new `VaadinService` event bus gives the in application picture that no generic APM can show.

This epic is scheduled last, after the polish passes. It instruments screens rather than shaping them, and instrumenting a screen that is still being redesigned is work done twice. The free half, the diagnostics view, is built and works. The kit's half has now been run once, deliberately, to find out whether the profile as written produces anything: it does, and it needed one fix to be reachable. What is still not done is the Grafana dashboard and the demo script around it.

### What the one run measured

`./mvnw spring-boot:run -Pobservability -Dspring-boot.run.profiles=observability`, then a walk through login, the order board, the closures admin and the dashboard.

- `observability-kit-starter:5.0.0-beta1` resolves from the platform BOM, with `observability-kit-micrometer` and `observability-kit-spring` behind it. No agent, no `-javaagent`, no `agent.properties`, exactly as the page says.
- `/actuator/prometheus` serves 22 `vaadin_*` metric families: sessions total, active and duration; UIs total, active and `ui.access` timing; navigation timing by route and outcome; request, RPC and data query durations; session lock wait and hold; fetched rows and requested pages by route; and, because `vaadin.observability.client=true`, client side bootstrap duration and Web Vitals FCP and LCP by route. Every one is labelled by route, which is what makes "which view is slowest" answerable.
- `/actuator/vaadin/observability` answers `{"schemaVersion":1,"instrumentation":"active","insights":[]}`. Instrumentation is live and the insight list is empty, because an insight is a slow interaction and nothing in a walk through a local application is slow. Demonstrating Interaction Insights therefore needs a deliberately slow interaction rather than ordinary use, which the demo script has to stage.
- No licence complaint anywhere, and no telemetry backend was needed to read either endpoint.

### The one fix it needed

The endpoints were not reachable. There was a single security chain and the Vaadin one accepts everything, so `/actuator/prometheus` answered a scrape with a 302 to the login page: the committed Prometheus job would have collected a login form every five seconds, and the Grafana dashboard would have been empty with nothing saying why.

The actuator now has a chain of its own, ordered first, matching `/actuator/**`, with HTTP Basic, no session and no CSRF, still requiring the admin role, and health still public. Measured after the change: health 200 with no credentials, prometheus and insights 200 with an admin's Basic header, 403 with a barista's, 302 without any. `ops/prometheus.yml` carries the credentials, which is what a scraper does.

## Observability Kit 5

Behind the `observability` profile.

```xml
<dependency><groupId>com.vaadin</groupId><artifactId>observability-kit-starter</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
<dependency><groupId>io.micrometer</groupId><artifactId>micrometer-registry-prometheus</artifactId></dependency>
```

```properties
management.endpoints.web.exposure.include=health,prometheus,vaadin
vaadin.observability.enabled=true
vaadin.observability.client=true
vaadin.observability.traces=true
```

No agent JAR, no `-javaagent`, no `agent.properties`. Metrics at `/actuator/prometheus`, insights at `/actuator/vaadin/observability`.

`docker compose up -d` starts Prometheus and Grafana. **Not built, and the compose file promises it anyway:** it mounts `./ops/grafana` as Grafana's provisioning directory and that directory does not exist, so the dashboard below is a description of what to build and not of what is there. `ops/prometheus.yml` is real and now carries the credentials the scrape needs.

| Row | Panels | State |
| --- | --- | --- |
| Traffic | Sessions, UIs, navigations per minute, navigation timing by route | Every metric exists, measured. No dashboard |
| Health | Errors by route and component, connection state, Web Vitals | Web Vitals exist, FCP and LCP by route. No error or connection metric was seen in the 22 families |
| Memory | UI state size gauges by route, node counts, sessions over time | Sessions exist. **No UI state size metric exists**, so this row is a claim the kit does not support, or supports under a name that did not appear |
| Data | Data provider queries per view, JDBC spans and fetch sizes by view | Query durations, fetched rows and requested pages exist by route. No JDBC span was seen |

Without a licence the kit degrades to no telemetry rather than a startup failure, and the about view says so.

## What we want the metrics to prove

A dashboard that nobody reads is decoration. Each of these is a question the bakery would actually ask, and the demo script walks them.

| Question | Where it is answered |
| --- | --- |
| Which view is slowest for real users | Interaction Insights, backtracking slow interactions |
| Which view leaks server side state | UI state size gauge by route. **No such metric appeared in the one run**, so this question is currently unanswerable and the claim needs checking against the kit's own documentation before the demo relies on it |
| Which view is responsible for that spike in database time | JDBC spans attributed per view |
| Did the last deployment make navigation slower | Navigation timing by route, before and after |
| Are customers on bad connections losing the session | Client side connection state and Web Vitals |

## The diagnostics view

`/admin/diagnostics`, admin only, built on the 25.3 `VaadinService` event bus. This is the part that is specific to Vaadin and that Grafana cannot show.

| Panel | Source | What it demonstrates |
| --- | --- | --- |
| Session lock contention | Session lock requested, acquired and released events | Long held locks, and which request held them |
| RPC traffic | RPC invocation events | Chatty views, and synchronised property updates |
| Data provider queries | Count and fetch query events | The headline: toggle a hidden column in the order board and watch the query count not move |
| Stale UI detector | `UI.getLastUpdateSentTimestamp` plus undelivered JavaScript invocation warnings | A background job that finished while nobody was listening |

The hidden column demonstration is written as an acceptance test, not just as a panel: open the order board, record the query count, hide the expensive column, reload the same page of data, and assert the count did not grow.

## Background work

The nightly summary job is the excuse to exercise the rest.

- The job runs off the UI thread and pushes its result with `UI.triggerAfter`, so no push connection is required for a deferred callback.
- If the UI went away while the job ran, the undelivered invocation warning is logged and the diagnostics view counts it.
- `UI.getLastUpdateSentTimestamp` drives a "this screen may be stale" banner after two minutes of silence on the kitchen board. Not built: nothing shows a stale banner today.

The first point needs rereading now. `Application` is annotated `@Push`, because the assistant streams its answer a token at a time and those tokens reach the browser no other way. So this application does have a push connection, and the demonstration that a deferred callback does not need one has to be staged deliberately rather than being the ambient condition it was written as. The diagnostics view still schedules the callback and still reports how long the UI had been silent, which is the part worth showing.

## Load

The `bulk` profile was to inflate the dataset to 50k orders programmatically, with a small generator driving navigations so the graphs have shape during a demo, never part of the committed SQL and never in CI. It is not built: there is no such profile and no generator, so a demo runs on the seeded dataset and the graphs have the shape that gives them.
