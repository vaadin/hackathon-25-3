# Epic 03: Public storefront and catalogue

**Goal:** the anonymous shopping surface, and the first impression of the demo.
**Feature spec:** `specs/features/03-storefront.md`
**Dependencies:** epics 01 and 02.

---

### US-3.1: Landing page

**As a** visitor **I want** a welcoming front page **so that** I know what this bakery sells today.

**Tasks:**
- [ ] Hero with name and today's opening hours, computed from the location and its closures
- [ ] Featured product row from a `ListSignal`, bound with `base/signals/Children.java` because `bindChildren` does not exist
- [ ] Category strip linking into the catalogue

**Verified by:** `LandingPageBrowserlessTest`

---

### US-3.2: Catalogue with filters

**As a** visitor **I want** to narrow the catalogue **so that** I find what I can eat.

**Tasks:**
- [ ] Responsive card grid over a `ListSignal<ProductCard>`, bound the same way
- [ ] Debounced search signal, category filter, allergen exclusion with `MultiSelectComboBox`
- [ ] Filters mirrored into query parameters and restored from them
- [ ] Sorting by relevance, price and name
- [ ] Empty state listing the active filters with a clear all

**25.3 APIs:** MultiSelectComboBox change event semantics, one refilter per user action.
**Verified by:** `CatalogueBrowserlessTest`, `CatalogueUrlStateBrowserlessTest`

---

### US-3.3: Product cards

**Tasks:**
- [ ] Photo with blurred placeholder, name, price, allergen chips, add to cart
- [ ] Lead time badge when the product needs advance notice
- [ ] Image resolution order: uploaded image, seeded file, category placeholder
- [ ] Compose around `Image`, which is no longer an `HtmlContainer`

**Verified by:** `CatalogueBrowserlessTest`

---

### US-3.4: Product page

**Tasks:**
- [ ] Route `/shop/product/{slug}` with `@DynamicPageTitle`
- [ ] Markdown description bound to a `Signal<String>`, shared with the admin preview
- [ ] Allergens with translated names, availability by weekday, lead time
- [ ] Related products, add to cart with a quantity stepper

**Verified by:** `ProductPageBrowserlessTest`

---

### US-3.5: Safe rendering

**Tasks:**
- [ ] One shared jsoup `Safelist` constant, applied to every piece of untrusted or admin authored HTML
- [ ] Rely on the 25.3 URL scheme validation, configure the safe scheme list once
- [ ] Tests for a script tag and a `javascript:` link

**Verified by:** `SanitizationTest`

---

### US-3.6: PWA

**Tasks:**
- [ ] `@PWA` with icons generated from one 512 pixel source
- [ ] Self contained offline page with hours, phone and a reconnect listener
- [ ] Installability step in the demo checklist

**Verified by:** `PwaInstallIT`

---

## Left undone

- `@PWA` names an offline page and icons were never generated from a 512 pixel source, so the manifest points at whatever Vaadin defaults to.
- Installability is in `DEMO.md` as a step nobody has walked since the theme selector arrived.
- The safe URL scheme list was never configured. The sanitizer strips `javascript:` links and that is tested; the platform level setting is untouched.

## Definition of Done

- [ ] Every acceptance criterion in `features/03-storefront.md` is checked
- [ ] The catalogue is usable on a phone with one hand
