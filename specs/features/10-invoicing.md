# Feature 10: Invoicing and printable documents

## Overview

Money leaving the building. An invoice is issued when an order is picked up, it is immutable, its numbering has no gaps, and it prints on paper that an accountant will accept.

Covers B7, B8, B11, C6, A11.

## Behaviour

### Issuing

Marking an order picked up issues its invoice in the same transaction. The invoice snapshots the billing name, email, address and VAT id, and copies every line with its own price and VAT rate. Nothing in it refers to a live product row.

Numbering is `YYYY-NNNNNN`, sequential per year, allocated by a transactional counter. Gaps are a defect, and the integrity test asserts there are none.

The `Billing` validation group applies here: a business customer without a complete billing address cannot be invoiced, and the barista is told which field is missing.

### Invoice list, route `/admin/invoices`

Grid of number, date, customer, net, VAT, gross, status. Filters by status, date range and customer. Mark as paid is a single action with an undo window. CSV export exports exactly what the filter shows, with a header row and locale independent numbers.

### Print view, route `/invoices/{number}/print`

- A clean document: bakery header, billing block, a line table, a VAT summary per rate, totals, payment terms and the due date.
- The line table and the VAT summary are built with the new `Table` family, `bindChildren` over the lines, so the printed markup is a real table.
- A dedicated print stylesheet hides the shell, forces black on white, sets page margins, and repeats the table header on every page.
- No PDF library. Print to PDF from the browser is the deliverable, and the demo script says so out loud.

### The language of an invoice

An invoice keeps the language it was issued in, for good. It is a document of record: two people opening the same invoice see the same words, and the copy filed in January still reads in January's language whatever the reader has selected today. This is the one screen in the application that does not follow the language selector, and the exception is deliberate.

### Voiding

Admin only, requires a reason, records it in the order history, and never reuses the number. A voided invoice prints with a void watermark.

### The export runs outside the session

A download is its own request. It arrives without the session lock, which the download documentation mentions in passing and nothing in the API prevents, so the export reads no UI state at all: the filter and the locale are copied into a snapshot by the same effect that reloads the grid, and the request reads that. Reading the signals from there was a race with whoever was typing in the filter.

And it cannot fail silently. A download has no screen to fail on, so an exception left to propagate reaches the browser as whatever the container makes of it and reaches the person as nothing, which is what "the application crashed" looks like from the outside. The callback catches, logs the filter that produced the failure, and answers `DownloadResponse.error(500, ...)` with a sentence.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A picked up order is reopened by an admin | The invoice is voided automatically and the reason names the reopening |
| A business customer has no billing address | Issuing is refused, naming the missing fields |
| Two pickups happen at the same instant | Both get numbers, both sequential, no duplicate |
| An invoice is printed on a narrow screen | The print stylesheet still produces an A4 document |
| A CSV export in Spanish | Numbers use a fixed machine format, not the locale's decimal comma |

## Acceptance criteria

### AC1: Issuing is correct
- [x] Picking up an order issues exactly one invoice with correct snapshots and totals
- [x] Gross equals net plus VAT for every invoice in the dataset
- [x] Numbers are sequential per year with no gaps

### AC2: The list is useful
- [ ] Filters by status, date range and customer work together
- [ ] Mark as paid records who and when, and can be undone within the window
- [x] The CSV contains exactly the filtered rows

### AC3: The print view is printable
- [x] The shell is hidden and the document is black on white
- [x] The line table is real table markup with a repeating header
- [x] A VAT summary per rate is present and adds up

### AC4: Voiding is controlled
- [ ] Only an admin can void, and a reason is required
- [ ] A voided invoice prints with a watermark and its number is never reused


### Still open

- An invoice does not remember the language it was issued in. INV-09 asks that an invoice issued in Spanish still reads Spanish when the reader has English selected, and there is nowhere to put that: the entity has no locale column, so the document is rendered in whoever is looking at it. `InvoiceLocaleBrowserlessTest` stays named and unwritten rather than testing something else.

- The three filters are exercised one at a time through the export test. Nothing asserts them working together, which is where a filter bug would actually live.
- Mark as paid, and undoing it inside the window, has no test.
- AC3 is measured by `InvoicePrintIT` under emulated print media, which needs CDP: there is no Selenium API for a medium, and TestBench hands back a proxy that has to be unwrapped before Chrome will take the command.
- Voiding has no test of its own. That a voided number is never reused is covered by `InvoiceNumberingTest`; that only an admin may void, that a reason is required, and that the watermark prints are not.

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| INV-01 | A ready order | Marking it picked up | One invoice issued with matching totals | browserless | `OrderDetailBrowserlessTest` |
| INV-02 | A business customer with no address | Marking picked up | Refused, naming the missing fields | browserless | `OrderDetailBrowserlessTest` |
| INV-03 | The seeded invoices | Checking arithmetic | Gross equals net plus VAT everywhere | unit | `DatasetIntegrityTest` |
| INV-04 | The last invoice of a year | Issuing two more concurrently | Two distinct sequential numbers | unit | `InvoiceNumberingTest` |
| INV-05 | The invoice list filtered by month | Exporting CSV | The file has exactly those rows with machine formatted numbers | browserless | `InvoiceListBrowserlessTest` |
| INV-06 | An issued invoice | Opening the print view with print media emulated | Shell hidden, table markup present, VAT summary correct | testbench | `InvoicePrintIT` |
| INV-07 | An issued invoice | Voiding it as an admin with a reason | Status void, reason in the order history, number not reused | browserless | `OrderDetailBrowserlessTest` |
| INV-08 | A picked up order | Reopening it as an admin | The invoice is voided automatically | browserless | `OrderDetailBrowserlessTest` |
| INV-09 | An invoice issued in Spanish | Opening it with English selected | It still reads Spanish, labels and formatted amounts alike | browserless | `InvoiceLocaleBrowserlessTest` |
