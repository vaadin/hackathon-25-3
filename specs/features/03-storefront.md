# Feature 03: Public storefront and catalogue

## Overview

The anonymous shopping surface: a landing page, a catalogue with filters, and a product page. It is the first impression of the demo, so it has to look like a bakery and not like a data grid.

Covers V1 to V4, V14, V15, V16 and A4's preview.

## Behaviour

### Landing page, route `/`

Hero with the bakery name and today's opening hours, a row of featured products, and the category strip. Everything anonymous.

### Catalogue, route `/shop` and `/shop/{categorySlug}`

- A responsive card grid, rendered with `bindChildren` over a `ListSignal<ProductCard>`. Adding a filter never rebuilds the whole grid.
- Each card: photo with a blurred placeholder while it loads, name, price, allergen chips, an add to cart button, and a badge when the product needs lead time ("order two days ahead").
- The photo opens the product as well as the name does, since it is the largest thing on the card and the thing a visitor points at. It is a second link to the same route, out of the tab order and hidden from assistive technology so the name stays the one announced way in, and the lead time badge sits outside it because that badge is content. The add to cart button is outside it too, so adding never navigates.
- Filters, all signals, all reflected in the URL as query parameters so a filtered catalogue is shareable: text search, category, and allergen exclusion.
- Allergen exclusion is a `MultiSelectComboBox`. In 25.3 its value synchronises on the change event, so the catalogue refilters once per user action and not once per chip.
- Text search is a debounced text field feeding a signal. No search button.
- Sorting: relevance, price ascending, price descending, name.
- The four filters are a `FormLayout` in responsive steps mode, with three states measured on the bar's own width rather than the window's, so the drawer does not shift them. From 52em: four columns, one row. From 34em: two columns, so a tidy two by two. Below that: one column with the labels beside their fields, which is what keeps four filters from pushing the catalogue off a phone screen. Four filters divide evenly into four, two and one, so no state strands a field on a row of its own and none of them needs a colspan. A three column state does not have that property, and the colspan that would have fixed it cannot vary between states: see the colspan row in `FEEDBACK-25.3.md`. Each label belongs to its form item and not to its field, because that is what can move to the side.
- The photographs in the catalogue grid sit at 80 percent opacity, so the names, prices and add buttons carry the page. The same card renders the home page picks at full opacity, where the photograph is the point.
- Empty state: an illustration, the active filters as removable chips, and a clear all action.

### Product page, route `/shop/product/{slug}`

- Photo, name, price, VAT note, allergen chips with full names, availability by weekday, lead time.
- The description is Markdown, rendered by the Markdown component bound to a `Signal<String>` so the admin preview and the public page share one code path.
- Related products from the same category.
- Add to cart with a quantity stepper.
- The page is a child of the catalogue and opens over it in an overlay, so the list, its filters and its scroll position are still there behind it. The route moved under `/shop` for that reason: a child route lives under its parent's path. See `15-ui-repair.md`.
- `@DynamicPageTitle` puts the product name in the tab title.
- Any external link in a description goes through the 25.3 URL scheme validation. A `javascript:` link never renders as a link.

### Images

`Image` is no longer an `HtmlContainer` in 25.3, so nothing nests inside it. The card composes image, badge and overlay as siblings in a positioned wrapper.

Image resolution order: uploaded `ProductImage` from the database, then the seeded file under `/images/products/`, then the category placeholder.

### Sanitization

Product descriptions are written by admins and still pass through the shared jsoup `Safelist` before rendering. There is exactly one Safelist constant in the application.

### PWA

`@PWA` on the shell, generated icons, and a self contained offline page with the opening hours and the phone number. Installability is part of the demo checklist.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A product has no photo | The category placeholder renders, never a broken image |
| A product is unavailable | It disappears from the catalogue immediately and its page shows "not available right now" with a link back |
| Every product is filtered out | The empty state explains which filters are active and offers to clear them |
| A description contains a script tag | Stripped by the Safelist, the rest of the description renders |
| A description contains a `javascript:` link | Rendered as plain text, never as an anchor |
| The visitor is offline | The offline page renders with hours and phone number |
| Two allergens are selected quickly | One refilter, not two, because the value syncs on change |

## Acceptance criteria

### AC1: The catalogue filters correctly
- [x] Searching narrows the grid as the visitor types, without a button
- [x] Choosing a category updates the grid and the URL
- [x] Excluding an allergen removes every product carrying it
- [x] Filters survive a page reload because they are in the URL
- [x] A URL carrying filters shows them in the filter bar, not only in the results. The bindings are two way, so the bar and the signals cannot disagree

### AC2: Cards are correct
- [x] Every card shows photo, name, price and allergen chips
- [x] A product needing lead time shows the lead time badge
- [x] An unavailable product never appears

### AC3: The product page is complete
- [x] Markdown renders as formatted text, not as source
- [x] The tab title carries the product name
- [x] Allergens show their translated full names

### AC4: Untrusted content is safe
- [x] A script tag in a description does not execute and does not render
- [x] A `javascript:` URL does not become a link

### AC5: The application is installable
- [ ] The manifest and the service worker are served
- [ ] With the network off, a full navigation shows the branded offline page

### Still open

- Nothing asserts what a card contains: photo, name, price and allergen chips, or the lead time badge. The catalogue tests count and filter cards by name, which is a weaker claim than the one written here.
- Allergens showing their translated full names on the product page is untested.
- The PWA half is still untested in both directions, and `PwaInstallIT` is one of the six browser tests not written: it needs the network turned off, which is a CDP command rather than an assertion.

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| SHOP-01 | The seeded catalogue | Typing "crois" in the search | Only matching products remain | browserless | `CatalogueBrowserlessTest` |
| SHOP-02 | The full catalogue | Excluding gluten | No product with the gluten allergen remains | browserless | `CatalogueBrowserlessTest` |
| SHOP-03 | Gluten already excluded | Adding lactose in the same combo box | Exactly one refilter happens | browserless | `CatalogueBrowserlessTest` |
| SHOP-04 | A URL with category and search parameters | Opening it directly | The grid shows the same result as after clicking | browserless | `CatalogueUrlStateBrowserlessTest` |
| SHOP-05 | A product with markdown | Opening its page | Headings and lists render as HTML | browserless | `ProductPageBrowserlessTest` |
| SHOP-06 | A description containing a script tag | Opening the page | The tag is absent from the DOM | browserless | `SanitizationTest` |
| SHOP-07 | A description containing a `javascript:` link | Opening the page | No anchor with that href exists | browserless | `SanitizationTest` |
| SHOP-08 | A product marked unavailable | Opening the catalogue | It is absent, and its page shows the unavailable notice | browserless | `CatalogueBrowserlessTest` |
| SHOP-09 | Filters that match nothing | Looking at the grid | The empty state lists the active filters | browserless | `CatalogueBrowserlessTest` |
| SHOP-10 | A running application | Going offline and navigating | The offline page renders | testbench | `PwaInstallIT` |
| SHOP-11 | A card in the catalogue | Looking at the link around its photo | It leads where the title leads, is out of the tab order and is hidden from assistive technology | browserless | `ProductCardContentBrowserlessTest` |
