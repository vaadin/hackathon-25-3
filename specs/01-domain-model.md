# 01 Domain model

Package `com.vaadin.bakery`, feature packages rather than layer packages. Entities are plain JPA classes with Jakarta validation. Records are used for read models and view models, never for entities, because Binder and Hibernate both want mutable beans.

## Diagram

```
User ─────────────┐ createdBy / assignedBaker
                  │
Customer ──1:N── Order ──1:N── OrderItem ──N:1── Product ──N:1── Category
   │              │  │                                  │
   │              │  ├──1:N── OrderHistoryItem          ├──N:M── Allergen
   │              │  └──1:N── OrderMessage ──1:N── Attachment
   │              │                                     └──0..1── ProductImage
   │              └──N:1── PickupLocation ──1:N── PickupClosure
   │
   └──1:1── Address (embedded)

Order ──1:0..1── Invoice ──1:N── InvoiceLine
```

## Conventions

- `AbstractEntity`: `@MappedSuperclass` with `Long id` (`@GeneratedValue(strategy = SEQUENCE)`, sequence starting at 10000 so the dataset owns the low ids) and `int version` (`@Version`). `equals` and `hashCode` on id only, `hashCode()` returns `getClass().hashCode()` so unsaved entities behave in sets. Never include `version` in identity: an optimistic lock bump must not change it.
- Money is `int` cents in the database and a `Money` record (cents plus currency) in Java. No `double` ever touches a price.
- All enums are real Java enums. `Role` is an enum, not the free string of the old app.
- Every user creatable entity carries `createdAt`, and `createdBy` where a staff member is responsible.
- `package-info.java` with `@NonNullApi` in every domain package.
- Deletion is refused, never cascaded, when an entity is referenced by an order. The service throws a domain exception that the UI renders as a sentence, not a stack trace.

## Entities

### Category

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `name` | String | `@NotBlank @Size(max=64) @Column(unique=true)` | Breads, Pastries, Cakes, Cookies, Savoury, Drinks |
| `slug` | String | `@NotBlank @Pattern("[a-z0-9-]+") @Column(unique=true)` | URL segment, `/shop/breads` |
| `displayOrder` | int | `@Min(0)` | |
| `iconName` | String | `@Size(max=64)` | Vaadin icon name |

### Allergen

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `code` | String | `@NotBlank @Size(max=32) @Column(unique=true)` | `GLUTEN`, `LACTOSE`, `EGG`, `NUTS`, `PEANUT`, `SOY`, `SESAME`, `SULPHITES`, `CELERY`, `MUSTARD`, `FISH`, `MOLLUSC` |
| `name` | String | `@NotBlank @Size(max=64)` | Translated through the bundle at render time |

An entity rather than an enum, because the allergen list is regulatory and an admin must be able to add one without a redeploy.

### Product

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `name` | String | `@NotBlank @Size(max=128) @Column(unique=true)` | |
| `slug` | String | `@NotBlank @Pattern("[a-z0-9-]+") @Column(unique=true)` | `/shop/breads/sourdough-loaf` |
| `category` | Category | `@ManyToOne(optional=false)` | |
| `descriptionMarkdown` | String | `@Size(max=4000)`, column `TEXT` | Rendered by the Markdown component, bound to a signal for live preview |
| `priceCents` | int | `@Min(1) @Max(100000)` | |
| `vatRate` | VatRate | `@NotNull` | `ZERO`, `REDUCED` 10 percent, `STANDARD` 21 percent |
| `imagePath` | String | `@Size(max=255)` | Seed image under `/images/products/`, null falls back to a category placeholder |
| `allergens` | Set&lt;Allergen&gt; | `@ManyToMany` | |
| `availableWeekdays` | Set&lt;DayOfWeek&gt; | `@ElementCollection` | Empty means every day. Feeds the disabled weekdays of the date picker |
| `leadTimeDays` | int | `@Min(0) @Max(14)` | 0 for stocked items, 2 for celebration cakes. The slowest product in a cart sets the earliest pickup date |
| `dailyCapacity` | Integer | `@Min(0)` | Null means unlimited. Feeds the date metadata provider |
| `stockToday` | int | `@Min(0)` | Edited inline in the products grid with GridPro |
| `available` | boolean | | Admin kill switch, hides the product from the storefront at once |
| `featured` | boolean | | Landing page |
| `sortOrder` | int | | |

Invariant: a product referenced by any order can be made unavailable but never deleted.

### ProductImage

Uploaded images live in the database so a restart does not lose them, while the seeded ones stay on disk as static resources. The resolver prefers the row and falls back to `imagePath`.

| Field | Type | Constraints |
| --- | --- | --- |
| `product` | Product | `@OneToOne(optional=false)` |
| `filename` | String | `@NotBlank @Size(max=255)` |
| `contentType` | String | `@NotBlank @Pattern("image/(png|jpeg|webp)")` |
| `sizeBytes` | long | `@Max(2097152)` | 2 MB ceiling, enforced by the upload component and again in the service |
| `data` | byte[] | `@Lob` |

