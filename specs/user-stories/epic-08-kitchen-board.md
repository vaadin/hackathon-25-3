# Epic 08: Kitchen board and live collaboration

**Goal:** one board, many screens, no push plumbing written by hand.
**Feature spec:** `specs/features/08-kitchen-board.md`
**Dependencies:** epics 02, 05 and 06.

---

### US-8.1: Shared ticket state

**Tasks:**
- [x] Shared signal carrying the ticket list, typed with a Jackson `TypeReference`
- [x] Service updates the signal inside the same transaction that changes the state
- [x] Subscribe in `whenAttached`, release through the returned registration

**25.3 APIs:** shared signals with `TypeReference`, `Component.whenAttached`.
**Verified by:** `KitchenBoardMultiUserBrowserlessTest`, `KitchenBoardLifecycleBrowserlessTest`

---

### US-8.2: The board

**Tasks:**
- [ ] `/kitchen` with columns by state, today and tomorrow toggle
- [x] Ticket card with slot, name, items, comments, allergen warnings and owner
- [ ] Flash a ticket that changed on another screen
- [ ] Touch targets at least 44 pixels, confirm on destructive actions

**Verified by:** `KitchenBoardMultiUserBrowserlessTest`, `KitchenBoardPushIT`

---

### US-8.3: Claiming

**Tasks:**
- [x] Claim assigns to the current baker, visible everywhere
- [x] Stealing a claimed ticket asks first and names the owner
- [x] Concurrent claims resolve with one winner and no exception

**Verified by:** `TicketClaimBrowserlessTest`

---

### US-8.4: Summaries as semantic tables

**Tasks:**
- [x] Production summary with `Table`, `TableRow` and `TableCell`, rebuilt from the signal rather than with `bindChildren`, which does not exist
- [ ] Production summary per product per day, printable
- [x] Header cells and a caption, so a screen reader can read it

**25.3 APIs:** Table family with `bindChildren`.
**Verified by:** `KitchenSummaryBrowserlessTest`

---

### US-8.5: Time pressure

**Tasks:**
- [ ] Highlight tickets past their expected preparation time, scheduled with `UI.triggerAfter`
- [ ] Stale board marker from `UI.getLastUpdateSentTimestamp` after two minutes of silence

**Verified by:** `StaleTicketBrowserlessTest`

---

## Left undone

- No today and tomorrow toggle. The board shows what the kitchen has now.
- Nothing flashes a ticket somebody else moved, so a change arrives silently on a screen nobody is touching.
- Touch targets and destructive confirmations were never audited. Claiming asks before stealing, which is the one destructive path that does confirm.
- US-8.5 is not built at all: no highlight for tickets past their expected time, and no stale marker from `UI.getLastUpdateSentTimestamp`.

## Definition of Done

- [ ] Every acceptance criterion in `features/08-kitchen-board.md` is checked
- [x] Two browsers, one move, both screens agree
