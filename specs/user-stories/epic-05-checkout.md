# Epic 05: Checkout and order lifecycle

**Goal:** cart to order, and the customer's life with that order afterwards.
**Feature spec:** `specs/features/05-checkout.md`
**Dependencies:** epic 04.

---

### US-5.1: Routed checkout with breadcrumbs

**Tasks:**
- [x] Parent route with `@RouteParent`, three child routes, each with `@DynamicPageTitle`
- [x] `Breadcrumbs` in router driven mode, no hand written trail
- [x] Step state in the session signal bean, surviving back, forward and refresh
- [x] Steps ahead of the current one are not clickable, which ROUTER mode gives for nothing: the trail is the ancestors of the current route, so a step not yet reached is not in it

**25.3 APIs:** Breadcrumbs, `@RouteParent`, `@DynamicPageTitle`.
**Verified by:** `CheckoutBrowserlessTest`

---

### US-5.2: Validation groups

**As a** barista **I want** to save an incomplete order **so that** I can take a phone call now and finish it later.

**Tasks:**
- [x] `BeanValidationBinder` with `setValidationGroups(Default.class)` per step
- [x] Review validates with `Default` plus `OnSubmit`
- [x] Place order enabled from a computed signal over the per step validity signals
- [x] Draft save path at the counter validates with `OnDraft` only

**Verified by:** `ValidationGroupsBrowserlessTest`

---

### US-5.3: Placing the order

**Tasks:**
- [x] One service call: deduplicate customer, snapshot prices, book slot, generate reference and token, first history entry
- [ ] Confirmation page with reference, slot, total, tracking link and a copy action
- [ ] `UI.triggerAfter` for the toast expiry and one deferred state refresh, with no push connection

**Verified by:** `CheckoutBrowserlessTest`

---

### US-5.4: Tracking page

**Tasks:**
- [x] `/track/{reference}` with the opaque token, identical not found for wrong reference and wrong token
- [x] Timeline from history, items, slot
- [ ] Live state through the shared signal of epic 08, degrading to a manual refresh until that epic lands
- [x] Cancel offered only while new, with confirmation
- [ ] Rate limit per IP

**Verified by:** `TrackingBrowserlessTest`, `TrackingLiveBrowserlessTest`

---

### US-5.5: Reorder

**Tasks:**
- [x] Refill the cart from a past order
- [x] Skip unavailable products and name them on the cart page

**Verified by:** `ReorderBrowserlessTest`

---

## Left undone

- There is no confirmation page. Placing an order navigates away and the reference reaches the customer through the tracking link, which works, rather than through a page that shows it with a copy action.
- `UI.triggerAfter` is used in the diagnostics view and never in checkout, so the deferred refresh described here does not exist.
- The tracking page still refreshes on navigation rather than following the shared signal. Epic 08 landed, and this was not revisited.
- No rate limiting per IP.

## Definition of Done

- [ ] Every acceptance criterion in `features/05-checkout.md` is checked
- [ ] A visitor can go from catalogue to a tracked order without ever seeing a stack trace
