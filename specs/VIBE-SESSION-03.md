# Vibe session 03: the second polish pass

A third conversational session, on 9 and 10 September 2026, working from a list somebody wrote by using the running application. The list is at the bottom of this file, exactly as it arrived. `specs/features/16-polish-02.md` is the contract it became.

## The setup

The same as the first two: the dev loop owning the application, a headed Chrome driven through Playwright in the same tab the user was looking at, and the specifications as the contract. The unit of work was a screen, as in session 01, rather than a finding, as in session 02.

One thing was different at the start. Three items on the list said **Needs a decision**, and the report's own instructions were to leave them and say so at the end. They were put to the product owner in the first minute instead, all three came back approved, and that changed the shape of the session: the counter order screen became a master detail, the kitchen board took a dragged ticket, and the invoice grew a letterhead that is a stored setting. Asking first cost one exchange and saved building two of the items the master detail then made unnecessary, which the report had predicted it would.

## What that produced

Twenty four acceptance criteria across seven screens, all built and all looked at in a browser. Fifteen test cases, each in the tier that can answer it. Six issue drafts with three reproducer projects.

The numbers that are worth something are the other ones.

**Two of the fixes were not what the report thought they were.** The report said the known customer picker "comes back empty because nothing fills that table". The table has 220 rows. What is empty is `CustomerService.search("")`, which answers a blank term with an empty list, correct for a search and wrong for an open dropdown. And the report said "Photo of a note does nothing in one browser and opens a file chooser in another", which reads as a browser difference and is not one: `vaadin-upload` disables its own add button when `maxFiles` is reached, so the browser that "worked" was the one that had not taken a picture yet. Both fixes are right; neither is the fix the row asked for.

**A test had been failing before any of this.** `OrderDetailsBandIT` opened the order panel by dispatching a double click on a reference cell. The board listened for one once and stopped long ago, so the test had been red on `-Pit` for some time and nothing said so, because the run before this session ended green in the notes rather than in a terminal. It was checked against a clean `HEAD` before being blamed on this pass, and repaired.

**One finding died while being reduced, and it was ours.** The row said a grid column has no minimum width. A column at `setWidth("14rem").setFlexGrow(2)` measures 527 pixels in a 1000 pixel box and stops at exactly 224 in a 360 one, while the table's scroll width goes to 544: the minimum is already there, it just has no name and no javadoc. The row is corrected and the issue asks for a sentence rather than a method. Session 02 lost eleven findings this way and the rate is holding.

## What the pass found in the platform

Six, each with a project somebody else can run:

- **`vaadin-upload` writes `disabled` onto an add button the application supplied**, so Flow and the component disagree about a control Flow owns and neither is told. This is the "does nothing in one browser" report, and it is a web components issue rather than a Flow one.
- **`Upload` has no Java setter for `capture`**, which the web component supports and which is the whole difference between a file browser and a camera on a phone.
- **A grid's header cells can only be joined on the top-most header row**, so filters under the titles and a filter spanning columns are mutually exclusive, and only the exception says so.
- **A grid column's width is already its minimum** and nothing says that either.
- **`theme="badge"` on a `Span` is a badge under Lumo and bare text under Aura.** It is how every Vaadin application since 14 draws a badge, the failure is silent, and the answer is the `Badge` component that 25.3 ships.
- **`ButtonVariant.LUMO_*` constants are inert under Aura**, with no deprecation and no javadoc to tell them apart from the theme neutral ones that work.

The last two are one shape: a Lumo style name that means nothing under Aura, failing without a sound. This application uses `theme="badge"` in eight places and this pass replaced one of them, the one the report named. The other seven are a row in `FEEDBACK-25.3.md` rather than a quiet sweep.

## What is still open

Nothing was posted. The six drafts are in `specs/issues/`, and `post-polish-02.sh` beside them is the script: it fills the reproducer links with the SHA of `HEAD` and refuses to send while a zip is missing from `HEAD` or `HEAD` is unpushed.

