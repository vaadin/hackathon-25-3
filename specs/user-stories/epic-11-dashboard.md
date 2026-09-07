# Epic 11: Dashboard and analytics

**Goal:** the numbers that run a bakery, rendered well.
**Feature spec:** `specs/features/11-dashboard.md`
**Dependencies:** epics 02 and 06.

---

### US-11.1: Panels and range

**Tasks:**
- [ ] CSS grid layout, structural changes from `Page.windowSizeSignal()`
- [ ] Range selector as a `ValueSignal`, one `Signal.effect` rebuilding every series
- [ ] Counters as computed signals over one query result

**Verified by:** `DashboardBrowserlessTest`

---

### US-11.2: The six panels

**Tasks:**
- [ ] Today, Attention, Revenue, Orders by state, Top products, Slot utilisation
- [ ] Every number links to the filtered board or list behind it
- [ ] Empty states for ranges with no data, weekly aggregation beyond a year

**Verified by:** `DashboardBrowserlessTest`

---

### US-11.3: Commercial and free rendering

**Tasks:**
- [ ] Charts under the `commercial` profile
- [ ] Table with CSS bars and `ProgressBar` theme neutral variants, which is what the panels draw today
- [ ] Both paths tested against the same numbers, and named on the about page

**Verified by:** `DashboardFallbackBrowserlessTest`, `ChartsRenderIT`

---

### US-11.4: Query cost

**Tasks:**
- [ ] Fewer than ten queries per load, asserted with the diagnostics counter

**Verified by:** `DashboardQueryCostBrowserlessTest`

---

## Left undone

- Four of the six panels exist: Today, Revenue, Orders by state and Top products. Attention and Slot utilisation were never built.
- Nothing reads `Page.windowSizeSignal()`, here or anywhere else in the application, so the layout is CSS grid and nothing restructures itself.
- The numbers do not link anywhere. Reading one and then finding those orders is a manual search on the board.
- Weekly aggregation beyond a year is not implemented.
- There is no commercial path: Charts is not a dependency, so "both paths tested against the same numbers" has only one path to test.
- The query budget is unasserted, and `DashboardQueryCostBrowserlessTest` does not exist.

## Definition of Done

- [ ] Every acceptance criterion in `features/11-dashboard.md` is checked
