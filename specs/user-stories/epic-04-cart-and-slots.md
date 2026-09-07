# Epic 04: Cart and pickup slots

**Goal:** a cart that is always right, and a calendar that cannot promise what the bakery cannot deliver.
**Feature spec:** `specs/features/04-cart-and-slots.md`
**Dependencies:** epics 02 and 03.

---

### US-4.1: Cart state

**Tasks:**
- [x] `@VaadinSessionScope CartSignals` with `ListSignal<CartLine>` and computed totals
- [x] Header badge bound to the computed count
- [x] Add to cart from card and from product page
- [ ] Cart survives navigation and a language switch

**Verified by:** `CartBrowserlessTest`

---

### US-4.2: Cart page

**Tasks:**
- [x] Lines bound through `base/signals/Children.java`, quantity stepper, per line comment, remove
- [x] Zero quantity removes the line
- [x] Net, VAT and gross as computed signals
- [x] Flag lines whose product became unavailable, and block checkout until removed
- [ ] Session expiry message with a link to the catalogue

**Verified by:** `CartBrowserlessTest`

---

### US-4.3: Slot service

**Tasks:**
- [x] Generate slots from location, opening hours, slot length and closures
- [x] `DaySlotLoad` for a date range, in one query
- [x] Capacity check inside the ordering transaction

**Verified by:** `SlotServiceTest`, `SlotConcurrencyTest`

---

### US-4.4: Date picker constraints

**As a** visitor **I want** impossible days to be unselectable **so that** I do not promise myself a cake that cannot exist.

**Tasks:**
- [x] `setDisabledWeekdays` from the location's closed weekdays
- [x] Disable closure dates, dates before today plus the cart's largest lead time, and fully booked days
- [x] `DateMetadataProvider` adding remaining capacity and a custom part name per day
- [x] Tooltip explaining why a day is disabled, using the closure reason
- [x] `refreshDateMetadata()` when the location or the cart changes

**25.3 APIs:** disabled dates and weekdays, date metadata provider, `refreshDateMetadata`.
**Verified by:** `SlotSelectionBrowserlessTest`, `DatePickerMetadataIT`

---

### US-4.5: Time selection

**Tasks:**
- [x] Offer only slots that exist and are not full
- [x] Prefill the next free time on the chosen date, using the DateTimePicker default time behaviour

**Verified by:** `SlotSelectionBrowserlessTest`

---

## Left undone

- The cart is never asserted to survive a language switch. It survives navigation, which is tested.
- There is no session expiry message on the cart page.

## Definition of Done

- [ ] Every acceptance criterion in `features/04-cart-and-slots.md` is checked
- [x] No path exists that books an order into a closed or full slot
