# 00 Overview

## What we are building

Bakery 25.3 is a rewrite of the classic Vaadin Bakery starter for Vaadin 25.3. It is a working bakery: an anonymous visitor browses the catalogue and orders online, counter staff take orders and drive them through the kitchen, bakers watch a shared production board, and an administrator manages the catalogue, the people, the invoices and the metrics.

The rewrite exists for two reasons. First, the original starter is a museum: seven Lit templates with `@Id` injection, a presenter layer, an add on data provider, display DTOs full of preformatted strings, a runtime generator that fabricates 20k orders on every boot, no images, no i18n, seven trivial tests. Second, Vaadin 25.3 ships a large batch of new APIs that are best judged inside a real application rather than in isolated snippets.

Every feature here exists because a real bakery would want it. Where a 25.3 API fits that need, the specification names the API. Where it does not, we do not force it.

## Hard rules

1. Nothing is copied from the old Bakery. It is read for behaviour only. No file, no class, no CSS rule is carried over.
2. No Lit or Polymer templates. Every view is Java. The only client side files are the ones Vaadin generates.
3. Signals first. Cross component state is a signal, not a listener chain and not a mutable field read from three places.
4. Every string a user can read comes from a translation bundle. English and Spanish, switchable at runtime.
5. This is the full featured version and it expects a Vaadin Pro subscription. See Licensing below. The test suite still runs with no licence and no OpenAI key, because neither the browser nor a production build is involved in it.
6. A feature is done when its acceptance criteria are checked, `./mvnw verify` is green, and the test cases named in its feature document exist as real tests.
7. No hacks. Where a platform API leaves no reasonable way to do something, the workaround is written plainly, kept small, and reported so it can be deleted when the platform is fixed. A workaround nobody wrote down is a hack.
8. Every workaround is recorded. A missing API, an awkward one, a bug routed around, or something that turned out to be impossible goes into `specs/FEEDBACK-25.3.md`, or into `specs/FEEDBACK-PLATFORM.md` when it is older than this release. Finding these is half the reason this application exists.

## How the work is committed

The specifications are written once, before any implementation, and they land as one commit. Judging them is easier when they arrive whole: a feature document that appears next to the code that implements it is a description, not a specification.

After that, each epic ends in a single commit carrying everything it changed. The implementation, whatever it had to change in shared code, the shell, the theme, the domain, and its tests, all together. An epic split across three commits, one of which leaves the application broken until the next arrives, is not three readable commits: it is one commit with two gaps in it.

What never travels with an epic is the record of the work rather than the product: the build report, the findings for the Vaadin teams, the note for whoever opens the next session. Those are written as things are discovered and committed on their own at the end, so that reading the history of the application is not the same as reading the diary of building it.

## Licensing

This is the full featured version and it expects a Vaadin Pro subscription. GridPro, the AI extensions and Observability Kit are used wherever they are the right tool for the job.

No code here checks a licence, degrades, hides a feature or offers a substitute for one. Vaadin enforces its own terms and does it better than we could: a production frontend build without a licence fails, and development marks what is unlicensed. Our own check would be a second, worse copy of that, in a place with no API to do it properly, and it would double the paths a reader has to hold in their head to understand one screen.

A core only version is contemplated, and when it is written it will be **a separate branch that replaces classes**, not a profile in this one. That keeps both versions readable: each branch is a whole application that compiles, rather than one codebase with a source root, a factory and an interface for every commercial component. It is not written yet, and nothing in this branch is shaped in advance to accommodate it.

What this rules out, so it is not proposed again: licence probes at runtime, a `commercial` Maven profile in this branch, free fallbacks standing next to commercial components, and any conditional that asks what the machine is allowed to run.

## Actors

| Actor | Authenticated | What they do |
| --- | --- | --- |
| Visitor | no | Browses the catalogue, reads product pages, fills a cart, orders as a guest |
| Customer | no account, holds a tracking link | Follows the order, talks to the bakery, cancels while it is still new, reorders, prints the invoice |
| Barista | role `BARISTA` | Takes counter and phone orders, edits them, moves them through their states, answers customers |
| Baker | role `BAKER` | Works the kitchen board, claims tickets, marks them ready, watches today at a glance |
| Admin | role `ADMIN` | Manages catalogue, users, locations, closures and invoices, and reads the metrics |

## Phases and epics

Phase 1 is a complete application on its own and fully replaces the old Bakery. Phase 2 epics are independent of each other and can be dropped from the bottom of the priority order without breaking anything.

