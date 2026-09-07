# 02 Dataset

The old app fabricated 10k to 20k orders in a `@PostConstruct` on every boot. It was slow, it was different on every machine, and nobody could read it. This one ships a static, reviewable SQL dataset that still looks alive on the day of the demo.

## Shape

| File | Purpose |
| --- | --- |
| `src/main/resources/schema.sql` | Explicit DDL. Hibernate runs with `ddl-auto=validate`, so a mismatch between entities and schema fails the boot instead of silently rewriting tables |
| `src/main/resources/data-h2.sql` | The demo data for H2, the default |
| `src/main/resources/data-postgresql.sql` | The same data for PostgreSQL, selected with `spring.sql.init.platform=postgresql` |
| `scripts/generate-dataset.mjs` | Emits both files from a seeded generator. Committed, so the dataset is regenerated rather than hand edited |
| `scripts/fetch-product-images.mjs` | Downloads, crops and converts the product photos, and writes `CREDITS.md` |

Both SQL files are generated from one model with one seed, so they are identical row for row.

## Volumes

| Table | Rows | Note |
| --- | --- | --- |
| `app_user` | 8 | 1 admin, 3 bakers, 4 baristas, one of them locked |
| `category` | 6 | Breads, Pastries, Cakes, Cookies, Savoury, Drinks |
| `allergen` | 12 | Subset of the EU 14 |
| `product` | 48 | 6 to 10 per category, every one with a photo |
| `product_allergen` | about 110 | |
| `pickup_location` | 3 | 30 minute slots, capacities 12, 8 and 6 |
| `pickup_closure` | 14 | Holidays plus two maintenance days. Sundays are a weekday rule, not rows |
| `customer` | 220 | 12 of them with a VAT id |
| `orders` | 1400 | 90 days back, 21 days forward |
| `order_item` | about 4200 | Three lines on average |
| `order_history_item` | about 4000 | |
| `order_message` | 180 | On about 90 orders |
| `order_message_attachment` | 24 | Small inline images |
| `invoice` | about 900 | One per picked up order |
| `invoice_line` | about 2700 | |

1400 orders, not 20000. That is enough to exercise lazy loading and to make the dashboard interesting, small enough that `data-h2.sql` stays around 2 MB and a human can read a diff of it. The `bulk` profile inflates the data to 50k orders programmatically for the observability and load demos. Never as SQL.

## Keeping it alive relative to today

A static file cannot contain "yesterday". The design is a fixed anchor plus a whole week shift at boot.

1. Every date in the file is literal and relative to the anchor `2026-01-05`, deliberately a **Monday**.
2. On startup, `DemoDateShifter` (an `ApplicationRunner` guarded by `demo.shift-dates=true`) issues a handful of bulk updates that add a fixed number of days to every date column.
3. The shift is a whole number of weeks: `weekShift = 7 * floor(daysBetween(ANCHOR, today) / 7)`.

The whole week rule is the important part. Shifting by a raw day count turns a seeded Tuesday into a Friday, which silently breaks "closed on Sundays", misaligns the closures and destroys the slot load pattern. Shifting by whole weeks preserves weekday alignment exactly. The data is never more than six days stale and never internally inconsistent.

Because states were derived from the pickup date at generation time, the relation to today survives the shift: past orders are picked up or cancelled, the anchor week carries a deliberate spread across all seven states, and future orders are new or confirmed. A small `StateReconciler` then nudges a handful of today's orders so the kitchen board is never empty when somebody demos at four in the afternoon.

Tests pin the clock instead: `application-test.properties` sets `demo.shift-dates=false` and a fixed `demo.today`, and a `Clock` bean is injected everywhere a date is compared. No assertion in the suite depends on the wall clock.

## Realism rules

The dataset has to survive being looked at during a demo.

- Prices follow the category: a cookie is not 48 euros. Each category declares a price band and the generator draws inside it.
- Product popularity is skewed, not uniform. Ten products carry half the volume, which makes the top products chart worth looking at.
- Orders cluster around opening and closing, with a lunch peak, so slot utilisation shows a shape rather than noise.
- Volume trends upward across the 90 days and jumps on Fridays and Saturdays, so the revenue chart has a story.
- About one order in eight carries an item comment, one in twelve has a customer note, one in fifteen ends in a problem or a cancellation.
- Names come from a wide pool across several languages. Emails follow from the names. Phones use documentation ranges.
- Twelve customers are businesses with a VAT id, so the invoice view has both shapes.

## Product photos

| Option | Verdict |
| --- | --- |
| Unsplash or Pexels | Permissive but the provenance is muddy and curation is manual |
| AI generated | Ownership is fine, but it is the wrong signal for a Vaadin branded repository |
| Inline SVG illustrations | Zero risk and tiny, but the storefront is the first impression and it would look like a placeholder |
| Wikimedia Commons and Open Food Facts, CC0 or CC BY | Chosen. Real food photography, clean licence, attributable |

48 photos, 640 by 480 WebP at quality 75, about 25 to 40 KB each and roughly 2 MB in total, committed under `src/main/resources/META-INF/resources/images/products/`. Each one also gets a 64 pixel blurred placeholder for the loading state. `CREDITS.md` in the same folder lists source URL, author and licence per file. No Git LFS.

`product_image` starts empty. The uploaded image path is exactly what epic 07 demonstrates: drop a photo on a product and it goes into the database and overrides the seeded file.

## Integrity

A dataset that lies is worse than no dataset. `DatasetIntegrityTest` runs in the normal test phase and asserts:

- Every order has at least one item, and every item has a product that exists.
- Every order total equals the sum of its lines, and every invoice satisfies `gross = net + vat`.
- No order sits on a closed weekday or a closure date for its location.
- No slot is overbooked: no location, date and time combination exceeds its capacity.
- Every invoice number is unique and the yearly sequence has no gaps.
- Every product has either an image file that exists on disk or a category placeholder.
- Every referenced image file is listed in `CREDITS.md`.

## Demo accounts

Printed on the login screen, as in the original.

| Email | Password | Role |
| --- | --- | --- |
| `admin@bakery.test` | `admin` | ADMIN |
| `baker@bakery.test` | `baker` | BAKER |
| `barista@bakery.test` | `barista` | BARISTA |

Passwords are BCrypt hashes in the SQL. They are demo credentials and the README says so out loud.
