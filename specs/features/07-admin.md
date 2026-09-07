# Feature 07: Admin

## Overview

Catalogue, people and configuration. Replaces `AbstractBakeryCrudView` and the shared Lit search bar, and carries the Switch, GridPro, modular Upload, Clipboard and ComboBox partial match stories.

**On `Crud`.** This document originally rejected the component outright, and that was too broad. Where a view is a list and an editor and nothing else, `Crud` is what it should be: people and closures use it, and they no longer hand write a new item button, an editor dialog, a delete confirmation and the keyboard handling that goes with them. The catalogue does not, and deliberately: its grid is a `GridPro` with inline editing on price and stock, which is one of the stories this application exists to show, and a `Crud` around it would put a second editor over a grid that already edits. The decision is per view, and the reason is inline editing rather than taste.

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
- [ ] The availability switch takes effect on the storefront immediately
- [ ] The markdown preview matches what the public page renders

### AC2: Images work three ways
- [ ] Drag and drop uploads a photo and it appears on the card
- [ ] The button upload does the same
- [ ] Pasting an image from the clipboard onto the drop zone does the same
- [ ] An oversized or wrong type file is rejected with a readable reason

### AC3: Partial match helps
- [ ] Typing a fragment from the middle of a category name finds it
- [ ] The customer picker matches on name, email and phone fragments

### AC4: People rules hold
- [ ] Opening a user and saving without typing a password leaves the password unchanged
- [ ] Deleting yourself is refused
- [ ] A locked user cannot be edited or deleted

### AC5: Configuration reaches the storefront
- [ ] Adding a closure removes that day from the public date picker
- [ ] A closure over open orders warns, names them, and saves nothing until it is answered
- [ ] Changing a location's slot length changes the offered times

### Still open

Nothing in this document is built yet.


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
