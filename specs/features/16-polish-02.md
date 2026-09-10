# Feature 16: Polish pass 02

## Overview

The second pass of using the running application and writing down what is wrong with it. The list came from `specs/PolishUI-01.md`, which is the report; this document is the contract that report became.

Nothing here adds a capability the application did not have. Three items are close to one: the counter order screen becomes a master detail, the kitchen board accepts a dragged ticket, and the invoice grows a letterhead that somebody writes once. Those three were marked in the report as questions for the product owner and were approved before they were built, which is why they are here rather than in a **Left for the product owner** section.

The report also asked for a mode chooser on the counter screen and for the assistant's cost line to move into something that can be hidden. Both are answered by the master detail and neither was built separately: closing the panel takes the conversation, the photograph and the cost line off the screen in one gesture, and the input controls are together inside it.

## What was wrong

Every item was reproduced in the browser before it was changed, and every one was looked at again afterwards.

| Screen | Defect | Cause |
| --- | --- | --- |
| Order board | The toolbar's six controls do not fit a phone and overlap | One flex row with a search field that has an 18rem floor |
| Order board | Aura draws a box around the row's edit pencil, Lumo draws a bare icon | `LUMO_TERTIARY_INLINE`, which is a variant name Aura does not know |
| Order board | The allergen chips in the details band are coloured under Lumo and bare words under Aura | `theme="badge"` on a `Span` is a Lumo convention. Aura has no rule for it |
| Order board | The internal note sits directly on the first product row | No vertical margin on the note |
| Order board | Confirmed and Cancelled are two warm greys three points apart | `#eceae4` against `#f0eeec`, and nothing but colour to tell them apart |
| Order board | The comment field costs a line of every order line | Always rendered, whether or not anybody has written in it |
| Conversation | The message box is called "Message" and is English in a Spanish page | `MessageInput`'s own default i18n, never replaced |
| Counter order | The known customer picker offers nobody until something is typed | `CustomerService.search` answers a blank term with an empty list, which is right for a search and wrong for an open dropdown |
| Counter order | "Photo of a note" opens a file chooser once and then does nothing | `vaadin-upload` counts its file list against `maxFiles` and disables its own add button when the limit is reached. With a limit of one, the control latches after the first picture, and whether it still looks enabled depends on who wrote `disabled` last |
| Products | Edit and Delete are two words at the end of every row | A component column of text buttons |
| Products | The name column shrinks to an ellipsis and takes the search field with it | No width on the column, so nothing stops it giving way |
| Products | "Price in cents" shows 290 for a product that costs €2.90 | The column is the raw `priceCents` |
| Products | The editor opens with an empty photo frame for a product that has one | The `Image` only ever had a source after an upload |
| Products | The editor is one field per line | A `FormLayout` at its default responsive steps |
| Diagnostics | The subtitle claims no licence is needed, and the application now asks for one | Written before `CommercialLicence` existed |

## What it should do now

### Order board

- At the board's own width, not the window's, a narrow board puts the four buttons on one row and the search field with its past orders checkbox on the row below.
- The row's edit control is a bare icon under both themes.
- The allergen chips are the platform's `Badge`, which both themes style.
- The internal note has the same air above and below it as the blocks around it.
- Confirmed and Cancelled are told apart by hue and, on the board's chips, by a line through the cancelled word: a second cue that survives any theme, either colour scheme and a reader who cannot separate two greys.
- A line's comment appears when the row has focus and stays hidden otherwise. A line that already carries a comment shows it always, because a note that only appears when you stand on it is a note nobody reads.

### Conversation

- The message box says "New message", in the reader's language, and so does its Send button.

### Counter order

The form is the page. The assistant is a panel beside it with a close button of its own, and a button on the form's header opens it again. It arrives open, because talking to the assistant is what this screen is for; a barista who wants the form on its own closes it and it stays closed for that order.

- Everything the assistant is and does lives in the panel: what the customer said, the controls that act on it, the conversation and the cost line.
- With no model, there is no panel and no button: the red sentence saying why is on the page, where it always was.
- The picture can arrive from a file chooser or from the camera. The camera control passes `capture` through to the file input, so a browser with no camera opens the chooser and it is never a dead button.
- Neither control latches. What is attached is said in words beside them, with a way to change your mind, and the upload's own file list is emptied as soon as the bytes are ours.
- The customer picker offers the people the bakery has served, most recent first, before anybody types.