The kitchen drag is the one thing in this pass that no test performs. Selenium's mouse does not raise the HTML5 drag events, so the gesture joins the six already listed in `NEXT.md` that this tier cannot make. What is tested is the decision the drop makes, which is why that decision is a method and not a lambda inside the listener.

## The lesson, in one line

A defect report is a description of a symptom, and the cause is somebody else's guess: two of the twenty four were fixed correctly and for a different reason than the one written down, and the row that was most confidently worded, about a table with no rows in it, was about a table with 220.

## The list this session worked from

Written by somebody using the running application, for an agent arriving with no memory of the session that produced it. Kept here exactly as it arrived.

A list of UI defects and improvements found by using the running application. Written for an agent that arrives with no memory of the session that produced it.

### How to work through this

1. Implement everything that does not say **Needs a decision**. Those are questions for the product owner: leave them, and say at the end which ones you left.
2. Do not write or run tests while implementing. Tests come last, in one pass, once the screens are right.
3. Use the dev loop and keep a browser open through Playwright, so a change is on screen in under a second: `.vaadin/vaadin-dev start`, then `apply` after each edit. Read `.agents/skills/vaadin-devloop/SKILL.md` first. Open the page **before** the first `apply`, because a CSS push needs a page already connected.
4. Verify each item in the browser, at the width it is about. Several of these are about narrow screens, so resize rather than assume.
5. When the screens are right, update the specifications: the feature document that owns each screen, and `specs/FEEDBACK-25.3.md` if you had to work around the platform.
6. Then write the tests, in the tier `specs/08-testing.md` names for that kind of assertion.

The rules in `CLAUDE.md` hold throughout: no user visible string in Java, both translation bundles in step, CSS only under `src/main/resources/META-INF/resources`, `whenAttached` rather than `onAttach`, and views never touch repositories.

### Order board, `/orders`

Files: `ordering/ui/OrderBoardView.java`, `ordering/ui/OrderDetailsBand.java`, `ordering/ui/OrderLineEditor.java`, `styles/views/board.css`.

- [ ] **The toolbar breaks at phone width.** The top row does not fit and its controls overlap. Move "Show past orders" and the search filter onto their own row below when the board is narrow.
- [ ] **Aura draws a border around the edit button.** It should look like the other icon buttons on that row.
- [ ] **Aura loses the orange background on the editor's badges.** The state badges in the order editor are flat under Aura and coloured under Lumo. They should read the same in both.
- [ ] **The internal note touches the product block.** It needs the same vertical rhythm as the blocks around it.
- [ ] **Confirmed and Cancelled are nearly the same colour.** Two states that mean opposite things have to be told apart at a glance, in both themes and in both colour schemes.
- [ ] **Rename "Message" to "New message"** on the conversation panel, so the button says what it does rather than what it is about.
- [ ] **The known customer field finds nobody.** The combo box that offers customers the bakery already knows comes back empty because nothing fills that table. Feed it from the customers that appear on existing orders, which is where the names actually live.
- [ ] **Show the line comment on focus.** The comment field on an order line takes space on every row. Reveal it when the row has focus and keep it hidden otherwise. Do not lose a comment that is already written: a row that has one shows it always.

### Language menu, in the shell

File: `base/ui/MainLayout.java`.

- [ ] **Tick the language in use.** The language menu offers the languages and does not say which one is on. The theme menu next to it already does this with checkable items: do the same, one tick at a time.

### Counter order, `/orders/new`

Files: `assistant/ui/PhoneOrderView.java`, `styles/views/board.css`.