| Phase | Epic | Size |
| --- | --- | --- |
| 1 | 01 Foundation and build | M |
| 1 | 02 Domain, persistence and dataset | M |
| 1 | 03 Public storefront and catalogue | L |
| 1 | 04 Cart and pickup slots | M |
| 1 | 05 Checkout and order lifecycle | L |
| 1 | 06 Staff order board | L |
| 1 | 07 Admin CRUD | L |
| 2 | 08 Kitchen board and live collaboration | M |
| 2 | 09 Order conversation and attachments | M |
| 2 | 10 Invoicing and printable documents | M |
| 2 | 11 Dashboard and analytics | M |
| 2 | 12 AI | L |
| 2 | 13 Observability and platform events | M |
| 2 | 14 Tests, i18n sweep, docs and demo polish | M |

If time runs out, cut phase 2 from the bottom of this order: 12, 08, 11, 09, 13, 10, 14.

## Use cases

These are the acceptance tests, written in the language of the actor. Each one is expanded in the feature document that owns it.

### Visitor

| # | Use case |
| --- | --- |
| V1 | Browse the storefront: product cards with photo, price and allergen chips |
| V2 | Search products by name and filter by category |
| V3 | Exclude allergens with a multi select and watch the catalogue narrow |
| V4 | Open a product page and read its markdown description |
| V5 | Add a product to the cart from a card or from the product page |
| V6 | See a cart badge with the item count and the running total on every view |
| V7 | Change quantities and remove lines in the cart |
| V8 | Choose a pickup location |
| V9 | Choose a pickup date, where closed weekdays, holidays and full days are disabled with a visible reason |
| V10 | See the remaining capacity of each day inside the calendar |
| V11 | Choose a pickup time, prefilled with the next free slot |
| V12 | Enter contact details and place the order as a guest |
| V13 | Land on a confirmation page with the order reference and a tracking link |
| V14 | Switch the interface between English and Spanish without losing the cart |
| V15 | Install the application and see a branded offline page when the network drops |
| V16 | Read opening hours and the closure calendar |
| V17 | Log in as staff |

### Customer

| # | Use case |
| --- | --- |
| C1 | Watch the order state change live, without reloading |
| C2 | Message the bakery about the order |
| C3 | Attach a reference photo, by upload or by pasting an image from the clipboard |
| C4 | Cancel the order while it is still new |
| C5 | Reorder the same basket in one click |
| C6 | Open and print the invoice |

### Barista

| # | Use case |
| --- | --- |
| B1 | See today's board grouped by pickup slot |
| B2 | Take a phone order by pasting what the customer said and letting the assistant fill the form |
| B3 | See which fields the assistant filled, with source and confidence, and override any of them |
| B4 | Find an existing customer by typing part of a name, an email or a phone number |
| B5 | Create a customer inline when there is no match |
| B6 | Confirm an order and see the remaining capacity of the chosen slot |
| B7 | Mark an order picked up, which issues the invoice |
| B8 | Mark an invoice as paid |
| B9 | Flag an order as a problem, with the reason recorded in the history |
| B10 | Reply to a customer message |
| B11 | Open the printable invoice |

### Baker

| # | Use case |
| --- | --- |
| K1 | See the kitchen board for today and tomorrow, grouped by slot, updating live for everyone |
| K2 | Claim a ticket or assign it to another baker |
| K3 | Advance a ticket and watch it move on every other screen instantly |
| K4 | Expand a row to read item comments and allergen warnings without changing the selection |
| K5 | See a production summary: units per product for a chosen day |
| K6 | Adjust today's stock per product inline in the grid |
| K7 | Get nudged when a ticket has sat in preparation too long |
| K8 | Ask the assistant which orders on screen are at risk of being late |

### Admin

| # | Use case |
| --- | --- |
| A1 | Manage products, including price, VAT rate, category and allergens |
| A2 | Upload a product photo by drag and drop, by button, or by pasting from the clipboard |
| A3 | Toggle availability and featured with a switch, in the grid and in the editor |
| A4 | Write a markdown description and see a live preview |
| A5 | Manage categories and allergens |
| A6 | Manage pickup locations with slot length and capacity |
| A7 | Declare a closure and see the storefront date picker reflect it |
| A8 | Manage staff users, assign roles, lock and unlock accounts |
| A9 | Browse all orders with a column chooser and multi column sort |
| A10 | Select many orders and confirm or cancel them in bulk |
| A11 | Search invoices, mark them paid, export the result as CSV |
| A12 | Read the dashboard: revenue by day, orders by state, top products, slot utilisation |
| A13 | Ask the assistant a question about the dashboard data and get a chart back |
| A14 | Open a diagnostics view showing session lock contention, RPC traffic and data provider queries |
| A15 | Read Prometheus metrics, Interaction Insights, UI state size, per view JDBC and Web Vitals |
| A16 | Switch between light and dark, and between Lumo and Aura |
| A17 | Open an about page listing the running version and every enabled 25.3 feature |

## Glossary

