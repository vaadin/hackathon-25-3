# Polish pass 01

A list of UI defects and improvements found by using the running application. Written for an agent that arrives with no memory of the session that produced it.

## How to work through this

1. Implement everything that does not say **Needs a decision**. Those are questions for the product owner: leave them, and say at the end which ones you left.
2. Do not write or run tests while implementing. Tests come last, in one pass, once the screens are right.
3. Use the dev loop and keep a browser open through Playwright, so a change is on screen in under a second: `.vaadin/vaadin-dev start`, then `apply` after each edit. Read `.agents/skills/vaadin-devloop/SKILL.md` first. Open the page **before** the first `apply`, because a CSS push needs a page already connected.
4. Verify each item in the browser, at the width it is about. Several of these are about narrow screens, so resize rather than assume.
5. When the screens are right, update the specifications: the feature document that owns each screen, and `specs/FEEDBACK-25.3.md` if you had to work around the platform.
6. Then write the tests, in the tier `specs/08-testing.md` names for that kind of assertion.

The rules in `CLAUDE.md` hold throughout: no user visible string in Java, both translation bundles in step, CSS only under `src/main/resources/META-INF/resources`, `whenAttached` rather than `onAttach`, and views never touch repositories.

## Order board, `/orders`

Files: `ordering/ui/OrderBoardView.java`, `ordering/ui/OrderDetailsBand.java`, `ordering/ui/OrderLineEditor.java`, `styles/views/board.css`.

- [ ] **The toolbar breaks at phone width.** The top row does not fit and its controls overlap. Move "Show past orders" and the search filter onto their own row below when the board is narrow.
- [ ] **Aura draws a border around the edit button.** It should look like the other icon buttons on that row.
- [ ] **Aura loses the orange background on the editor's badges.** The state badges in the order editor are flat under Aura and coloured under Lumo. They should read the same in both.
- [ ] **The internal note touches the product block.** It needs the same vertical rhythm as the blocks around it.
- [ ] **Confirmed and Cancelled are nearly the same colour.** Two states that mean opposite things have to be told apart at a glance, in both themes and in both colour schemes.
- [ ] **Rename "Message" to "New message"** on the conversation panel, so the button says what it does rather than what it is about.
- [ ] **The known customer field finds nobody.** The combo box that offers customers the bakery already knows comes back empty because nothing fills that table. Feed it from the customers that appear on existing orders, which is where the names actually live.
- [ ] **Show the line comment on focus.** The comment field on an order line takes space on every row. Reveal it when the row has focus and keep it hidden otherwise. Do not lose a comment that is already written: a row that has one shows it always.

## Language menu, in the shell

File: `base/ui/MainLayout.java`.

- [ ] **Tick the language in use.** The language menu offers the languages and does not say which one is on. The theme menu next to it already does this with checkable items: do the same, one tick at a time.

## Counter order, `/orders/new`

Files: `assistant/ui/PhoneOrderView.java`, `styles/views/board.css`.

- [ ] **"Photo of a note" does nothing in one browser and opens a file chooser in another.** Find out why the upload button behaves differently and make it open the file chooser everywhere.
- [ ] **Offer the camera as well as the file chooser.** On a phone, taking the photo is the point. Two buttons, or one button that offers both, whichever reads better on a narrow screen.
- [ ] **A way to close the message list.** Once the form is filled, the conversation takes space that the barista no longer needs.
- [ ] **Needs a decision. Turn "What the customer said" into a mode chooser.** A radio group with write, record, photo of a note, and image, so the barista picks the input rather than meeting all of them at once.
- [ ] **Needs a decision. Move the cost line into a panel that can be hidden.** It is diagnostic information sitting in the middle of an order screen.
- [ ] **Needs a decision, and it is the preferred shape.** Make the assistant a master detail: the form is the page, and a button opens the assistant beside it with a close button of its own. That keeps the assistant from disturbing the form, and it subsumes the two items above. Confirm this before building it, because it replaces them.

## Kitchen board, `/kitchen`

Files: `ordering/ui/KitchenBoardView.java`, `styles/views/kitchen.css`.

- [ ] **Needs a decision. Drag and drop between columns.** Moving a ticket by dragging its card rather than by pressing a button. Confirm before building: it is the largest item on this list and it needs a keyboard equivalent to stay accessible.
- [ ] **"Claim this" should choose a person.** Claiming a ticket is a baker's action, and an administrator pressing it should be assigning it rather than taking it. Replace the button with a picker: the bakers, with the current user preselected when they are one, and free assignment when they are not.

## Products, `/admin/products`

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

## Dashboard, `/admin/dashboard`

Files: `ordering/ui/DashboardView.java`, `styles/views/dashboard.css`.

- [ ] **The first widget should be a banner.** Different background from the widgets under it, and the full width of the page.
- [ ] **Make it more informative and better looking.** The reference is the old Bakery's dashboard at https://bakery-flow.demo.vaadin.com/dashboard, `admin@vaadin.com` / `admin`. Read it for what it shows and how it groups, not to copy its code: this application is a rewrite and the rule against copying the old Bakery still holds.

## Invoices, `/admin/invoices`

Files: `billing/ui/InvoiceListView.java`, `styles/views/billing.css`.

- [ ] **Print and "Mark as paid" should be icons** with tooltips and accessible names, like the order board's bulk actions.
- [ ] **Needs a decision. A global editor for the invoice header.** One place, markdown, where the bakery's own details are written and every invoice picks them up. Confirm the shape before building: it is a new stored setting, so it needs a home in the domain.

## Diagnostics, `/admin/diagnostics`

Files: `diagnostics/ui/DiagnosticsView.java`, `base/CommercialLicence.java`.

- [ ] **The subtitle says "No licence and no monitoring backend needed" and that is no longer the whole truth.** The application now asks whether a commercial key is present, because a component that validates its licence in its constructor blocks for five minutes without one. Say what is actually true: the build and the tests need no licence, and name whether a key was found on this machine, the way the panel already names why observability is off.