### Address (embeddable)

`street` `@Size(max=160)`, `postalCode` `@Size(max=16)`, `city` `@Size(max=96)`, `country` `@Size(max=2)` ISO code, default `FI`.

### Customer

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `firstName`, `lastName` | String | `@NotBlank @Size(max=80)` | |
| `email` | String | `@NotBlank @Email @Column(unique=true)` | Lowercased on persist. The deduplication key |
| `phone` | String | `@NotBlank @Pattern("^(\\+\\d{1,3})?[ -]?(\\d[ -]?){6,14}$")` | |
| `address` | Address | `@Embedded @Valid` | Only required for an invoice, enforced with the `Billing` group |
| `vatId` | String | `@Size(max=20)` | Business customers |
| `notes` | String | `@Size(max=500)` | Staff only, never rendered to the customer |
| `marketingOptIn` | boolean | | |
| `createdAt` | Instant | `@NotNull` | |

Unlike the old app, where every order owned a private copy of its customer, customers are shared and deduplicated by email. A repeat order updates the name and phone from the newest submission.

### User

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `email` | String | `@NotBlank @Email @Column(unique=true)` | Login name, lowercased on persist |
| `passwordHash` | String | `@NotNull @Size(min=8, max=255)` | BCrypt |
| `firstName`, `lastName` | String | `@NotBlank @Size(max=80)` | |
| `role` | Role | `@NotNull` | `ADMIN`, `BAKER`, `BARISTA` |
| `locale` | String | `@Size(max=8)` | `en` or `es`, the initial UI language |
| `avatarPath` | String | `@Size(max=255)` | |
| `locked` | boolean | | A locked user cannot log in, be edited or be deleted |

### PickupLocation

| Field | Type | Constraints |
| --- | --- | --- |
| `name` | String | `@NotBlank @Size(max=96) @Column(unique=true)` |
| `address` | Address | `@Embedded @Valid` |
| `opensAt`, `closesAt` | LocalTime | `@NotNull` |
| `slotMinutes` | int | `@Min(15) @Max(120)`, default 30 |
| `slotCapacity` | int | `@Min(1)`, default 8 |
| `closedWeekdays` | Set&lt;DayOfWeek&gt; | `@ElementCollection` |
| `active` | boolean | |

Slots are computed, not stored. A slot is `location` plus `date` plus `time`, its capacity is `slotCapacity` unless a closure overrides it, and its load is a count of orders. The read model is the record `DaySlotLoad(LocalDate date, int capacity, int booked)`.

### PickupClosure

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `location` | PickupLocation | `@ManyToOne`, nullable | Null means every location |
| `date` | LocalDate | `@NotNull` | |
| `wholeDay` | boolean | | |
| `fromTime`, `toTime` | LocalTime | | Used when `wholeDay` is false |
| `reason` | String | `@NotBlank @Size(max=120)` | Shown in the date picker as the reason a day is disabled |
| `kind` | ClosureKind | `@NotNull` | `HOLIDAY` or `MAINTENANCE` |

### Order

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `reference` | String | `@NotBlank @Column(unique=true)` | `ORD-2026-000123`, assigned on first save |
| `customer` | Customer | `@ManyToOne(optional=false)` | A real foreign key, no cascade. Deleting an order never deletes a customer |
| `pickupLocation` | PickupLocation | `@ManyToOne(optional=false)` | |
| `pickupDate` | LocalDate | `@NotNull` | |
| `pickupTime` | LocalTime | `@NotNull` | |
| `state` | OrderState | `@NotNull` | |
| `channel` | Channel | `@NotNull` | `ONLINE`, `PHONE`, `EMAIL`, `COUNTER` |
| `assignedBaker` | User | `@ManyToOne` | Set from the kitchen board |
| `items` | List&lt;OrderItem&gt; | `@OneToMany(cascade=ALL, orphanRemoval=true) @OrderColumn @NotEmpty @Valid` | |
| `history` | List&lt;OrderHistoryItem&gt; | `@OneToMany(cascade=ALL) @OrderColumn`, lazy | |
| `messages` | List&lt;OrderMessage&gt; | `@OneToMany(cascade=ALL) @OrderColumn`, lazy | |
| `customerNote` | String | `@Size(max=500)` | Free text from the customer. Sanitized with a jsoup Safelist before it is ever rendered |
| `internalNote` | String | `@Size(max=500)` | Staff only |
| `totalNetCents`, `totalVatCents`, `totalGrossCents` | int | `@Min(0)` | Recomputed by the service on every mutation, so a grid never has to sum a lazy collection |
| `placedAt` | Instant | `@NotNull` | |
| `createdBy` | User | `@ManyToOne` | Null for online orders |

Entity graphs: `Order.brief` fetches customer and pickup location, `Order.full` adds items, products and history.

State machine:

```
NEW ──▶ CONFIRMED ──▶ IN_PREPARATION ──▶ READY ──▶ PICKED_UP
 │          │                │             │
 └──────────┴────────────────┴─────────────┴──▶ PROBLEM ──▶ CANCELLED or back to CONFIRMED
 └──▶ CANCELLED
```