### Kitchen board

- A ticket can be dragged from one column to the next. The drop obeys exactly the rules the buttons obey, refuses the same moves and says so, and writes to the same signal.
- The buttons stay. Dragging is a mouse gesture and a kitchen board has to be usable from a keyboard, so the buttons are the path and the drag is the shortcut.
- "Claim this" is a picker of bakers rather than a button that means "me". The current user comes first when they are a baker, and is ticked when the ticket is already theirs. Anybody else assigns freely from the same list.

### Products

- Edit and delete are two icons in the first column, each with its name in its accessible name and its tooltip.
- The product column has a floor and grows into whatever is spare.
- The search spans the name, price and stock columns, because only the category has a filter of its own.
- The price column is money, formatted for the reader, and it is edited in the same words: a currency symbol and either decimal separator are all accepted. Storage is unchanged and is still integer cents.
- The editor opens showing the product's own photo, puts price beside VAT, puts the four small facts of a day's baking on one line, and gives the description and its preview the full width of the dialog.
- "New product" is under the table at the end of the row, where People and Closures put theirs.

### Dashboard

- The first widget is a banner: the width of the page, a different surface from the widgets under it, and the day's figures large enough to read from the doorway. Its title sits inside it, beside the date.
- Today's takings are on it, which no panel showed before.
- The range has a totals panel of its own: how many orders, what they came to, and what the average one is worth. The charts show the shape; nothing on the page said the size.
- A panel says when the counter is busy, as orders per hour of the day over the range. Everything else on the page is measured in days.

### Invoices

- Print and Mark as paid are icons with a tooltip and an accessible name each, the way the order board's bulk actions carry theirs.
- The bakery's own details are written once, in markdown, from a button on the invoice list, and print at the top of every invoice. An administrator writes them; a barista does not. With none written, the document prints the bakery's name, which is what it always printed.

### Diagnostics

- The subtitle says what is true: the build and the tests need no licence and no monitoring backend. A second line names whether a commercial key was found on this machine and what follows from the answer, the way the observability panel already names why it is off.

## Acceptance criteria

- [x] **POL2-01** The board's toolbar answers to the board's width, and a narrow board puts the filters on a row of their own under the buttons.
- [x] **POL2-02** The row's edit control has no border under either theme.
- [x] **POL2-03** The details band's allergen chips are coloured under both themes.
- [x] **POL2-04** The internal note is separated from the blocks above and below it.
- [x] **POL2-05** Confirmed and Cancelled differ by hue and by a line through the cancelled word, in both themes and both colour schemes.
- [x] **POL2-06** A line's comment is hidden until the row has focus, and always shown when one is written.
- [x] **POL2-07** The message box is called "New message" and follows the reader's language.
- [x] **POL2-08** The language menu ticks the language in force, one at a time.
- [x] **POL2-09** The known customer picker offers customers before anything is typed, from the customers on existing orders.
- [x] **POL2-10** The photograph controls never latch: both open a chooser however many pictures have been taken.
- [x] **POL2-11** The counter screen offers the camera as well as the file chooser.
- [x] **POL2-12** The counter screen is a master detail, and the assistant closes and reopens without disturbing the form.
- [x] **POL2-13** A kitchen ticket can be dragged between columns, and a refused move says why and does not happen.
- [x] **POL2-14** Assigning a kitchen ticket names a baker.
- [x] **POL2-15** Products lists edit and delete as two named icons in the first column.
- [x] **POL2-16** The product column has a floor and the search spans the columns that do not filter.
- [x] **POL2-17** The price column reads as money and is edited as money.
- [x] **POL2-18** The product editor opens with the product's photo, and its fields are grouped by line.
- [x] **POL2-19** "New product" is under the table, at the end of the row.
- [x] **POL2-20** The dashboard's first widget is a full width banner on a different surface.
- [x] **POL2-21** The dashboard says what the range came to and when the counter is busy.
- [x] **POL2-22** Print and Mark as paid are named icons.
- [x] **POL2-23** The bakery's own details are written in one place and print on every invoice.
- [x] **POL2-24** The diagnostics subtitle is true, and names whether a key was found here.

## Still open

