# Feature 11: Dashboard and analytics

## Overview

Replaces the old dashboard: a Lit template, a `vaadin-board`, five charts and a hand written performance marker. This one is Java, responsive by signal, and its charts follow the theme rather than carrying a palette of their own.

Covers A12.

## Behaviour

### Route `/admin/dashboard`

Layout is CSS grid, reflowing by breakpoint, with structural changes driven by `Page.windowSizeSignal()`.

| Panel | Content |
| --- | --- |
| Today | Orders due today, how many are ready, the next pickup time, and a progress bar of the day's slot utilisation |
| Attention | Orders in problem state, orders late for their slot, unread customer messages. Each number links to a filtered board |
| Revenue | Gross revenue per day over the selected range, with the previous period behind it for comparison |
| Orders by state | Distribution over the selected range |
| Top products | Units and revenue, top ten, over the selected range |
| Slot utilisation | Booked against capacity per slot for the selected day |

The range selector is a `ValueSignal`. One `Signal.effect` rebuilds every series when it changes. Counters are computed signals over one query result, not six separate queries.

### How the panels are drawn

Revenue, orders by state and top products are Charts. This build is licensed for them, so there is one rendering and no fallback beside it: see Licensing in `00-overview.md`. Charts are built in styled mode, which is what makes them read the theme's colour properties and follow the light and dark colour schemes instead of drawing Highcharts' own palette on a white plot area.

The counters and utilisation panels stay plain: a `Div` per counter and `ProgressBar` in its theme neutral variants. Nothing is gained by charting a single number.

### Performance

The dashboard is the heaviest page in the application and it is the one the observability epic measures. Every query it issues is attributable to this view in the JDBC spans, and the target is a single digit number of queries per load.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A range with no orders | Panels show an explicit empty state, never a blank chart |
| A range longer than a year | The query aggregates by week instead of by day |
| A narrow screen | Widgets reflow to fewer columns and the charts shrink rather than scroll |

## Acceptance criteria

### AC1: The numbers are right
- [ ] Every counter matches a direct query over the same range
- [ ] Changing the range updates every panel from one effect

### AC2: The panels are drawn with the right component
- [ ] Revenue, orders by state and top products are Charts, which this build is licensed for
- [ ] Every panel renders, and a range with no data says so rather than drawing nothing

### AC3: It is cheap
- [ ] A dashboard load issues fewer than ten database queries, asserted by the diagnostics counter

### AC4: It is responsive
- [ ] At phone width the panels stack and remain readable

### Still open

Nothing in this document is built yet.


## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| DASH-01 | The seeded data | Opening the dashboard | Today's counters match a direct query | browserless | `DashboardBrowserlessTest` |
| DASH-02 | The dashboard | Changing the range to last 7 days | Every panel updates | browserless | `DashboardBrowserlessTest` |
| DASH-03 | A range with no orders | Opening the dashboard | Every panel renders an empty state rather than nothing | browserless | `DashboardBrowserlessTest` |
| DASH-04 | A query counter running | Loading the dashboard | Fewer than ten queries | browserless | `DiagnosticsBrowserlessTest` |
| DASH-05 | A range with no orders | Opening the dashboard | Empty states, no exception | browserless | `DashboardBrowserlessTest` |
| DASH-06 | The dashboard | Opening it | Charts render | testbench | `ChartsRenderIT` |
