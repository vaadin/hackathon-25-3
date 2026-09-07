# Epic 13: Observability and platform events

**Goal:** measure the application, and prove two claims the other epics make.
**Feature spec:** `specs/features/13-observability.md`, full design in `specs/07-observability.md`
**Dependencies:** epics 06 and 11.

---

### US-13.1: Observability Kit

**Tasks:**
- [x] `observability` profile with the starter, actuator and the Prometheus registry
- [ ] `vaadin.observability.*` configuration, endpoints restricted to admin except health
- [ ] `docker compose` with Prometheus and Grafana, plus a committed dashboard JSON with the four rows

**Verified by:** `ObservabilityEndpointTest`

---

### US-13.2: Diagnostics view

**Tasks:**
- [x] `/admin/diagnostics` with panels for session lock contention, RPC traffic, data provider queries and stale UI
- [x] Listeners registered on the `VaadinService` event bus, released on shutdown
- [x] Works with the default profile, because the event bus is free platform

**25.3 APIs:** VaadinService event bus.
**Verified by:** `DiagnosticsBrowserlessTest`

---

### US-13.3: Proving the claims

**Tasks:**
- [x] Query counter used by `HiddenColumnCostBrowserlessTest` to prove hidden columns cost nothing
- [ ] Same counter used by `DashboardQueryCostBrowserlessTest` to hold the dashboard under ten queries

**Verified by:** `HiddenColumnCostBrowserlessTest`, `DashboardQueryCostBrowserlessTest`

---

### US-13.4: Background work

**Tasks:**
- [ ] Nightly summary job delivering through `UI.triggerAfter` with no push connection
- [ ] Undelivered invocation counter and log warning when the UI is gone
- [ ] Stale banner from `UI.getLastUpdateSentTimestamp`

**25.3 APIs:** `UI.triggerAfter`, `UI.getLastUpdateSentTimestamp`, undelivered invocation warnings.
**Verified by:** `TriggerAfterBrowserlessTest`

---

### US-13.5: Load

**Tasks:**
- [ ] `bulk` profile inflating to 50k orders programmatically, never in SQL, never in CI
- [ ] A small navigation generator so the graphs have shape during a demo

**Verified by:** manual, recorded in `DEMO.md`

---

## Left undone

- `compose.yaml` starts Prometheus and Grafana and `ops/prometheus.yml` is committed. `ops/grafana` is an empty directory: there is no dashboard JSON, so a demo would build the four rows by hand.
- `vaadin.observability.*` is never configured, and nothing has ever run with the profile on, in the suite or in CI.
- The nightly summary job does not exist. The diagnostics view schedules a deferred callback on a button, which shows the same API, and `Application` is now annotated `@Push` for the assistant, so the no push connection premise needs restaging.
- No stale banner.
- US-13.5 is not built: no `bulk` profile, no navigation generator.
- `DashboardQueryCostBrowserlessTest` does not exist, so only one of the two proof tests is green.

## Definition of Done

- [ ] Every acceptance criterion in `features/13-observability.md` is checked
- [ ] Both proof tests are green and referenced from the features they defend
