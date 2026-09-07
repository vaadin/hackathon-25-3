# Feature 06: Staff order board

## Overview

The screen the counter lives on. It replaces the old `StorefrontView`, and it is where an order is found, read, changed and taken. Everything a member of staff does to a single order happens without leaving the list, because losing the list is losing the queue.

Covers B1, B6, B9, A9, A10, K4.

## Behaviour

### The list, route `/orders`

- Orders, filtered to today and later by default, ordered by pickup date and time.
- Grouped by relative date, with the group heading staying put while its group scrolls: Recent, Yesterday, Today, Tomorrow, This week, Upcoming.
- A search field matches reference, customer name, phone and email. A toggle brings back the past.
- Selecting several orders allows confirming or cancelling them together. A bulk action asks first, then says how many it changed and names every order it refused, with the reason for each.
- The select all box, the per row boxes and every sorter carry a name a screen reader can read.
- On a narrow screen the table becomes a list of cards, because a table with seven columns on a phone is unreadable.

### Which columns are shown

- The columns are reference, customer, slot, state, items summary, channel and total.
- Reference, customer, slot and state are shown to begin with. Items summary, channel and total are not.
- A column menu lists **every** column, each showing whether it is currently on, and turning one on or off changes the table straight away. Nothing about a column is discoverable only by guessing: the menu is the full inventory.
- A column that is off costs nothing. The application never works out the contents of a cell nobody can see, and never asks the database for it. The items summary column is expensive on purpose so that this can be proved rather than claimed.

### Reading an order without leaving the list

Expanding a row opens a band underneath it. This is the quick answer, for the question "what is in this one", asked while keeping an eye on the rest of the queue.

- The band never sits flush against the left edge with the rest of the row empty. Its content is held in a comfortable reading column, centred across the width of the row.
- The items are the point, so they are the largest thing in the band: each one a tile carrying its quantity as the prominent figure, the product beneath it, and any comment on that line beneath that. Somebody glancing at the band should read the quantities before they read anything else.
- The tiles reflow according to **how wide the table is, not how wide the window is**. Narrowing the table, or opening the order panel beside it, changes how many tiles sit on a row even though the browser window never changed.
- Allergens follow as one line of chips, and the last two things that happened to the order follow as a short timeline.
- Expanding a row leaves the selection exactly as it was, and selecting a row does not expand it. A baker can read one order while a different one is selected for a bulk action.

### Opening an order

Opening an order reveals a panel beside the list, and over it when there is not enough room for both. The list stays on screen and keeps its place.

- The address carries the order's reference while the panel is open, so the order can be linked to and the link shared. Following such a link arrives at the list with that order already open, not at a bare page.
- Closing the panel returns to the plain list. Pressing Escape, clicking outside the panel, and the panel's own close control all do it.
- The panel carries the whole order: who it is for, the slot, the lines, the history, the internal note and the conversation with the customer from feature 09.
- The state actions are the ones this order can actually move to, for this role, right now. Not a list of choices that will be refused. Every action taken writes an entry in the history naming who did it.

### Changing what was ordered

The lines of an order are edited in that same panel, and the shape of the editor is the same whether the order already existed or is being taken now.

- One row per line: which product, how many, what that line costs, a comment, and a way to remove it.
- An empty row waits at the end at all times, so adding a product is a matter of typing into it rather than first asking for a row. Filling it produces a new empty one behind it.
- Until a row has a product, its quantity, comment and remove control do nothing, because they have nothing to act on.
- Any quantity the catalogue allows is allowed here. Setting a quantity to zero removes the line, because that is what everybody tries first.
- A total sits below the lines and follows them as they change, before anything is saved.
- Saving writes the order and the list behind the panel reflects it at once. Abandoning the panel with unsaved changes asks first.

### Taking a new order

The bakery takes orders three ways, and the application has to be able to record all three. Until now it could record only one: the customer's own checkout. The seeded data contains hundreds of counter and phone orders that no part of the application was able to produce.

- **At the counter.** The board offers to take a new order. It opens the same panel, empty, with the same line editor. The order is recorded as a counter order.
- **On the telephone.** The assistant screen keeps what it is for, pasting in what the customer said and having the lines worked out from it, and it now records the result. The order is recorded as a phone order.
- **Online.** Unchanged: the customer's own basket and checkout, recorded as an online order.
- The counter and the telephone use the same line editor. Somebody who has learned one has learned the other, which is the whole reason for not building two.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A bulk cancel includes an already picked up order | It is skipped and named in the result |
| Two staff members change the same order | The second save is refused and offers to reload rather than overwrite |
| An order is cancelled elsewhere while its panel is open | The panel shows the new state and the actions that no longer apply stop being offered |
| The search finds nothing | An empty state naming what was searched for, and a way to clear it |
| A baker opens the board | Reads everything, changes only the states their role allows |
| The panel is open and the table is now narrow | The tiles in an expanded row reflow to the narrower table, without the window having changed |
| A new order is saved with no lines | Refused, saying so, rather than writing an empty order |
| A product is removed from the catalogue while a line for it is being edited | The line says so and the order cannot be saved until it is resolved |
| Every column is turned off | Refused: the last remaining column cannot be turned off, so the table can never become unreadable |

## Acceptance criteria

### AC1: The list reads well
- [ ] Orders are grouped by relative date and the group heading stays visible while its group scrolls
- [ ] The default filter shows today and later, and the toggle reveals the past
- [ ] Search matches reference, name, phone and email

