# Feature 05: Checkout and order lifecycle

## Overview

Turning a cart into an order, and everything that happens to that order afterwards from the customer's side. This is the epic that carries the validation groups story and the routed breadcrumb hierarchy.

Covers V12, V13, C1, C4, C5.

## Behaviour

### A step view

Checkout is a step view, not one long form and not a wizard that throws away what it was told. Three routes under one parent, declared with `@RouteParent`, so the breadcrumb trail is router driven and nobody writes a trail by hand.

Every step carries Back and Continue. Continue validates the step it is leaving and moves on. **Back never validates anything**: leaving a step half filled is what a person does when they need to go and check something, and refusing to let them is how a form makes an enemy.

Nothing typed is ever lost. Going back to a step shows exactly what was in it, however many times the visitor moves in either direction, and a browser refresh reopens the step they were on with their values in place. That is a property of where the state lives, the session signal bean, rather than of each step remembering to save on the way out.

| Route | Step |
| --- | --- |
| `/checkout/contact` | Name, email, phone, optional note |
| `/checkout/slot` | Location, date, time, from feature 04 |
| `/checkout/review` | Read only summary and the place order action |
| `/checkout/done` | The confirmation, which is not a step and carries no breadcrumb |

Each step sets `@DynamicPageTitle`. Step state lives in the session signal bean, so going back never loses a field and a browser refresh mid checkout resumes where the visitor was.

The breadcrumb shows the route hierarchy: Bakery, Cart, the steps already passed, and the current one. Clicking a completed step returns to it. A step ahead of the current one never appears, because ROUTER mode builds the trail from the ancestors of the route the visitor is on, so there is nothing to make unclickable.

### Validation, per step and again at the end

This is the direct demonstration of the new `BeanValidationBinder` group support, and it is what makes the two paragraphs above possible.

- Every step validates with `OnDraft`, which allows an incomplete order and lets the visitor move back and forth without being scolded.
- Each step validates its own fields when it is left going forward. What it does not do is complain about a step nobody has reached yet.
- The review step validates the whole order with `OnSubmit`, which additionally requires contact details, a non empty cart, a date that respects lead time, and a slot with capacity. A form that only ever checks each step in isolation is how an order reaches the kitchen with a pickup time in the past.
- The place order button binds its enabled state to a computed signal over the per step validity signals, so it stays disabled until every step is submit valid.

### Placing the order

One service call. It deduplicates the customer by lowercased email, snapshots prices, books the slot, generates the reference and the tracking token, writes the first history entry, and returns the order.

Placing lands on `/checkout/done`, which shows the reference, the state, the slot, the total, and the tracking link, and offers to copy the link. The link is absolute, because the point of it is to be pasted somewhere that is not this browser.

The page is not a checkout step: it has no breadcrumb and no way back into a checkout that has already been cleared. What it reads is the one thing the session still holds after that clearing, the reference and the token of what was just placed, and a visitor who arrives with neither is sent to the basket. `UI.triggerAfter` expires the "order received" toast and reads the state again once after a few seconds, so somebody still on the page sees the bakery pick the order up without a push connection or a poll.

### Tracking, route `/track/{reference}`

- Anonymous, protected by the opaque token described in `04-security.md`.
- Shows state with a timeline built from the history, the items, the slot, and the conversation from feature 09.
- The state updates live through the shared signal of feature 08. No reload, no polling.
- Cancel is offered only while the order is `NEW`, and it asks for confirmation.
- Reorder puts the same lines back in the cart, dropping anything no longer available and saying which.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| The visitor refreshes mid checkout | The step reopens with its values intact |
| The visitor goes back and empties the cart | The review step blocks with "your basket is empty" |
| An email belongs to an existing customer | The order attaches to that customer, the name and phone are updated, nothing is disclosed to the visitor |
| The slot fills between the slot step and review | Review shows the conflict and returns the visitor to the slot step |
| The token is wrong or missing | The same not found page as an unknown reference |
| Cancel is attempted on a confirmed order | The action is not offered, and the direct call is refused by the service |
| Reorder contains a discontinued product | It is skipped and the cart page names it |