- [ ] **"Photo of a note" does nothing in one browser and opens a file chooser in another.** Find out why the upload button behaves differently and make it open the file chooser everywhere.
- [ ] **Offer the camera as well as the file chooser.** On a phone, taking the photo is the point. Two buttons, or one button that offers both, whichever reads better on a narrow screen.
- [ ] **A way to close the message list.** Once the form is filled, the conversation takes space that the barista no longer needs.
- [ ] **Needs a decision. Turn "What the customer said" into a mode chooser.** A radio group with write, record, photo of a note, and image, so the barista picks the input rather than meeting all of them at once.
- [ ] **Needs a decision. Move the cost line into a panel that can be hidden.** It is diagnostic information sitting in the middle of an order screen.
- [ ] **Needs a decision, and it is the preferred shape.** Make the assistant a master detail: the form is the page, and a button opens the assistant beside it with a close button of its own. That keeps the assistant from disturbing the form, and it subsumes the two items above. Confirm this before building it, because it replaces them.

### Kitchen board, `/kitchen`

Files: `ordering/ui/KitchenBoardView.java`, `styles/views/kitchen.css`.

- [ ] **Needs a decision. Drag and drop between columns.** Moving a ticket by dragging its card rather than by pressing a button. Confirm before building: it is the largest item on this list and it needs a keyboard equivalent to stay accessible.
- [ ] **"Claim this" should choose a person.** Claiming a ticket is a baker's action, and an administrator pressing it should be assigning it rather than taking it. Replace the button with a picker: the bakers, with the current user preselected when they are one, and free assignment when they are not.

### Products, `/admin/products`

Files: `catalogue/ui/ProductAdminView.java`, `catalogue/ui/ProductEditor.java`, `styles/views/admin.css`.

- [ ] **Edit and delete belong on the row, as two icons**, in the first column, the way the other admin screens do it.
- [ ] **The product column shrinks too far.** As the window narrows, that column takes the loss and the filter field under its header nearly disappears. Give it a floor.
- [ ] **Let the search field span several columns.** The columns beside it do not all need a filter of their own, so the search can occupy the width of the ones that do not.
- [ ] **Rename "Price in cents" to "Price"**, and format the value as money, `4.55 €`, rather than as an integer of cents. The storage stays in cents.
- [ ] **The photo is missing when the editor opens.** An existing product's image should be there before anything is changed.
- [ ] **Put price and VAT on one row** in the editor.
- [ ] **Put stock today, days notice, on sale and featured on one row.**
- [ ] **Put the markdown description and its rendered preview on one row**, and give that row the full width of the editor.
- [ ] **Move "New product" to the bottom right**, matching the People and Closures editors.

### Dashboard, `/admin/dashboard`

Files: `ordering/ui/DashboardView.java`, `styles/views/dashboard.css`.

- [ ] **The first widget should be a banner.** Different background from the widgets under it, and the full width of the page.
- [ ] **Make it more informative and better looking.** The reference is the old Bakery's dashboard at https://bakery-flow.demo.vaadin.com/dashboard, `admin@vaadin.com` / `admin`. Read it for what it shows and how it groups, not to copy its code: this application is a rewrite and the rule against copying the old Bakery still holds.

### Invoices, `/admin/invoices`

Files: `billing/ui/InvoiceListView.java`, `styles/views/billing.css`.

- [ ] **Print and "Mark as paid" should be icons** with tooltips and accessible names, like the order board's bulk actions.
- [ ] **Needs a decision. A global editor for the invoice header.** One place, markdown, where the bakery's own details are written and every invoice picks them up. Confirm the shape before building: it is a new stored setting, so it needs a home in the domain.

### Diagnostics, `/admin/diagnostics`

Files: `diagnostics/ui/DiagnosticsView.java`, `base/CommercialLicence.java`.

- [ ] **The subtitle says "No licence and no monitoring backend needed" and that is no longer the whole truth.** The application now asks whether a commercial key is present, because a component that validates its licence in its constructor blocks for five minutes without one. Say what is actually true: the build and the tests need no licence, and name whether a key was found on this machine, the way the panel already names why observability is off.