| Term | Meaning |
| --- | --- |
| Order | A basket of products, promised for a date, a time and a pickup location, owned by a customer |
| Order state | `NEW`, `CONFIRMED`, `IN_PREPARATION`, `READY`, `PICKED_UP`, `PROBLEM`, `CANCELLED` |
| Slot | A pickup location plus a date plus a time, with a capacity in orders. Computed, never stored |
| Lead time | Days a product needs between ordering and pickup. The slowest product in a cart sets the earliest date |
| Closure | A date on which a location does not serve, either a holiday or maintenance |
| Invoice | An immutable snapshot of a picked up order, numbered per year, with VAT broken out |
| Channel | Where an order came from: `ONLINE`, `PHONE` or `COUNTER` |

## What we deliberately leave out

Ingredients and stock depletion, staff shifts, delivery and routing, payment gateways, customer accounts with login, loyalty and coupons, multi tenancy. Each is a large modelling exercise that adds almost no Vaadin 25.3 surface. See the rejection table in `01-domain-model.md` for the reasoning per item.

Also out of scope: Tailwind (it competes with the Aura work in epic 12), Spreadsheet, Rich Text Editor, the CRUD component, Collaboration Kit (shared signals cover it natively and cheaper), Hilla, and native image builds.

## 25.3 feature coverage

Every feature lands in exactly one epic. If an epic is cut, this table says what coverage is lost.

| 25.3 feature | Epic |
| --- | --- |
| Aura theme properties, light and dark | 01 |
| URL scheme validation, X-Frame-Options | 01 |
| Coding agent dev loop, `vaadin:install-dev-cli`, `flow-devloop-daemon` | 01 |
| `Element.whenAttached` and `Component.whenAttached` | 01 |
| JSR 303 validation groups in `BeanValidationBinder` | 05, declared in 02 |
| Markdown component with signal binding | 03 |
| Html sanitization with a jsoup Safelist | 03 |
| `Image` as `HtmlComponent` | 03 |
| MultiSelectComboBox change event semantics | 03 |
| PWA | 03 |
| DatePicker disabled dates and weekdays | 04 |
| Date Metadata Provider and `refreshDateMetadata` | 04 |
| DateTimePicker default time | 04 |
| Breadcrumbs, `@RouteParent`, `@DynamicPageTitle` | 05 |
| `UI.triggerAfter` | 05 |
| Grid hidden columns skip data | 06 |
| Grid i18n API | 06 |
| Row details decoupled from `activeItem` | 06 |
| Switch | 07 |
| GridPro inline editing | 07 |
| Modular Upload: `UploadManager`, `UploadButton`, `UploadDropZone`, `UploadFileList` | 07 |
| Clipboard paste of files and images | 07 |
| ComboBox partial match mode | 07 |
| Table, TableRow, TableCell with `bindChildren` | 08, 10 |
| Shared signals with Jackson `TypeReference` | 08 |
| MessageList bubble and one to one variants, attachments | 09 |
| Charts with a free fallback | 11 |
| AI orchestrator, request interception, response metadata, `ToolException`, background execution, per turn session context | 12 |
| Form AI controller, field marker, source tracking, confidence | 12 |
| Grid AI controller, orchestrator reading grid state | 12 |
| VaadinService event bus: session lock, RPC, data provider count and fetch queries | 13 |
| `UI.getLastUpdateSentTimestamp`, undelivered invocation warnings | 13 |
| Observability Kit 5, Interaction Insights, UI state size, per view JDBC, Web Vitals | 13 |
| Browserless multi user and multi window testing | 14 |

What of that list did not land, as of this pass:

| Feature | State |
| --- | --- |
| `bindChildren` | Does not exist in 25.3.0-beta1, whatever the documentation says. `base/signals/Children.java` does the same job with `Signal.effect`. See `FEEDBACK-25.3.md` |
| Charts with a free fallback | Only the fallback is built. Charts is not a dependency and no panel draws one |
| Form AI controller, field marker, source tracking, confidence | Not built. `vaadin-ai-extensions-flow` is a dependency that nothing imports |
| Grid AI controller | Not built |
| `ToolException` and background execution | Unused. The orchestrator, the interceptor and the response metadata are all exercised |
| Clipboard paste | The component is wired into the product editor and the conversation. Proving it needs a real paste event, so it waits on `ClipboardPasteIT` |
| Observability Kit 5 | The profile exists and nothing runs it, in the suite or in CI. The diagnostics view is fed by the free service event bus instead, which is the more interesting half |
| X-Frame-Options and URL scheme validation | Never configured explicitly. Whatever Spring Security defaults to is what this application does |
| MessageList attachments | The uploads are built and untested |

Everything else on the list is in the application and covered. The one arrival since the table was written is the live assistant: `SpringAILLMProvider` ships inside `vaadin-ai-core-flow`, which is free, and epic 12 now wires it.
| Copilot Kotlin support | 14, the about view is written in Kotlin |
| Copilot Test Recorder, FormLayout editor, All Components view | 14, documented in the demo script |
