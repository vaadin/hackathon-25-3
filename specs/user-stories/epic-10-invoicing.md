# Epic 10: Invoicing and printable documents

**Goal:** money leaving the building, on paper an accountant accepts.
**Feature spec:** `specs/features/10-invoicing.md`
**Dependencies:** epics 02 and 06.

---

### US-10.1: Issuing

**Tasks:**
- [x] Issue on pickup, in the same transaction, with full billing and line snapshots
- [ ] Apply the `Billing` validation group and name missing fields
- [x] VAT summarised per rate, arithmetic asserted over the whole dataset

**Verified by:** `InvoicingBrowserlessTest`, `DatasetIntegrityTest`

---

### US-10.2: Invoice list

**Tasks:**
- [x] `/admin/invoices` with filters by status, date range and customer
- [ ] Mark as paid with an undo window, recording who and when
- [x] CSV export of exactly the filtered rows, machine formatted numbers

**Verified by:** `InvoiceExportBrowserlessTest`

---

### US-10.3: Print view

**Tasks:**
- [x] `/invoices/{number}/print` built with the `Table` family, rebuilt from the data rather than with `bindChildren`, which does not exist
- [x] Print stylesheet: shell hidden, black on white, page margins, repeating table header
- [ ] Void watermark for voided invoices

**25.3 APIs:** Table family.
**Verified by:** `InvoicePrintIT`

---

### US-10.4: Voiding

**Tasks:**
- [x] Admin only, reason required, recorded in the order history
- [ ] Reopening a picked up order voids its invoice automatically
- [x] Numbers are never reused

**Verified by:** `InvoicingBrowserlessTest`, `InvoiceNumberingTest`

---

## Left undone

- The `Billing` validation group is declared and the issuing path does not apply it, so a missing billing field is not named at the moment of issue.
- Mark as paid, with its undo window and its record of who and when, is not built.
- No void watermark on the printed document.
- Reopening a picked up order does not void its invoice. The state machine allows the move and billing is not told.

## Definition of Done

- [ ] Every acceptance criterion in `features/10-invoicing.md` is checked
- [ ] A printed invoice on A4 needs no manual fixing
