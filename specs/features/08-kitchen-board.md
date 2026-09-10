# Feature 08: Kitchen board and live collaboration

## Overview

The board on the wall of the kitchen, shared by every baker in real time. This is where shared signals earn their place: one baker moves a ticket and it moves on every other screen, including the customer's tracking page, with no push plumbing written by hand.

Covers K1 to K7 and C1.

## Behaviour

### Route `/kitchen`

The shape is part of what it does, because this screen lives on a wall and nobody walks over to scroll it.

- One column per state, filling the height of the screen. Each column scrolls **inside itself**, so the headings stay where the baker last looked for them and a busy Saturday does not push the third column off the bottom. The page itself never scrolls.
- Each column header carries its count, so a full column reads as full before anybody counts cards.
- The day's production summary is a **detail panel beside the board**, opened from a button in the header, not a section below the board. Anything below the fold on a wall display may as well not exist.
- Opening the summary costs the board nothing. The columns keep the width they had, the cards do not reflow or clip, and closing it again leaves the board exactly as it was found. A baker reading the summary is answering a second question, not giving up the first.
- The panel is as wide as the summary needs and no wider, so it covers as little of the board as it can. Its width follows its contents rather than a figure chosen up front, because the product names decide how wide the table has to be.
- Columns by state: confirmed, in preparation, ready. Today by default, with a toggle for tomorrow.
- Each ticket shows the slot time, the customer's first name, the items with quantities and comments, allergen warnings, and who claimed it.
- The board is backed by a shared signal carrying the ticket list, typed with a Jackson `TypeReference` so a list of records round trips properly.
- Subscription happens in `whenAttached` and is released through the returned registration. No `onAttach` override anywhere.
- Moving a ticket is a service call. The service writes the state and updates the shared signal, so every session sees it. A moved ticket flashes briefly on the other screens so a baker notices.
- Claim assigns the ticket to the current baker. Claiming an already claimed ticket asks before stealing it.
- A ready ticket stays on the wall until somebody hands it over, and the wall offers a baker no way to do that. Handing an order over is where its invoice is issued, so it belongs to the counter, and the roles that may do it are in `04-security.md`. A baker who has finished baking leaves the ticket in the ready column and the counter closes it.
- The per slot summary is rendered with the new `Table` family, `bindChildren` over the slot rows, so the markup is a real table for a screen reader and a wall display.
- Production summary: units per product for the chosen day, also a `Table`, printable.
- A ticket that has been in preparation longer than its expected time is highlighted. `UI.triggerAfter` schedules the check without any push connection.
- After two minutes of silence the board shows a discreet "may be stale" marker, driven by `UI.getLastUpdateSentTimestamp`.

### Touch

The board is used on a tablet with flour on it. Targets are at least 44 pixels, the primary action on a ticket is a single tap, and destructive actions need a confirm.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| Two bakers claim the same ticket at once | One wins, the other is told who has it, no exception |
| A ticket is cancelled from the order board | It disappears from the kitchen board on every screen |
| A session was open overnight | The stale marker appears and a refresh action reloads from the service |
| The shared signal is empty | The column shows a friendly empty state, not a blank box |
| The summary is opened on a narrow screen | It still does not squeeze the columns. It covers part of the board and the board is unchanged underneath |
| A product name is long enough to need more width | The panel is wide enough to show the row without wrapping it, and still no wider than it needs |
| A baker's view is detached mid update | The registration is released and no update is delivered to a dead UI |
| A ticket is ready and nobody is at the counter | It waits in the ready column. The wall shows it, the baker cannot close it, and it leaves the board when the counter hands it over |

## Acceptance criteria

### AC1: The board is live
- [x] A state change made by one baker appears on another baker's board with no reload
- [x] The same change appears on the customer's tracking page
- [x] The changed ticket is visually flagged for a moment on the observing screens

### AC2: Claiming is safe
- [x] A ticket claimed by one baker shows their name everywhere
- [x] A second claim asks for confirmation and names the current owner

### AC3: The summaries are semantic tables
- [x] The slot summary and the production summary render real table markup with headers
- [ ] Both update when the underlying signal changes

### AC4: Lifecycle is clean
- [ ] Leaving the board releases the shared signal subscription
- [ ] No update is delivered to a detached UI