### AC2: Columns the reader controls, and hidden ones cost nothing
- [ ] The column menu lists every column, each showing whether it is on
- [ ] Turning a column on or off changes the table immediately
- [ ] Turning off the expensive column does not increase the number of database queries on the next fetch, and its contents are never worked out
- [ ] The last visible column cannot be turned off

### AC3: Details and selection are independent
- [ ] Expanding a row does not change the selection
- [ ] Selecting a row does not open its details

### AC4: The details band reads well
- [ ] The band's content is centred across the row rather than flush left
- [ ] Each item appears as a tile whose quantity is its most prominent element
- [ ] Narrowing the table reflows the tiles, with the browser window unchanged
- [ ] Allergens and the last two history entries are both present

### AC5: Opening an order keeps the list
- [ ] Opening an order leaves the list on screen and in place
- [ ] The address carries the reference, and following that address later arrives with the order open
- [ ] Escape, clicking outside, and the close control each return to the plain list

### AC6: Lines can be composed freely
- [ ] A product can be added in any quantity the catalogue allows
- [ ] An empty row is always available at the end without asking for one
- [ ] Setting a quantity to zero removes the line
- [ ] The total follows the lines before anything is saved
- [ ] Leaving with unsaved changes asks first

### AC7: All three channels can be recorded
- [ ] An order taken at the board is recorded as a counter order
- [ ] An order taken on the assistant screen is recorded as a phone order
- [ ] An order placed by a customer is recorded as an online order
- [ ] The counter and the telephone present the same line editor

### AC8: Accessibility
- [ ] The select all box, the row boxes and every sorter are named
- [ ] The column menu, the order panel and the line editor are reachable and operable from the keyboard

### AC9: Bulk actions are honest
- [ ] A bulk action reports how many succeeded and names every refusal with a reason
- [ ] Every state change appends a history entry naming the actor

### Still open

Nothing in this document is built yet.


## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| BOARD-01 | The seeded orders | Opening the board as a barista | Today's orders are visible and grouped | browserless | `OrderBoardBrowserlessTest` |
| BOARD-02 | The board | Searching for a customer's phone | Only their orders remain | browserless | `OrderBoardBrowserlessTest` |
| BOARD-03 | The expensive column visible, a query counter running | Turning the column off and fetching the next page | The query count does not grow and the cell contents are never worked out | browserless | `HiddenColumnCostBrowserlessTest` |
| BOARD-04 | A selected row | Expanding a different row | The selection is unchanged | browserless | `OrderBoardBrowserlessTest` |
| BOARD-05 | The grid | Reading the accessible names | Select all, row select and sorters are named | browserless | `GridI18nBrowserlessTest` |
| BOARD-06 | Three selected orders, one already picked up | Bulk cancelling | Two cancelled, one named as refused | browserless | `BulkActionsBrowserlessTest` |
| BOARD-07 | An order open in two sessions | Saving in both | The second is refused with a reload offer | browserless | `ConcurrentEditBrowserlessTest` |
| BOARD-08 | A narrow viewport | Opening the board | Cards render instead of a table | browserless | `ResponsiveBoardBrowserlessTest` |
| BOARD-09 | An order panel | Confirming the order | The history gains an entry naming the barista | browserless | `OrderDetailBrowserlessTest` |
| BOARD-10 | The column menu | Opening it | Every column is listed with its current state, and toggling one changes the table | browserless | `ColumnVisibilityBrowserlessTest` |
| BOARD-11 | A table showing one column | Turning that column off | Refused, the table keeps a column | browserless | `ColumnVisibilityBrowserlessTest` |
| BOARD-12 | An expanded row | Reading the band | One tile per line, each carrying its quantity, plus allergens and the last two history entries | browserless | `OrderBoardBrowserlessTest` |
| BOARD-13 | An expanded row in a wide table | Opening the order panel so the table narrows | The tiles reflow, with the browser window unchanged | testbench | `OrderDetailsBandIT` |
| BOARD-14 | The list | Opening an order | The list is still on screen and the address carries the reference | browserless | `OrderPanelBrowserlessTest` |
| BOARD-15 | An address naming an order | Following it directly | The list is shown with that order already open | browserless | `OrderPanelBrowserlessTest` |
| BOARD-16 | An open order panel | Pressing Escape | The panel closes and the address returns to the list | browserless | `OrderPanelBrowserlessTest` |
| BOARD-17 | An order being edited | Adding a product with a quantity of four | The line and the total both show it before saving | browserless | `OrderEditorBrowserlessTest` |
| BOARD-18 | An order line | Setting its quantity to zero | The line is removed and the total follows | browserless | `OrderEditorBrowserlessTest` |
| BOARD-19 | An edited order with unsaved changes | Closing the panel | It asks before discarding | browserless | `OrderEditorBrowserlessTest` |
| BOARD-20 | The board | Taking a new order and saving it | It is recorded as a counter order and appears in the list | browserless | `OrderCreationBrowserlessTest` |
| BOARD-21 | The assistant screen with lines worked out from pasted text | Saving | It is recorded as a phone order | browserless | `OrderCreationBrowserlessTest` |
| BOARD-22 | A new order with no lines | Saving | Refused, saying why, and nothing is written | browserless | `OrderCreationBrowserlessTest` |
| BOARD-23 | A baker | Trying to hand an order over or cancel one | Refused, and the order does not move | unit | `OrderServiceTest` |
| BOARD-24 | A barista and an order the kitchen left behind | Moving it on themselves | Allowed, so the counter can unstick it | unit | `OrderServiceTest` |

BOARD-13 is the only case here that a browser has to answer. Everything else is about what the application does, which is settled without one. The reflow is about what the layout does at a given table width, and no component API can be asked that question.
