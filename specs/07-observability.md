# 07 Observability

Two halves. Observability Kit 5 gives the production picture, and a diagnostics view built on the new `VaadinService` event bus gives the in application picture that no generic APM can show.

This epic is scheduled last, after the polish passes. It instruments screens rather than shaping them, and instrumenting a screen that is still being redesigned is work done twice. The free half, the diagnostics view, is built; the kit's profile exists and has not been run yet, which is the schedule and not a gap.

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

`docker compose up -d` starts Prometheus and Grafana with a committed dashboard JSON at `ops/grafana/bakery-dashboard.json`. The dashboard has four rows:

| Row | Panels |
| --- | --- |
| Traffic | Sessions, UIs, navigations per minute, navigation timing by route |
| Health | Errors by route and component, connection state, Web Vitals |
| Memory | UI state size gauges by route, node counts, sessions over time |
| Data | Data provider queries per view, JDBC spans and fetch sizes by view |

Without a licence the kit degrades to no telemetry rather than a startup failure, and the about view says so.

## What we want the metrics to prove

A dashboard that nobody reads is decoration. Each of these is a question the bakery would actually ask, and the demo script walks them.

| Question | Where it is answered |
| --- | --- |
| Which view is slowest for real users | Interaction Insights, backtracking slow interactions |
| Which view leaks server side state | UI state size gauge by route |
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