- The `theme="badge"` convention is a Lumo one and this pass replaced it in exactly one place, the details band, because that is the place the report named. Seven other views still put it on a `Span`: the kitchen ticket's allergens, the invoice status, the product card, the product page, the cart warning, the cost meter and the navbar's cart count. Every one of them is a coloured badge under Lumo and bare text under Aura. The row is in `FEEDBACK-25.3.md`.
- The product editor binds an allergen picker that is not on the form. It round trips correctly, so nothing is lost on save, and nothing can be edited either. It predates this pass and was left alone.
- `DashboardService.Today.slotUtilisationPercent` still computes booked against booked, which is always 100. Nothing reads it.
- The drag itself is not tested anywhere. Selenium's mouse does not raise the HTML5 drag events, so the gesture cannot be made in the browser tier either, and it joins the list in `NEXT.md` of the six browser tests that need a gesture rather than an assertion. What is tested is the decision a drop makes, through the method the drop listener calls, and the gesture was checked by hand in the browser: a card dragged from Confirmed to Preparing moved the order, and the same card dragged to Ready did not and said why.

- "Coloured under Aura" is a painted pixel, and nothing in the suite paints one. `POL2-15` asserts the thing that decides it instead: the chips are the platform's `Badge`, which is styled by both themes by construction, rather than a `Span` wearing a Lumo theme name. The pixels were compared by hand, in both themes, before and after.

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| POL2-01 | An amount typed the way a reader writes one | Parsing it for the price column | "2,45 €", "2.45" and "1.234,56" all read as the cents they mean, and nonsense reads as nothing | unit | `PriceFormatTest` |
| POL2-02 | An order with a line that carries a comment and one that does not | Rendering the editor | Only the row with a comment is marked to show it | browserless | `OrderEditorBrowserlessTest` |
| POL2-03 | The staff conversation | Reading the message box | It is named for the language in force, and it says "New message" in English | browserless | `ConversationBrowserlessTest` |
| POL2-04 | The shell in English and then in Spanish | Reading the language menu | Exactly one entry is ticked, and it is the one in force | browserless | `LocaleSwitchBrowserlessTest` |
| POL2-05 | Customers that appear on existing orders | Asking the picker with nothing typed | It answers with those customers rather than with nothing | unit | `CustomerServiceTest` |
| POL2-06 | The counter order screen with a model behind it | Opening it, closing the assistant and opening it again | The form is untouched and the assistant comes back | browserless | `PhoneOrderBrowserlessTest` |
| POL2-07 | A kitchen ticket and a baker who is not the current user | Assigning it to them | The ticket is theirs, and the board says so | browserless | `KitchenAssignmentBrowserlessTest` |
| POL2-08 | A confirmed ticket | Dropping it on Ready, which is not a legal move, and then on Preparing, which is | The first does not move it, the second does | browserless | `KitchenAssignmentBrowserlessTest` |
| POL2-09 | The product list | Reading the first column | Two icons, each with a name a screen reader can read | browserless | `ProductAdminBrowserlessTest` |
| POL2-10 | A product with a photo | Opening the editor | The image already has a source | browserless | `ProductAdminBrowserlessTest` |
| POL2-11 | The dashboard | Rendering a range | The first widget is the banner, and the totals answer with the range's orders, takings and average | browserless | `DashboardBrowserlessTest` |
| POL2-12 | A bakery letterhead written once | Printing any invoice | The letterhead is on the document | browserless | `InvoicePrintBrowserlessTest` |
| POL2-13 | This machine | Opening diagnostics | The subtitle claims only the build and the tests, and a second line names whether a key was found | browserless | `DiagnosticsBrowserlessTest` |
| POL2-14 | The order board at phone width | Opening it | The filters are on a row under the buttons, and the search field is still a field | testbench | `ResponsiveToolbarIT` |
| POL2-15 | An expanded row on the board | Reading the band's allergen chips | Each one is the platform's `Badge`, which both themes style | browserless | `OrderBandContentBrowserlessTest` |

POL2-14 is the only one here that a browser has to answer, and it is the only one that is about a measurement. The rest are decisions, and a decision is answered where it is made.

POL2-08 is the decision a drop makes and not the drop: a drop is a gesture, this tier cannot make one, and the gesture was checked by hand instead. That is why the rule is a method rather than a lambda inside the drop listener, and why the buttons still make the same move through the same code.
