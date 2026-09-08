# Feature 13: Observability and platform events

## Overview

Specified in full in `07-observability.md`. This document holds the behaviour that has to be tested and demonstrated.

Covers A14, A15.

## Behaviour

Observability Kit 5 under the `observability` profile, exporting through actuator to Prometheus, with a committed Grafana dashboard. A diagnostics view at `/admin/diagnostics` built on the `VaadinService` event bus, with four panels: session lock contention, RPC traffic, data provider queries, and the stale UI detector.

The diagnostics view is not decoration. It is the instrument that proves two claims made elsewhere in these specifications:

1. Hiding a Grid column in 25.3 issues no query and runs no value provider, claimed in feature 06.
2. The dashboard loads with fewer than ten queries, claimed in feature 11.

Both are written as assertions, not as screenshots.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| No commercial licence | The kit degrades to no telemetry, the application boots, and the about page says so |
| The `observability` profile is off | `/admin/diagnostics` still works, because the event bus is part of the free platform. Only the Prometheus panels are absent |
| No Prometheus running | The view shows the local counters and a note that no backend is configured |
| A background job finishes after its UI is gone | The undelivered invocation warning is logged and the counter increments |

## Acceptance criteria

### AC1: Metrics are exported
- [ ] With the profile on, `/actuator/prometheus` returns Vaadin metrics including navigation timing and UI state size
- [ ] `/actuator/vaadin/observability` returns insights

### AC2: The diagnostics view needs no backend
- [x] Session lock, RPC and data provider panels populate under normal use with the default profile
- [x] A fetch is attributed to the component that issued it, and a filtered fetch is named as one, from the event rather than from the stack

### AC3: It proves the Grid claim
- [x] Toggling the expensive column on the order board does not increase the query counter

### AC4: Stale UI detection works
- [ ] A deferred callback delivered through `UI.triggerAfter` reaches the UI with no push connection
- [ ] A callback whose UI has gone increments the undelivered counter

### AC5: Access is controlled
- [x] Only an admin can open the diagnostics view or the actuator endpoints, apart from health

### Still open

- Neither endpoint criterion is tested. `ObservabilityEndpointTest` proves health is public and that the metrics endpoint is not, which is AC5, not AC1. Asserting the Vaadin metrics themselves needs a run with the `observability` profile, and nothing runs one.
- AC4 needs rereading rather than testing. `Application` is now annotated `@Push`, because the assistant streams its answer token by token and those tokens reach the browser no other way. The premise of both criteria, a deferred callback delivered with no push connection, no longer describes this application. The button in the diagnostics view still schedules the callback and still reports what it cost, so the interesting part survives, but the undelivered counter now needs a UI that has actually gone rather than one that merely cannot be reached.

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| OBS-01 | The observability profile | Requesting the Prometheus endpoint | Vaadin metrics are present | unit | `ObservabilityEndpointTest` |
| OBS-02 | The default profile | Opening the diagnostics view as admin | Session lock and RPC panels populate | browserless | `DiagnosticsBrowserlessTest` |
| OBS-03 | The order board with the counter running | Hiding the expensive column | The query count does not grow | browserless | `HiddenColumnCostBrowserlessTest` |
| OBS-04 | A background job | Completing after the UI detached | The undelivered counter increments and a warning is logged | browserless | `DiagnosticsBrowserlessTest` |
| OBS-05 | A barista | Opening the diagnostics view | Refused | browserless | `SecurityRulesTest` |
