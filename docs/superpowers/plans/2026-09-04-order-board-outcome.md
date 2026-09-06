# Order board: what shipped, what was deferred, what is still open

Outcome record for `docs/superpowers/plans/2026-09-04-order-board.md`. Branch range `9431ca3..43c9be9` on `manolo`, 18 commits, suite green at 214 tests.

Five tasks, each independently reviewed with its own fix rounds, then one whole-branch review and one fix wave. Everything below was found deliberately and left undone deliberately, with the reason recorded. Nothing here was discarded silently.

Everything here was found during per-task reviews and deliberately not fixed in that task's fix round. Nothing here was discarded silently: each line is a decision with a reason. The final review's job is to triage which of these must be fixed before merge and which can travel.

Plan scope: commits `e327b36..c6aea12`, base `9431ca3`.

## Deferred minors, by where they live

### ColumnChooser (Task 1)

- `ColumnChooser.java:53-55` — the springback that refuses to hide the last visible column re-enters its own value-change listener, which then calls `setVisible(true)` redundantly. Harmless and idempotent, but the control flow deserves a comment for the next reader.
- `ColumnChooser.java:42` — `of(...)` assumes it is the only submenu on the `MenuBar` it is given. True for its single caller; a javadoc note would prevent a surprise if anyone calls it twice.

### The details band (Task 2)

- Allergen chips use `flex-wrap`, so an order with many allergens wraps them onto a second line, against the spec's literal "one line of chips". Deferred deliberately: wrapping beats overflowing, and reading the spec as "one row of chips" is fair. If the final review disagrees, the spec sentence is what should change.

### The panel and its routing (Task 3)

- The literal route `"orders"` is repeated in `OrderBoardView.closePanel()`, `OrderDetailView.close()` and both `@Route` annotations. A shared constant removes the drift risk if the route is ever renamed.

### The line editor (Task 4)

- An order carrying a discontinued product is now unsaveable rather than silently truncated. This is strictly better than the silent data loss it replaced, and staff get the product name so they can remove the row, but nothing in the editor hints that the row is what blocks saving, and no test covers that save path. **This one has a product consequence worth a human's opinion**, not just a code opinion: an unrelated quantity edit on another line cannot be saved either.
- The union product list feeds every row's ComboBox in that editor, so an off-sale product already on the order can be chosen again in a second row, which `updateLines` then refuses. Scoped to one editor instance and to products already on that order.
- `setProduct` silently no-ops for an id outside the map. As a test seam it can only under-report, but a seam that quietly does nothing is a poor debugging surface.
- `@DirtiesContext(AFTER_CLASS)` on `OrderEditorBrowserlessTest` is unexplained; it forces a Spring context rebuild for the rest of the suite.
- A weakened assertion lost the brief's "a fresh editor has one row and zero lines" case, which is now covered nowhere.
- `board.editor.price` is a dead translation key in all three bundles.
- Only `DomainException` is caught in the panel's save handler; anything else surfaces as an internal error with no user feedback, which is precisely how the Hibernate defect hid during development.

### Taking orders (Task 5)

- The lead time computation is duplicated across `NewOrderView` and `PhoneOrderView`, in different packages so neither can call the other. Its natural home is `OrderLineEditor`, which already holds the `CatalogueService`.
- `@PageTitle` values are user-visible literals in annotations, against the stated constraint. Consistent with every sibling view in the codebase and already tracked as the `PageTitleLocaleBrowserlessTest` gap, so this is not a new regression.
- The UI refusal branch (empty lines produce a notification) is untested; only the service-level throw is covered.
- "New order" sits ahead of the search field in the toolbar. Unusual ordering for a primary create action.

## Coverage gaps carried into the branch