### AC5: Staleness is visible
- [x] After the configured silence, the board shows the stale marker
- [x] Refreshing clears it

### AC6: The summary does not cost the board
- [x] Opening the production summary leaves the columns at exactly the width they had, with no card reflowed or clipped
- [x] The panel is no wider than the summary it contains
- [x] Closing the summary returns the board to what it was before it was opened

### Still open

- The board looked as though a reload duplicated it, and it does not. The duplication, and a ticket that moved back after being moved, were both the browserless signal environment: a class that writes to a shared signal and inherits a context whose environment has been replaced has every write dropped in silence. Three attempts to fix the board were reverted, because the board was never wrong. Every class that writes to it now takes a context of its own before it runs, and the finding is in `FEEDBACK-25.3.md`.

- The customer's tracking page following a kitchen change waits on `TrackingLiveBrowserlessTest`. Between bakers it is proven; the last hop to the customer is not.
- The flag on a ticket somebody else moved is a class on a freshly built card and a CSS animation that ends it, so nothing is scheduled to take it off again: the next redraw builds the card without one. A screen does not flash its own work, because a baker who pressed the button knows what they pressed.
- Nothing asserts that the summaries update when the signal changes. The columns are covered, the summary tables are not.
- AC4 has no test at all. `AttachLifecycleTest` proves the codebase uses `whenAttached` rather than overriding `onAttach`, which is a rule about how subscriptions are written, not evidence that this board releases its own or that nothing is delivered to a detached UI.
- Staleness is measured against the wall clock and not against the application's `Clock` bean, which is frozen in tests and shifted for the demo dataset. The threshold is `bakery.kitchen.stale-after`, two minutes by default, and the check re-arms itself through `UI.triggerAfter`. What the browserless test covers is the arithmetic and the marker; the scheduling needs a live UI and belongs to the browser tier.
- The highlight for a ticket that has been in preparation longer than expected, KIT-06, is still not built, and it is not an acceptance criterion here. It needs a notion of expected preparation time that the domain does not have: no product carries one, and inventing a constant would be a rule nobody agreed.
- The two width criteria in AC6 are measured by `KitchenSummaryOverlayIT`, and how they are measured took several attempts: the shell opens its drawer a moment after the first render, which moves every column and has nothing to do with the summary. The test compares the share of the board its columns occupy, read in one call so both numbers describe the same frame.

## Changed by the second polish pass

A ticket can be dragged from one column to the next, obeying exactly the rules the buttons obey and writing to the same signal; the buttons stay, because dragging is a mouse gesture and this board has to be usable from a keyboard. "Claim this" is a picker of bakers rather than a button that means "me": the current user comes first when they are a baker, and an administrator assigns freely. See `16-polish-02.md`.

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| KIT-01 | Two bakers with the board open | One moves a ticket to ready | The other sees it without a reload | browserless | `KitchenBoardMultiUserBrowserlessTest` |
| KIT-02 | A customer tracking page and a baker board | The baker moves the ticket | The tracking page updates | browserless | `KitchenBoardMultiUserBrowserlessTest` |
| KIT-03 | An unclaimed ticket | Two bakers claiming at once | One owns it, the other is informed | browserless | `KitchenBoardMultiUserBrowserlessTest` |
| KIT-04 | The board | Reading the slot summary markup | It is a table with header cells | browserless | `KitchenBoardMultiUserBrowserlessTest` |
| KIT-05 | A board view | Navigating away | The shared signal subscription count returns to its previous value | browserless | `KitchenBoardMultiUserBrowserlessTest` |
| KIT-06 | A ticket in preparation past its expected time | Waiting for the check | It is highlighted | browserless | `StaleTicketBrowserlessTest` |
| KIT-09 | A board that has heard nothing for longer than the threshold | Checking | The stale marker shows, and reloading clears it | browserless | `StaleTicketBrowserlessTest` |
| KIT-10 | A board open on one screen | Somebody else moving a ticket | That ticket's card is flagged here, and a ticket moved here is not | browserless | `StaleTicketBrowserlessTest` |
| KIT-07 | Two real browsers | Moving a ticket in one | The other updates | testbench | `KitchenBoardPushIT` |
| KIT-08 | A board with its three columns measured | Opening the production summary | The columns measure the same as before, and the panel is no wider than its own table | testbench | `KitchenSummaryOverlayIT` |
