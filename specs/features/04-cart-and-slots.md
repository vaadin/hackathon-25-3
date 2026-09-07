# Feature 04: Cart and pickup slots

## Overview

The cart, and the part that makes a bakery a bakery: you cannot pick up a cake on a day the shop is closed, on a day that is already full, or tomorrow when the cake needs two days.

Covers V5 to V11.

## Behaviour

### The cart

- Lives in a `@VaadinSessionScope` bean holding a `ListSignal<CartLine>` where `CartLine` is a record of product id, quantity and comment. Never persisted.
- The header badge binds to a computed signal of the total quantity. It updates the moment a card's add button is pressed, from any view.
- The cart page lists lines with a quantity stepper, a per line comment, a remove action, and the running net, VAT and gross totals, all computed signals.
- Setting a quantity to zero removes the line.
- The cart survives navigation and a language switch. It does not survive a session timeout, and the page says so if it happens.

### Location and date

- The visitor picks a pickup location. Changing it re invalidates the calendar, because closures and capacity are per location.
- The date picker disables:
  - weekdays the location is closed, with `setDisabledWeekdays`,
  - closure dates for that location,
  - any date earlier than today plus the largest lead time in the cart,
  - days whose every slot is full.
- Each rendered day carries metadata from a `DateMetadataProvider`: remaining capacity, and a custom part name so a nearly full day looks different from an empty one. Tooltip text explains why a day is disabled, using the closure reason when there is one.
- Changing the location or the cart contents calls `refreshDateMetadata()` rather than rebuilding the picker.

### Time

- The time picker offers the location's slots between opening and closing, at the location's slot length, minus the ones already full.
- The default time is prefilled with the next free slot on the chosen date. That is the 25.3 DateTimePicker default time behaviour, and it saves the most common click.

### Capacity

Capacity is advisory in the picker and authoritative in the service. Between choosing a slot and submitting, another customer may take the last place, so the submit path re checks and reports it clearly.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| The cart holds a two day lead time cake | Today and tomorrow are disabled, and the picker explains why |
| The cart is emptied while a date is chosen | The date stays, and the constraint relaxes on the next refresh |
| A day is fully booked | Disabled, with "fully booked" as the reason |
| The chosen slot fills up before submit | Submit is refused with the slot named, the calendar refreshes, and the cart is untouched |
| The location changes after a date was chosen | The date is kept if it is still valid, cleared with a message if it is not |
| A product becomes unavailable while it sits in the cart | The line is flagged on the cart page and blocks checkout until it is removed |
| The session expires | The cart page explains that the basket expired and offers the catalogue |

## Acceptance criteria

### AC1: The badge is always right
- [x] Adding from a card updates the badge without a navigation
- [ ] Changing a quantity on the cart page updates the badge and the totals
- [x] Setting a quantity to zero removes the line

### AC2: The calendar tells the truth
- [x] Closed weekdays for the chosen location are not selectable
- [x] Closure dates are not selectable and show their reason
- [x] Dates before the cart's earliest possible day are not selectable
- [ ] A fully booked day is not selectable and says so

### AC3: Metadata renders
- [x] Each selectable day shows its remaining capacity
- [x] A nearly full day is visually distinct from an empty one
- [ ] Changing the location refreshes the metadata without rebuilding the picker

### AC4: Time defaults are useful
- [x] Choosing a date prefills the next free time on that date
- [ ] Full times are not offered

### AC5: Capacity is enforced
- [x] Submitting into a slot that just filled is refused with the slot named and nothing is written

### Still open

- Changing a quantity to something other than zero is not asserted against the badge and the totals. Removal is, and adding is, and the case in between is not.
- A fully booked day being unselectable is untested at the day level. `fullSlotsAreNotOffered` only asserts that no option reports a negative remaining count, which is true of any list of options and proves nothing about a full one being hidden. That test needs rewriting around a slot deliberately filled first, and until then both that criterion and the full times one stay open.
- Changing the location refreshes the metadata without rebuilding the picker: implemented, never asserted.
- CART-06 and CART-07 are browser tier by design and wait on `DatePickerMetadataIT`. The capacity number and the part name are covered browserless, the hover reason is not.

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| CART-01 | An empty cart | Adding a product from a card | The badge shows one | browserless | `CartBrowserlessTest` |
| CART-02 | A cart with two lines | Opening the cart page | Both lines and correct totals are shown | browserless | `CartBrowserlessTest` |
| CART-03 | A cart with two croissants | Setting the quantity to zero | The line disappears and the badge updates | browserless | `CartBrowserlessTest` |
| CART-04 | A cart containing a two day lead time product | Opening the date picker | Today and tomorrow are disabled | browserless | `SlotSelectionBrowserlessTest` |
| CART-05 | A location closed on Sundays | Opening the date picker | Every Sunday is disabled | browserless | `SlotSelectionBrowserlessTest` |
| CART-06 | A holiday closure with a reason | Hovering that day | The reason is shown | testbench | `DatePickerMetadataIT` |
| CART-07 | A day with two places left | Opening the picker | That day shows a remaining capacity of two | testbench | `DatePickerMetadataIT` |
| CART-08 | A date chosen | Opening the time picker | The default is the next free slot | browserless | `SlotSelectionBrowserlessTest` |
| CART-09 | A slot with one place, taken concurrently | Submitting | Refused with the slot named, cart intact | browserless | `SlotConcurrencyTest` |
| CART-10 | A product made unavailable while in the cart | Opening the cart | The line is flagged and checkout is blocked | browserless | `CartBrowserlessTest` |