## Acceptance criteria

### AC1: It is a step view, and moving through it loses nothing
- [ ] Breadcrumbs reflect the route hierarchy with no hand written trail
- [x] Going back keeps the values already entered, however many times the visitor moves either way
- [x] Every step offers Back and Continue, and Back validates nothing
- [x] A refresh resumes the same step, with its values

### AC2: Validation happens per step and again at the end
- [x] An incomplete contact step can be left without an error
- [x] Leaving a step forward validates that step's own fields, and no other step's
- [x] The review step refuses to place an order that fails `OnSubmit`, whatever each step said on its own
- [x] The place order button is disabled until every step is submit valid

### AC3: Placing works
- [x] A placed order has a reference, a booked slot, a first history entry and correct totals
- [x] The confirmation shows the reference and a working tracking link

### AC4: Tracking is live and private
- [ ] A staff state change appears on the tracking page without a reload
- [x] A wrong token shows not found
- [x] Cancel appears only while the order is new

### AC5: Reorder
- [x] Reordering fills the cart with the same lines
- [ ] Unavailable products are skipped and named

The draft group is what makes the forward check possible. `OnDraft` was declared in epic 02 and never used: the shape rules, `@Email`, `@Pattern` and the sizes, now carry it alongside `Default`, while `@NotBlank` stays in `Default` alone. So a draft validation asks whether what is written is well formed without asking whether it is finished, and persistence keeps the guarantee it always had.

### Still open

- The breadcrumb trail is built from the route hierarchy and nothing tests it.
- Reorder proves every line that is still sold comes back. It does not cover the discontinued line being named, which is the half a customer would notice.
- CHK-08, a state change appearing on an open tracking page with no reload, waits on `TrackingLiveBrowserlessTest`. It is the last unproven claim about signals reaching a customer facing screen.

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| CHK-01 | A cart with two lines | Walking the three steps and placing | The order exists with the right totals and state new | browserless | `CheckoutBrowserlessTest` |
| CHK-02 | A half filled contact step | Navigating to the slot step and back | The values are still there | browserless | `CheckoutBrowserlessTest` |
| CHK-12 | Any step, half filled | Pressing Back | It moves, validating nothing, and the values are still there on return | browserless | `CheckoutBrowserlessTest` |
| CHK-13 | A just placed order | Landing on the confirmation | The reference is shown and the link it prints opens the order | browserless | `CheckoutBrowserlessTest` |
| CHK-03 | A contact step missing the phone | Reaching review | Place order is disabled and the phone is flagged | browserless | `CheckoutBrowserlessTest` |
| CHK-04 | The same form | Saving as a draft at the counter | No validation error is raised | browserless | `CheckoutBrowserlessTest` |
| CHK-05 | A checkout in progress | Refreshing the browser | The same step reopens with values | browserless | `CheckoutBrowserlessTest` |
| CHK-06 | An email that already exists | Placing an order | One customer row, two orders | browserless | `CheckoutBrowserlessTest` |
| CHK-07 | A placed order | Opening the tracking link | State, items and slot are shown | browserless | `TrackingBrowserlessTest` |
| CHK-08 | A tracking page open | A barista confirming the order | The page shows confirmed without a reload | browserless | `TrackingLiveBrowserlessTest` |
| CHK-09 | A tracking link with a wrong token | Opening it | Not found | browserless | `TrackingBrowserlessTest` |
| CHK-10 | A confirmed order | Attempting to cancel as the customer | Refused, and the action is not offered | browserless | `TrackingBrowserlessTest` |
| CHK-11 | A past order with a discontinued product | Reordering | The cart holds the rest and names the skipped one | browserless | `TrackingBrowserlessTest` |
