# Epic 07: Admin

**Goal:** catalogue, people and configuration, without a generic CRUD abstraction.
**Feature spec:** `specs/features/07-admin.md`
**Dependencies:** epics 02 and 03.

---

### US-7.1: Products grid

**Tasks:**
- [x] Grid with GridPro inline editing of price and today's stock, validated inline
- [ ] `Switch` columns for available and featured, applied at once with an undo toast
- [x] Refusal to delete a referenced product, with the offer to mark it unavailable

**25.3 APIs:** Switch, GridPro.
**Verified by:** `ProductAdminBrowserlessTest`, `GridProEditIT`

---

### US-7.2: Product editor

**Tasks:**
- [x] Dialog with `BeanValidationBinder` and `bindInstanceFields`
- [x] Category `ComboBox` in partial match mode, allergens in a `MultiSelectComboBox`
- [x] Markdown field with a live preview sharing the public rendering path

**25.3 APIs:** ComboBox partial match, Markdown with signal binding.
**Verified by:** `ProductAdminBrowserlessTest`, `PartialMatchBrowserlessTest`

---

### US-7.3: Product images

**Tasks:**
- [x] `UploadManager` coordinating `UploadDropZone`, `UploadButton` and `UploadFileList`
- [x] Clipboard paste of an image onto the drop zone
- [x] Type, magic bytes and 2 MB checks, with readable rejections in the file list
- [x] Store as `ProductImage`, served by the application, overriding the seeded file

**25.3 APIs:** modular Upload, Clipboard paste.
**Verified by:** `UploadDropZoneIT`, `ClipboardPasteIT`, `UploadValidationBrowserlessTest`

---

### US-7.4: Categories, allergens, locations and closures

**Tasks:**
- [ ] Category and allergen grids with inline creation and drag reordering
- [ ] Location editor with hours, slot length, capacity and closed weekdays
- [x] Closure editor that warns about affected existing orders before saving
- [x] Changes visible immediately in the public date picker

**Verified by:** `ClosureAdminBrowserlessTest`, `SlotSelectionBrowserlessTest`

---

### US-7.5: Users

**Tasks:**
- [x] Grid with a `Switch` for locked
- [x] Password field empty on open, re encoded only when typed
- [x] Refusals: delete yourself, edit or delete a locked user

**Verified by:** `UserAdminBrowserlessTest`

---

### US-7.6: Customer picker

**Tasks:**
- [x] Partial match `ComboBox` over name, email and phone
- [ ] Inline create dialog when there is no match, human only

**Verified by:** `PartialMatchBrowserlessTest`

---

## Left undone

- The availability and featured switches apply one at a time with no undo toast.
- US-7.4 is half built. Closures have their own view, with the warning about orders it would strand. Categories, allergens and locations have no editor at all, so those three are changed in SQL.
- The customer picker matches on any fragment. There is no inline create dialog when nothing matches.

## Definition of Done

- [ ] Every acceptance criterion in `features/07-admin.md` is checked
- [ ] An admin can run the bakery for a day without touching the database
