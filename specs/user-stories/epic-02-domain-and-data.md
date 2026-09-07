# Epic 02: Domain, persistence and dataset

**Goal:** every entity, repository and service, plus a static dataset that looks alive.
**Feature spec:** `specs/features/02-domain-and-data.md`, model in `specs/01-domain-model.md`, data in `specs/02-data-set.md`
**Dependencies:** epic 01.

---

### US-2.1: Entities and schema

**Tasks:**
- [x] Every entity of `01-domain-model.md` with its constraints, enums and entity graphs
- [x] `Money` record and the VAT rules
- [x] Explicit `schema.sql`, Hibernate in `validate` mode, sequences from 10000
- [x] Validation groups `OnDraft`, `OnSubmit`, `Billing` declared

**Verified by:** `SchemaValidationTest`

---

### US-2.2: Repositories and services

**Tasks:**
- [x] Spring Data repositories with Specifications for dynamic filters
- [x] `CatalogueService`, `SlotService`, `OrderService`, `CustomerService`, `InvoiceService`, `UserService`, `DashboardService`
- [x] `DomainException` hierarchy with translation keys, and one global handler
- [x] Read models as records, never display DTOs with preformatted strings

**Verified by:** `OrderServiceTest`, `CatalogueServiceTest`, `CustomerServiceTest`

---

### US-2.3: Ordering rules

**Tasks:**
- [x] Cart to order in one transaction: revalidate availability, snapshot prices, check lead time, book the slot
- [x] State machine with history on every transition, slot release on cancel
- [x] Reference and tracking token generation
- [x] Optimistic failure path that reports "that slot just filled up"

**Verified by:** `OrderStateMachineTest`, `SlotConcurrencyTest`

---

### US-2.4: Invoice numbering

**Tasks:**
- [x] Transactional per year counter, gap free
- [x] Issue on pickup with full snapshots
- [x] Void with a reason, never reusing a number

**Verified by:** `InvoiceNumberingTest`

---

### US-2.5: Dataset generator

**Tasks:**
- [ ] `scripts/generate-dataset.mjs`, seeded, emitting `data-h2.sql` and `data-postgresql.sql` byte identically per run
- [x] Volumes and realism rules from `02-data-set.md`
- [x] `scripts/fetch-product-images.mjs` producing 48 WebP images plus placeholders and `CREDITS.md`
- [x] `DemoDataShifter` with the whole week shift, so the weekday alignment of the dataset survives being moved to today
- [x] `DatasetIntegrityTest` with every check listed in the data spec

**Verified by:** `DatasetIntegrityTest`, `DemoDateShifterTest`

---

## Left undone

- The generator is not asserted to emit byte identical SQL for the same seed, so a regenerated dataset is reviewed by eye.
- PostgreSQL is never booted. Both the schema and the data script are written for it and nothing runs them.

## Definition of Done

- [ ] The application boots on H2 and on PostgreSQL with `validate` and no warning
- [x] Booting on any weekday leaves a coherent, non stale dataset
- [ ] Every acceptance criterion in `features/02-domain-and-data.md` is checked
