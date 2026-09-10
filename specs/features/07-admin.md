# Feature 07: Admin

## Overview

Catalogue, people and configuration. Replaces `AbstractBakeryCrudView` and the shared Lit search bar, and carries the Switch, GridPro, modular Upload, Clipboard and ComboBox partial match stories.

**On `Crud`.** The decision is per view, and the reason is inline editing rather than taste. Where a view is a list and an editor and nothing else, `Crud` is what it should be: people and closures use it, and none of them hand writes a new item button, an editor dialog, a delete confirmation and the keyboard handling that goes with them. The catalogue does not, and deliberately: its grid is a `GridPro` with inline editing on price and stock, which is one of the stories this application exists to show, and a `Crud` around it would put a second editor over a grid that already edits.

Covers A1 to A8, B4, B5, K6.

## Behaviour

### Products, route `/admin/products`

- Grid with inline editing through GridPro on two columns: price and today's stock. Everything else is edited in a dialog.
- `Switch` in the grid for available and featured, applied immediately with an undo toast.
- The editor dialog uses `BeanValidationBinder` with the `Default` group, and a category `ComboBox` in partial match mode, so typing "cake" finds "Celebration cake" and not just names starting with cake.
- Allergens are a `MultiSelectComboBox`.
- The markdown description has a live preview beside the field, the same Markdown component the public page uses, bound to the same signal.
- The image area is the modular Upload set: `UploadDropZone` around the preview, `UploadButton` for the file dialog, `UploadFileList` for progress and errors, all coordinated by one `UploadManager`. A photo can also be pasted from the clipboard directly onto the drop zone.
- Constraints on upload: PNG, JPEG or WebP, at most 2 MB, checked by content type and magic bytes. Rejections appear in the file list with a readable reason, never as a stack trace.
- Deleting a product that has orders is refused with an offer to mark it unavailable instead.

### Categories and allergens, route `/admin/categories`

Two simple grids on one page with inline creation. Reordering categories by drag changes their display order. Deleting a category with products is refused.

### Locations and closures, routes `/admin/locations` and `/admin/closures`

- Locations: name, address, opening and closing time, slot length, capacity, closed weekdays, active.
- Closures: a calendar oriented list. Adding a closure for a location immediately changes what the storefront date picker offers, which is the demo worth showing.
- A closure that would strand existing orders warns and lists them before saving.

### Users, route `/admin/users`

- Grid of email, name, role, locked.
- `Switch` for locked.
- The password field is empty when the editor opens and only re encodes when something is typed, so opening a user does not wipe their password.
- Deleting yourself is refused. Editing or deleting a locked user is refused until it is unlocked.

### Customer picker, used by the counter

A `ComboBox` in partial match mode over customers, matching on any part of name, email or phone. When there is no match, an inline create action opens a small dialog with the three required fields. The assistant may search this picker but may never create a customer.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| An upload exceeds 2 MB | Rejected in the file list with the size named. Nothing reaches the database |
| A file claims to be a PNG but is not | Rejected on magic bytes |
| A paste contains text, not an image | Ignored, with a hint that an image is expected |
| GridPro edit sets a price of zero | Rejected inline, the cell keeps the old value |
| Two admins edit the same product | The second save fails on the version and offers to reload |
| A closure is added over existing orders | The warning lists them, and saving does not cancel anything automatically |
| A category is deleted with products | Refused with the count |

## Acceptance criteria

### AC1: Products can be managed
- [ ] Price and stock are editable inline and persist
- [x] The availability switch takes effect on the storefront immediately
- [x] The markdown preview matches what the public page renders

### AC2: Images work three ways
- [ ] Drag and drop uploads a photo and it appears on the card
- [x] The button upload does the same
- [ ] Pasting an image from the clipboard onto the drop zone does the same
- [x] An oversized or wrong type file is rejected with a readable reason