- **No automated test exercises the save path through the panel at all.** Neither the board refresh after save, nor `updateLines` reached through the panel's save button. Both were verified by hand only.
- `OrderEditorBrowserlessTest` is the one test in its class that writes to the shared catalogue. It restores the rows in a `finally` and is contained by `@DirtiesContext`, but it is a shared-state test in a suite whose spec forbids exactly that.
- **BOARD-13** (the row-details tiles reflow to the width of the table, not the window) has no automated test by design: it is layout, and this project writes no browser tests. I verified it by hand at a fixed 1800px window: panel closed gives a 1520px table and 5 tile columns, panel open gives 456px and 2 columns.
- **The real Escape key closing the panel is unverified by anyone.** The Java wiring is proven by a composed `KeyboardEvent` dispatched from page script reaching the detail element and closing the panel, and by tests that fire the framework events directly. Neither I nor the implementer could deliver native keyboard input to the page, so the hardware key path was never exercised. The other two ways of closing, the close control and clicking outside, are verified.

## Open questions that need a human, not a reviewer

These are product decisions. They were raised to the user during execution and are not code defects to be fixed by judgement.

1. **May staff edit the lines of an already invoiced order?** `OrderService.updateLines` has no order-state gate, so the lines of a `PICKED_UP` or `CANCELLED` order can be replaced and repriced, and `InvoiceService` is wired into the same view. If editing is allowed, what happens to the issued invoice: is it voided and reissued, or is the order locked once invoiced?

2. **Do views get to call repositories?** The architecture spec says views never touch repositories and services are the only transaction boundary. `NewOrderView` and `PhoneOrderView` take `PickupLocationRepository` directly, and so does the pre-existing `CheckoutSlotView`. Either the spec's absolute is wrong, or three views need a lookup service. Not fixed in the last task of the plan because correcting it properly means touching the customer checkout again, right after a review proved that flow untouched.

## Suite cost: RETRACTED, my measurement was wrong

**This entry was wrong and is withdrawn.** A clean run on the finished branch, with the dev loop daemon and the browser idle and the machine on mains power, does the whole suite in **32.2 seconds for 214 tests**, and the slowest class is 5.3 seconds. `OrderCreationBrowserlessTest` is not in the top six.

My earlier readings of 431 seconds for that one class and 13 minutes for the suite were taken while the dev loop daemon held a running application and an H2 database, a Playwright browser was live, and the machine was on battery and throttling. I measured contention, not cost, and reported it as cost.

What survives is only the countable fact: 16 test classes carry `@DirtiesContext`, eleven of them predating this plan and five added by it. Whether that is worth changing is now an open question with no evidence behind it, not a finding. `08-testing.md` still says each test should create and clean its own data, so the rule remains stricter than the practice, but nothing here demonstrates a cost.

Separately, and genuinely worth a look: the suite contains `DbgGrid`, `DebugThemeTest` and `DebugI18nTest`, which read like scratch classes left in the tree. They predate this branch.

## Original entry, kept so the correction has something to correct

`OrderCreationBrowserlessTest` reports 431 seconds. Its eight tests sum to 0.7 seconds: the rest is Spring context startup, which re-seeds roughly 1400 orders.

16 test classes force a context rebuild with `@DirtiesContext`. Eleven pre-date this plan. This plan added five: `ColumnVisibilityBrowserlessTest`, `OrderPanelBrowserlessTest`, `OrderEditorBrowserlessTest` and `OrderCreationBrowserlessTest` are new, and `PhoneOrderBrowserlessTest` gained the annotation.

The plan did not create this pattern and did make it measurably worse. This is a suite-wide question rather than a defect in any one task: do these classes need a rebuilt context, or can they clean up after themselves? `08-testing.md` already says each test should create and clean its own data and reserves `@DirtiesContext` for where a shared signal makes it necessary, so the codebase's own rule is stricter than its practice. Worth triaging here because no per-task review can see it: reviewers do not re-run suites.

## The same defect, one view further out

`CheckoutReviewView.java:94-96` still resolves the customer with `customers.findOrCreate` in its own committed transaction before calling the eight argument `place`. So an online checkout refused for `notOnThatDay`, `leadTimeViolated` or `slot.full` still leaves a created or renamed customer row behind, which is the defect Task 5 spent two rounds closing on the staff screens.

It is less damaging there: the customer is editing their own record, behind a binder validated contact step. And the fix is now a one line switch to the eleven argument overload that Task 5 created. It was out of scope for that task because touching the customer checkout at the end of a plan is how late regressions happen, but the branch should not pretend the defect is gone when it is closed on two views out of three.