Invariants: every transition appends an `OrderHistoryItem`. A `PICKED_UP` order is immutable except for messages, and reaching it issues the invoice. Cancelling frees the slot. Only `ADMIN` may move an order out of `PICKED_UP`. A customer may cancel only while the order is `NEW`.

### OrderItem

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `product` | Product | `@ManyToOne(optional=false)` | |
| `quantity` | int | `@Min(1) @Max(99)` | |
| `unitPriceCents` | int | `@Min(0)` | Snapshot taken when the line is created. A later price change never rewrites history |
| `vatRate` | VatRate | `@NotNull` | Snapshot, same reason |
| `comment` | String | `@Size(max=255)` | "Gluten free", "Happy birthday Ana" |

### OrderHistoryItem

`newState` (nullable for a plain note), `message` `@NotBlank @Size(max=1000)`, `timestamp` `@NotNull`, `createdBy` `@ManyToOne`.

### OrderMessage

The conversation with the customer, kept apart from the state history so the message list has one clean source.

| Field | Type | Constraints |
| --- | --- | --- |
| `authorName` | String | `@NotBlank @Size(max=160)` |
| `fromStaff` | boolean | |
| `author` | User | `@ManyToOne`, null when the customer wrote it |
| `text` | String | `@NotBlank @Size(max=2000)` |
| `sentAt` | Instant | `@NotNull` |
| `readByStaff` | boolean | |

### OrderMessageAttachment

`filename`, `contentType` `@Pattern("image/(png|jpeg|webp)|application/pdf")`, `sizeBytes` `@Max(2097152)`, `data` `@Lob`.

### Invoice

| Field | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `number` | String | `@NotBlank @Column(unique=true)` | `2026-000123`, sequential per year with no gaps, issued by a transactional counter |
| `order` | Order | `@OneToOne(optional=false)` | |
| `status` | InvoiceStatus | `@NotNull` | `ISSUED`, `PAID`, `VOID` |
| `issuedAt` | LocalDate | `@NotNull` | |
| `dueAt` | LocalDate | `@NotNull` | 14 days after issue |
| `billingName`, `billingEmail` | String | `@NotBlank` | Snapshot |
| `billingAddress` | Address | `@Embedded` | Snapshot |
| `vatId` | String | `@Size(max=20)` | Snapshot |
| `lines` | List&lt;InvoiceLine&gt; | `@OneToMany(cascade=ALL) @OrderColumn @NotEmpty` | |
| `netCents`, `vatCents`, `grossCents` | int | `@Min(0)` | Stored, not derived. They are the legal record |
| `paid` | boolean, `paidAt` Instant | | |

Invariants: an issued invoice is immutable. Corrections void it and issue a new one. `grossCents` equals `netCents` plus `vatCents` for every invoice, asserted by a test that walks the entire dataset.

### InvoiceLine

`description` (snapshot of the product name), `quantity`, `unitPriceCents`, `vatRatePercent`, `netCents`, `vatCents`, `grossCents`, `sortOrder`. No foreign key to `Product`, so a line survives a catalogue deletion.

## Not entities, on purpose

| Thing | Where it lives |
| --- | --- |
| Cart | `@VaadinSessionScope CartSignals` with a `ListSignal<CartLine>` and computed totals. Never persisted |
| Slot availability | Computed by `SlotService` from location, closures and order counts |
| Order card, kitchen ticket, sales point | Java records returned by services. No display DTOs with preformatted strings |

Turning a cart into an order is one service call that revalidates prices, availability and slot capacity, because the cart may have been open for an hour.

## Validation groups

Declared in `com.vaadin.bakery.domain.validation`, the direct use of the new validation group support in `BeanValidationBinder`.

| Group | Applied when | Adds on top of `Default` |
| --- | --- | --- |
| `Default` | always | Field shape: sizes, patterns, ranges |
| `OnDraft` | saving an unfinished counter order | Nothing. A draft may be incomplete |
| `OnSubmit` | placing an order, online or at the counter | Contact details present, cart not empty, pickup date not in the past, lead time respected, slot has capacity |
| `Billing` | issuing an invoice | Billing address complete, VAT id shape when present |

## What we chose not to model, and why

| Rejected | Reason |
| --- | --- |
| Ingredients, recipes, stock depletion | Unit conversion and recipe explosion is days of modelling and buys one more grid. A single `stockToday` integer gets the GridPro demo for nothing |
| Staff shifts and rotas | A scheduling UI. The calendar story is already covered by pickup slots and closures |
| Delivery and routing | Pickup only, like the original. The one embedded address survives for invoicing |
| Payment gateway | No 25.3 surface. Reduced to `paid` and `paidAt` |
| Customer accounts with login | A whole registration and password reset epic. Replaced by an accountless tracking link at `/track/{reference}` |
| Loyalty, coupons, reviews | Zero new feature coverage |
