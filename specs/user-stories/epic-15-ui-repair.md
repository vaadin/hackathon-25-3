# Epic 15: Repair and polish

**Goal:** fix the four things that are broken, and replace what was hand built with the component that already exists.
**Feature spec:** `specs/features/15-ui-repair.md`
**Dependencies:** everything. This is the pass a demo needs after the features are in.

The order below is the order to work in. The first story is worth doing on its own because none of it depends on anything else, and every one of its four items is a defect a visitor can hit.

---

### US-15.1: The four defects

**Tasks:**
- [ ] Apply the theme to every route, not to the layout most routes happen to use, so the login page is themed on a cold load
- [ ] Export as CSV hands the bytes to the browser
- [ ] The invoice print route says `autoLayout = false`
- [ ] Find out why a product page's header resolves to the not found text, and fix that rather than the symptom

**Verified by:** `ColdLoginThemeIT`, `InvoiceCsvExportIT`, `InvoicePrintBrowserlessTest`, `ProductHeaderBrowserlessTest`

---

### US-15.2: The shared polish

One commit, many views, no new dependency. Everything here is CSS or a formatter.

**Tasks:**
- [ ] Alternating rows and a distinguishable header for every plain table, in one stylesheet rule rather than per view
- [ ] No two row actions touching each other anywhere
- [ ] Short dates in tables: "Thu, 8 Sept"
- [ ] The navbar header on one line
- [ ] `Avatar` beside the signed in user's name, initials when there is no picture

**Verified by:** `UserAvatarBrowserlessTest`

---

### US-15.3: `Crud` for the three admin views

**Tasks:**
- [ ] `vaadin-crud-flow` as an ordinary dependency, no profile and no fallback
- [ ] Catalogue, people and closures on `Crud`, with its new item button, its editor and its delete confirmation
- [ ] Sortable columns where a person would sort
- [ ] The filters as a header row inside the grid

**Verified by:** `CatalogueCrudBrowserlessTest`

---

### US-15.4: The dashboard reads at a glance

**Tasks:**
- [x] `vaadin-dashboard-flow` as an ordinary dependency
- [x] The panels as widgets, charts spanning more than one column when there is room
- [x] Today and the work in progress before the trends
- [x] One column at phone width, nothing scrolling sideways

**Verified by:** `DashboardLayoutIT`

---

### US-15.5: The order board's toolbar and grid

**Tasks:**
- [ ] A selection column no wider than its checkbox
- [ ] A working select all, or no header text at all
- [ ] The column chooser inside the grid
- [ ] Confirm and cancel as icons with tooltips, and a toolbar that groups selection actions apart from the rest

**Verified by:** `BoardToolbarBrowserlessTest`

---

### US-15.6: The catalogue as a master detail

**Tasks:**
- [ ] A product opens over the list in an overlay rather than replacing the page
- [ ] Back returns to the list at the same scroll position

**Verified by:** `CatalogueOverlayBrowserlessTest`

---

### US-15.7: Diagnostics side by side

**Tasks:**
- [ ] Panels share a row when there is room, and a height when they share a row

**Verified by:** `DiagnosticsLayoutIT`