### AC3: Partial match helps
- [x] Typing a fragment from the middle of a category name finds it
- [x] The customer picker matches on name, email and phone fragments

### AC4: People rules hold
- [x] Opening a user and saving without typing a password leaves the password unchanged
- [x] Deleting yourself is refused
- [x] A locked user cannot be edited or deleted

### AC5: The lists are `Crud`, except the one that is not
- [x] People and closures are `Crud`, with its new item button, its editor and its delete confirmation
- [x] The catalogue keeps `GridPro` and gets a new product control of its own
- [x] Every catalogue column a person would sort by is sortable
- [x] The catalogue filters are a header row inside the grid

### AC6: Configuration reaches the storefront
- [x] Adding a closure removes that day from the public date picker
- [x] A closure over open orders warns, names them, and saves nothing until it is answered
- [ ] Changing a location's slot length changes the offered times

### Still open

- Inline editing is the reason GridPro is in this application and it is untested. The cell editing itself needs a browser, `GridProEditIT`, but that the edited price and stock persist could be asserted browserless today and is not.
- The preview showed the raw markdown while the public page renders it cleaned, so anything the safelist strips looked fine to whoever wrote it and vanished for everybody else. Both sides clean now, and `MarkdownPreviewBrowserlessTest` compares them.
- The markdown preview matching what the public page renders has no test.
- Two of the three upload routes wait on the browser tier: drag and drop, `UploadDropZoneIT`, and clipboard paste, `ClipboardPasteIT`. The validation half is covered browserless, including a file that only claims to be an image.
- Changing a location's slot length changing the offered times is untested. Closures are covered.

## Changed by the second polish pass

The products screen moved in `16-polish-02.md`: edit and delete are two named icons in the first column, the product column has a floor, the search spans the columns that carry no filter of their own, the price column reads and is edited as money while the storage stays in integer cents, and "New product" is under the table at the end of the row. The editor opens showing the product's own photo, puts price beside VAT, puts stock, days of notice, on sale and featured on one line, and gives the description and its preview the full width of the dialog.

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| ADM-01 | The products grid | Editing a price inline | The new price persists and appears on the storefront | testbench | `GridProEditIT` |
| ADM-02 | A product marked available | Flipping the switch | It disappears from the catalogue at once | browserless | `ProductAdminBrowserlessTest` |
| ADM-03 | The editor with a markdown description | Typing in the field | The preview updates live | browserless | `ProductAdminBrowserlessTest` |
| ADM-04 | The editor | Dropping a valid photo | It uploads and becomes the product image | testbench | `UploadDropZoneIT` |
| ADM-05 | The editor | Pasting an image from the clipboard | Same result as dropping | testbench | `ClipboardPasteIT` |
| ADM-06 | The editor | Dropping a 5 MB file | Rejected with the size named, nothing stored | browserless | `UploadValidationBrowserlessTest` |
| ADM-07 | The category combo box | Typing a middle fragment | The matching category is offered | browserless | `PartialMatchBrowserlessTest` |
| ADM-08 | The customer picker | Typing part of a phone number | The customer is found | browserless | `PartialMatchBrowserlessTest` |
| ADM-09 | A user editor | Saving without touching the password | The user can still log in | browserless | `UserAdminBrowserlessTest` |
| ADM-10 | The admin's own row | Deleting it | Refused with a message | browserless | `UserAdminBrowserlessTest` |
| ADM-11 | A locked user | Editing it | Refused | browserless | `UserAdminBrowserlessTest` |
| ADM-12 | A new closure on a day with orders | Saving | The warning lists the affected orders | browserless | `ClosureAdminBrowserlessTest` |
| ADM-13 | A closure saved | Opening the public date picker | That day is disabled with the reason | browserless | `SlotSelectionBrowserlessTest` |
| ADM-14 | The catalogue admin | Opening it | A new product can be started, every listed column sorts, and the filters narrow the grid from its own header row | browserless | `CatalogueCrudBrowserlessTest` |
