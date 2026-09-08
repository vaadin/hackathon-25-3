# 07 Observability

Two halves. Observability Kit 5 gives the production picture, and a diagnostics view built on the new `VaadinService` event bus gives the in application picture that no generic APM can show.

This epic is scheduled last, after the polish passes. It instruments screens rather than shaping them, and instrumenting a screen that is still being redesigned is work done twice. The free half, the diagnostics view, is built and works. The kit's half has now been run once, deliberately, to find out whether the profile as written produces anything: it does, and it needed one fix to be reachable. What is still not done is the Grafana dashboard and the demo script around it.

### What the one run measured

`./mvnw spring-boot:run -Pobservability -Dspring-boot.run.profiles=observability`, then a walk through login, the order board, the closures admin and the dashboard.

- `observability-kit-starter:5.0.0-beta1` resolves from the platform BOM, with `observability-kit-micrometer` and `observability-kit-spring` behind it. No agent, no `-javaagent`, no `agent.properties`, exactly as the page says.
- `/actuator/prometheus` serves 22 `vaadin_*` metric families: sessions total, active and duration; UIs total, active and `ui.access` timing; navigation timing by route and outcome; request, RPC and data query durations; session lock wait and hold; fetched rows and requested pages by route; and, because `vaadin.observability.client=true`, client side bootstrap duration and Web Vitals FCP and LCP by route. Every one is labelled by route, which is what makes "which view is slowest" answerable.
- `/actuator/vaadin/observability` answers `{"schemaVersion":1,"instrumentation":"active","insights":[]}`. Instrumentation is live and the insight list is empty, because an insight is a slow interaction and nothing in a walk through a local application is slow. Demonstrating Interaction Insights therefore needs a deliberately slow interaction rather than ordinary use, which the demo script has to stage.
- No licence complaint anywhere, and no telemetry backend was needed to read either endpoint.

### The panel that joins the two halves

The diagnostics view is the free half and it works in every build, which makes it the one screen whose reader is looking for the other half. So it carries an **Observability Kit** panel.

Running, it is three links: the metrics, the insights, and health. They open in a new tab and the browser asks for a password, because the actuator chain is stateless and this screen's session does not reach it, which the panel says in as many words.

In both states it also carries the dashboard command and four short lines under it. The first names the three programs, because the commands were on the screen and nothing said which of them was the application, which one remembered the numbers and which one drew them. The other three are the questions somebody actually asks about the middle one, each answered in a sentence: how a container reaches an application that is not in it (`host.docker.internal`, not `localhost`, which inside a container is the container), how it knows where to look (`ops/prometheus.yml` says so, and the application announces itself to nobody), and whether it has to authenticate (yes, with the credentials in that same file, on every request). Four lines rather than one block, because a paragraph on a screen is a paragraph nobody reads.

Not running, it names which of the three switches is off, because "it does not work" sends a reader to the wrong one: the dependency is not in the build, or `vaadin.observability.enabled` is not true, or the actuator is not publishing the endpoint. `ObservabilityStatus` answers that, and it looks the kit up by class name rather than importing it, because the default build does not have it. Then the two commands:

```
./mvnw spring-boot:run -Pobservability -Dspring-boot.run.profiles=observability

./mvnw package -Pobservability -Pproduction
java -jar target/bakery-*.jar --spring.profiles.active=observability
```

Both halves are needed in both cases, and that is the mistake worth preventing: the Maven profile adds the dependencies and the Spring profile switches the properties on. The commands are constants in the view rather than bundle entries, because a shell command is the same in every language and a bundle is the one place where somebody would helpfully translate a flag. Every sentence around them is translated.

### The one fix it needed

The endpoints were not reachable. There was a single security chain and the Vaadin one accepts everything, so `/actuator/prometheus` answered a scrape with a 302 to the login page: the committed Prometheus job would have collected a login form every five seconds, and the Grafana dashboard would have been empty with nothing saying why.

The actuator now has a chain of its own, ordered first, matching `/actuator/**`, with HTTP Basic, no session and no CSRF, still requiring the admin role, and health still public. `ops/prometheus.yml` carries the credentials, which is what a scraper does.

