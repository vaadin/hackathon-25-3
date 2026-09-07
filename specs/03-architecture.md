# 03 Architecture

## Package layout

Feature packages, not layer packages. A feature owns its entity, its repository, its service, its views and its tests, and everything about it is in one folder.

```
com.vaadin.bakery
  Application.java              @SpringBootApplication, @StyleSheet, @PWA
  base
    ui                          MainLayout, ViewToolbar, EmptyState, Money renderers
    signals                     SignalSupport helpers, session scoped signal beans
    error                       DomainException hierarchy and the global error handler
    i18n                        TranslationProvider, LanguageSelector
    security                    SecurityConfiguration, CurrentUser, roles
  catalogue                     Category, Allergen, Product, ProductImage + repos + services
    ui                          storefront, product page, admin product views
  ordering                      Order, OrderItem, history, state machine, SlotService
    ui                          cart, checkout steps, staff order board, kitchen board
  billing                       Invoice, InvoiceLine, numbering, print view
  people                        Customer, User and their admin views
  assistant                     LLMProvider wiring, the guarded database, AI panels
  diagnostics                   event bus listeners, observability config, admin views
```

Every package carries a `package-info.java` with `@NonNullApi`.

## Layers inside a feature

| Layer | Rule |
| --- | --- |
| Entity | JPA plus Jakarta validation. No Vaadin imports, ever |
| Repository | Spring Data, JPA Specifications for anything dynamic. No add on data provider |
| Service | The only place a transaction starts. Returns entities to editors and records to read only views |
| View | Vaadin, Java only. Never touches a repository |

Errors travel as a small `DomainException` hierarchy (`NotFound`, `Conflict`, `RuleViolation`), each carrying a translation key. One handler renders them, which replaces the hand rolled presenter error mapping of the old app.

## Views

- `@Route` plus `@Menu` plus `@PageTitle`. `MainLayout` is annotated `@Layout`, so no view names it. Navigation is built from `MenuConfiguration`.
- Route hierarchies use `@RouteParent` and `@DynamicPageTitle`, which is what makes the Breadcrumbs component work without hand written trails.
- Constructor injection everywhere. No field injection in views.
- Views never format money or dates by hand. `Money` and the formatters in `base.ui` do it, locale aware.
- View local models are records next to the view.

## Signals

Signals are the default way state moves between components. The rules:

| Rule | Why |
| --- | --- |
| Cross view state lives in a `@VaadinSessionScope` bean holding signals | The cart is the canonical case: header badge, cart page and checkout all read the same source |
| Derived values are `Signal.computed`, never a listener that writes a field | One source of truth, and no ordering bugs |
| `bindText`, `bindValue`, `bindEnabled`, `bindVisible`, `bindClassName` and `getStyle().bind` are preferred over manual updates | Less code and no stale UI |
| A list of components comes from `bindChildren` over a `ListSignal` | Handles insert, remove and reorder without rebuilding everything |
| A field feeding a signal sets `ValueChangeMode.EAGER` | Otherwise the signal lags a keystroke behind |
| Grid has no `bindItems`. Use `Signal.effect(grid, () -> grid.setItems(...))` | The documented idiom |
| Anything shared across sessions is a shared signal, subscribed in `whenAttached` and released by the returned registration | Leaks otherwise, and `whenAttached` is the 25.3 way to avoid overriding `onAttach` |

Responsive layout comes from `Page.windowSizeSignal()`, not from media queries in Java. Dark mode is `ui.getElement().getThemeList().bind("dark", darkSignal)`.

## Data access and grids

- Lazy loading through `grid.setItemsPageable(service::page)` or a `Specification` based provider. No `FilterablePageableDataProvider` add on.
- Filters are signals. One `Signal.computed` builds the `Specification`, one `Signal.effect` pushes it into the grid.
- Sorting maps `QuerySortOrder` to Spring `Sort` in one shared helper.
- Grids declare hidden columns for the column chooser. In 25.3 a hidden column runs no value provider and fetches no data, and the diagnostics view of epic 13 proves it with a live query counter.

## Forms

- `BeanValidationBinder` with `bindInstanceFields`, field names matching property names.
- Validation groups drive the difference between saving a draft and submitting, as defined in `01-domain-model.md`.
- A form never writes to the entity until it validates. Services validate again, because a browser is not a trust boundary.

## What we are not doing

| Not doing | Instead |
| --- | --- |
| Lit or Polymer templates | Java views |
| Presenter classes per view | Services plus signals |
| Display DTOs with preformatted strings | Records plus renderers that format at the edge |
| A generic CRUD abstraction over `vaadin-crud` | Plain views. The three admin screens differ enough that the abstraction cost more than it saved |
| Runtime data generation | A static dataset, see `02-data-set.md` |
| Hard coded `Locale.US` | Locale from the user, the session or the browser, see `05-theming.md` and the i18n rules below |

## Internationalisation

- Bundles under `src/main/resources/vaadin-i18n/`: `translations.properties` as the fallback, `translations_en.properties` and `translations_es.properties`.
- Keys are dotted and namespaced by feature: `catalogue.product.price`, `ordering.state.IN_PREPARATION`.
- No literal user visible string in Java, annotation values included. A code review that finds one rejects the change.
- Enum display names resolve through the bundle, so `OrderState.IN_PREPARATION` reads "In preparation" or "En preparacion".
- The language selector lives in the shell. Changing the language retranslates what is already on screen, where it stands: nothing navigates, nothing reloads, and nothing already started is lost, the cart included. What counts as visible, and the two kinds of text that deliberately do not follow the change, are specified in `features/01-foundation.md`.
- A screen that is correct only when it is built after the language changed is not correct. The behaviour to hold is the one a reader sees when they switch while looking at it.
- Dates, times and money are formatted in the active language. The one document that does not follow it is the invoice, which keeps the language it was issued in, see `features/10-invoicing.md`.
