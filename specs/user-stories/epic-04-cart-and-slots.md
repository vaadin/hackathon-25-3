# Epic 04: Cart and pickup slots

**Goal:** a cart that is always right, and a calendar that cannot promise what the bakery cannot deliver.
**Feature spec:** `specs/features/04-cart-and-slots.md`
**Dependencies:** epics 02 and 03.

---

### US-4.1: Cart state

**Tasks:**
- [ ] `@VaadinSessionScope CartSignals` with `ListSignal<CartLine>` and computed totals
- [ ] Header badge bound to the computed count
- [ ] Add to cart from card and from product page
- [ ] Cart survives navigation and a language switch

**Verified by:** `CartBrowserlessTest`

---

### US-4.2: Cart page

**Tasks:**
- [ ] Lines bound through `base/signals/Children.java`, quantity stepper, per line comment, remove
- [ ] Zero quantity removes the line
- [ ] Net, VAT and gross as computed signals
- [ ] Flag lines whose product became unavailable, and block checkout until removed
- [ ] Session expiry message with a link to the catalogue

**Verified by:** `CartBrowserlessTest`

---

### US-4.3: Slot service

**Tasks:**
- [ ] Generate slots from location, opening hours, slot length and closures
- [ ] `DaySlotLoad` for a date range, in one query
- [ ] Capacity check inside the ordering transaction

**Verified by:** `SlotServiceTest`, `SlotConcurrencyTest`

---

### US-4.4: Date picker constraints

**As a** visitor **I want** impossible days to be unselectable **so that** I do not promise myself a cake that cannot exist.

**Tasks:**
- [ ] `setDisabledWeekdays` from the location's closed weekdays
- [ ] Disable closure dates, dates before today plus the cart's largest lead time, and fully booked days
- [ ] `DateMetadataProvider` adding remaining capacity and a custom part name per day
- [ ] Tooltip explaining why a day is disabled, using the closure reason
- [ ] `refreshDateMetadata()` when the location or the cart changes

**25.3 APIs:** disabled dates and weekdays, date metadata provider, `refreshDateMetadata`.
**Verified by:** `SlotSelectionBrowserlessTest`, `DatePickerMetadataIT`

---

### US-4.5: Time selection

**Tasks:**
- [ ] Offer only slots that exist and are not full
- [ ] Prefill the next free time on the chosen date, using the DateTimePicker default time behaviour

**Verified by:** `SlotSelectionBrowserlessTest`

---

## Left undone

- The cart is never asserted to survive a language switch. It survives navigation, which is tested.
- There is no session expiry message on the cart page.

## Definition of Done

- [ ] Every acceptance criterion in `features/04-cart-and-slots.md` is checked
- [ ] No path exists that books an order into a closed or full slot
