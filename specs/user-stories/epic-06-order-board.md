# Epic 06: Staff order board

**Goal:** replace the old storefront view with a Java grid that proves the 25.3 Grid improvements.
**Feature spec:** `specs/features/06-order-board.md`
**Dependencies:** epics 02 and 05.

---

### US-6.1: The board

**Tasks:**
- [x] `/orders` grid, lazy loaded through a Specification based provider
- [ ] Relative date group headers from a computed signal, pushed with `Signal.effect(grid, ...)`
- [x] Search over reference, name, phone and email, plus a past orders toggle
- [ ] Card layout below the breakpoint, driven by `Page.windowSizeSignal()`

**Verified by:** `OrderBoardBrowserlessTest`, `ResponsiveBoardBrowserlessTest`

---

### US-6.2: Columns that cost nothing when hidden

**Tasks:**
- [x] Column chooser with items summary, channel and total hidden by default
- [x] Make the items summary column deliberately expensive, and instrument its value provider with a counter
- [x] Test asserting no query and no value provider call while hidden

**25.3 APIs:** hidden columns skip data.
**Verified by:** `HiddenColumnCostBrowserlessTest`

---

### US-6.3: Details and selection

**Tasks:**
- [x] Row details with items, comments, allergen warnings and the last two history entries
- [ ] Details independent of selection, both directions asserted

**25.3 APIs:** row details decoupled from `activeItem`.
**Verified by:** `OrderBoardBrowserlessTest`

---

### US-6.4: Accessibility

**Tasks:**
- [x] `GridI18n` for select all, row select and sorters, translated in both bundles

**Verified by:** `GridI18nBrowserlessTest`

---

### US-6.5: Bulk actions

**Tasks:**
- [x] Multi select, bulk confirm and bulk cancel with confirmation
- [x] Result reports successes and names every refusal with its reason
- [x] Every change writes a history entry naming the actor

**Verified by:** `BulkActionsBrowserlessTest`

---

### US-6.6: Order detail

**Tasks:**
- [x] `/orders/{reference}` nested under the same `@RouteParent`, breadcrumb trail
- [x] State actions limited to what the role and the current state allow
- [x] History timeline, internal note, conversation placeholder for epic 09
- [ ] Optimistic lock conflict offers a reload instead of losing work

**Verified by:** `OrderDetailBrowserlessTest`, `ConcurrentEditBrowserlessTest`

---

## Left undone

- No relative date group headers. The board sorts by slot and does not group, and the class comment says as much.
- No card layout below the breakpoint, and nothing reads `Page.windowSizeSignal()` anywhere in the application. `ResponsiveBoardBrowserlessTest` is named and unwritten.
- Details independent of selection is asserted in one direction only, expanding not changing the selection. Selecting not opening the details is not.
- Optimistic lock conflicts are not handled: two sessions saving the same order is untested and unbuilt, which is what `ConcurrentEditBrowserlessTest` was for.

## Definition of Done

- [ ] Every acceptance criterion in `features/06-order-board.md` is checked
- [x] The hidden column claim is proven by a test, not by a screenshot