It took one more fix to be usable by a person as well. An unauthenticated request was answered 302 to the login view, with the `WWW-Authenticate` header on it, which is the worst of both: a browser follows the redirect instead of asking for a password, and a scraper reads a login form. The redirect came from the container's error dispatch, which goes through the chain again and hits the chain that matches everything, so that chain now permits the error dispatch explicitly and the actuator chain carries its own Basic entry point. Measured after that: **401 with `WWW-Authenticate: Basic realm="Bakery metrics"`** and no credentials, 200 with an admin's, 403 with a barista's, and health 200 for anyone. That 401 is what makes the browser prompt, and therefore what makes the links on the diagnostics screen work.

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

`docker compose up -d prometheus grafana` starts both, provisioned from `ops/`, and it is built and was run: Prometheus scraped the application at `host.docker.internal:8080` with the credentials in `ops/prometheus.yml`, Grafana came up with the datasource and the dashboard already there, and every panel drew from the running application. Grafana is on 3000 with anonymous access, Prometheus on 9090.

```
ops/prometheus.yml                            the scrape job, with the admin credentials
ops/grafana/datasources/prometheus.yaml       the datasource, uid prometheus
ops/grafana/dashboards/bakery.yaml            the file provider, reloads within ten seconds
ops/grafana/dashboards/bakery-dashboard.json  the dashboard, uid bakery-vaadin
```

Four rows and twenty three panels. Every panel is built from a metric this application was measured serving, which is the rule that shaped the rows: the third row is locks and chatter rather than memory, because no UI state size metric exists to put in a memory row.

| Row | Panels | Reads |
| --- | --- | --- |
| Traffic | Sessions now, UIs now, navigations per minute, slowest navigation, navigation time by route, requests per second by kind | Sessions 4, UIs 7, and `uidl`, `static`, `other` and `heartbeat` separated, which is how a chatty view is spotted before anybody complains |
| Health | Uncaught client errors, failed navigations in the last hour, failed navigations by route, largest contentful paint by route, first contentful paint by route, client bootstrap by route | Two uncaught client errors, which are real and worth chasing, and Web Vitals per route measured in the browser rather than on the server |
| Locks and chatter | Session lock wait by context, session lock held worst case, RPC per second by type, `ui.access` time, sessions started and ended per minute | The lock panels separate `request` from `access`, so a background job holding the lock is not confused with a browser waiting its turn. RPC splits into `event`, `mSync` and `publishedEventHandler` |
| Data | Rows per fetch, fetch queries per minute, rows fetched per second by route, pages requested per second by route, query time filtered against unfiltered, queries per second count against fetch | Rows per fetch 56 and every data panel labelled by route, which is the half a generic APM cannot tell you. Filtered and unfiltered are separated because a combo box searching and a grid turning a page are not the same cost |

Two details the queries had to get right, and both are the kind of thing that makes a dashboard look broken. A Micrometer summary arrives as `_count`, `_sum` and `_max`, so an average is `rate(sum) / rate(count)` and the worst case is the `_max` gauge, never an average of averages. And every route grouped query excludes `route=""`, which is the series for requests belonging to no view: left in, it draws a line labelled "Value" in the legend of every panel and means nothing.

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
| Observability Kit | `ObservabilityStatus`, which looks the kit up by name rather than importing it | Where the other half is, or how to start it. Three links when it is running, and when it is not, the switch that is off and the command for development and for production |

The hidden column demonstration is written as an acceptance test, not just as a panel: open the order board, record the query count, hide the expensive column, reload the same page of data, and assert the count did not grow.

## Background work

The nightly summary job is the excuse to exercise the rest.

- The job runs off the UI thread and pushes its result with `UI.triggerAfter`, so no push connection is required for a deferred callback.
- If the UI went away while the job ran, the undelivered invocation warning is logged and the diagnostics view counts it.
- `UI.getLastUpdateSentTimestamp` drives a "this screen may be stale" banner after two minutes of silence on the kitchen board. Not built: nothing shows a stale banner today.

The first point needs rereading now. `Application` is annotated `@Push`, because the assistant streams its answer a token at a time and those tokens reach the browser no other way. So this application does have a push connection, and the demonstration that a deferred callback does not need one has to be staged deliberately rather than being the ambient condition it was written as. The diagnostics view still schedules the callback and still reports how long the UI had been silent, which is the part worth showing.

## Load

The `bulk` profile was to inflate the dataset to 50k orders programmatically, with a small generator driving navigations so the graphs have shape during a demo, never part of the committed SQL and never in CI. It is not built: there is no such profile and no generator, so a demo runs on the seeded dataset and the graphs have the shape that gives them.
