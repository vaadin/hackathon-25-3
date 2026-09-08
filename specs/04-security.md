# 04 Security

## Model

Staff authenticate with email and password. Customers never authenticate: they hold a tracking link containing an order reference. Everything public is explicitly annotated as such, and everything else denies by default.

| Role | Granted authority | Intent |
| --- | --- | --- |
| `ADMIN` | `ROLE_ADMIN` | Everything, including reopening a picked up order and reading diagnostics |
| `BAKER` | `ROLE_BAKER` | Kitchen board, the states an order passes through while it is being baked, stock |
| `BARISTA` | `ROLE_BARISTA` | Orders taken by telephone, by email or at the counter, customers, invoices, every order state |

`Role` is an enum. The old app stored a free string, which meant a typo created a role nobody had.

## Route access

| Route | Access |
| --- | --- |
| `/`, `/shop/**` | `@AnonymousAllowed` |
| `/cart`, `/checkout/**` | `@AnonymousAllowed` |
| `/track/{reference}` | `@AnonymousAllowed`, guarded by the reference itself |
| `/login` | `@AnonymousAllowed` |
| `/about` | `@AnonymousAllowed` |
| `/orders/**` | `@RolesAllowed({ADMIN, BARISTA, BAKER})` |
| `/kitchen` | `@RolesAllowed({ADMIN, BAKER})` |
| `/admin/products/**`, `/admin/categories`, `/admin/locations`, `/admin/closures` | `@RolesAllowed({ADMIN})` |
| `/admin/users/**` | `@RolesAllowed({ADMIN})` |
| `/admin/invoices/**` | `@RolesAllowed({ADMIN, BARISTA})` |
| `/admin/dashboard` | `@RolesAllowed({ADMIN, BARISTA})` |
| `/admin/diagnostics` | `@RolesAllowed({ADMIN})` |

The menu hides what the current user cannot reach, using `AccessAnnotationChecker`. Hiding is a courtesy, not a control: the route annotation is the control.

## Action level rules

Route access is not enough. These are enforced in the service layer and tested there.

| Action | Rule |
| --- | --- |
| Cancel an order | Customer only while `NEW`. `BARISTA` any time before `READY`. `ADMIN` always. A `BAKER` never cancels: see the transition table below |
| Reopen a picked up order | `ADMIN` only, and it voids the invoice |
| Edit or delete a locked user | Refused for everyone, including admin, until the user is unlocked |
| Delete yourself | Refused |
| Delete a product with orders | Refused, offer to mark it unavailable |
| Change a price | Never rewrites existing order lines, which carry a price snapshot |
| Void an invoice | `ADMIN` only, and it requires a reason recorded in the order history |
| Read another customer's order | Impossible: the tracking route resolves only by reference and never lists |

### Which role may move an order to which state

A baker bakes. Everything a baker does to an order is something that happens between accepting it and having it ready on the shelf, so those are the only states a baker may set. Handing the order over is not baking: it is the moment the bakery takes the money, because marking an order picked up issues its invoice. Cancelling is a commercial decision about a customer, not a judgement about an oven.

A barista may move an order anywhere it can legally go, including the states the kitchen normally drives. This is deliberate: the counter is where somebody notices that an order nobody moved is late, and the person who notices should be able to unstick it rather than go looking for a baker.

| Target state | `ADMIN` | `BARISTA` | `BAKER` |
| --- | :-: | :-: | :-: |
| `CONFIRMED` | yes | yes | yes |
| `IN_PREPARATION` | yes | yes | yes |
| `READY` | yes | yes | yes |
| `PROBLEM` | yes | yes | yes |
| `PICKED_UP` | yes | yes | no |
| `CANCELLED` | yes | before `READY` | no |

A baker keeps `PROBLEM` because a problem with an order is usually something only the person at the oven can see, and taking it away would leave them with no way to say so.

This is a rule about what a role may do, not about which screen it is on, so it is enforced in the service that changes the state. A screen may only ever offer less than the rule allows, never more: the kitchen board offers a baker no way to hand an order over, and the order panel offers a baker no way to cancel one.

## The tracking link

`/track/{reference}` is the only anonymous view of private data, so it gets its own rules.

- The reference is `ORD-2026-000123`. The sequential part alone is guessable, so the URL carries an additional opaque token stored on the order and generated with a `SecureRandom`: `/track/ORD-2026-000123?t=<token>`.
- A wrong or missing token renders the same not found page as a wrong reference. No oracle.
- The page shows state, items, slot and messages. It never shows the internal note, the customer's other orders, or staff names beyond a first name.
- Rate limited per IP at the filter level to make enumeration pointless.

## Input handling

| Input | Treatment |
| --- | --- |
| Customer note and messages | Sanitized with a jsoup `Safelist` before rendering. The Safelist is a single shared constant, no per view variation |
| Markdown product descriptions | Written by admins, still rendered through the sanitizing pipeline |
| Uploaded images | Content type and magic bytes checked, size capped at 2 MB, stored as bytes and served from the application, never from the upload path |
| Any URL that reaches `Anchor`, `Page.open`, `SideNavItem` or a breadcrumb | Passed through the 25.3 scheme validation. The safe scheme list is configured once and documented here: `http`, `https`, `mailto`, `tel` |

## Headers and transport

- `X-Frame-Options` is sent by default in 25.3 and stays on. The application is never framed.
- CSRF is Vaadin's own for UIDL, Spring Security's for anything else.
- Session fixation protection on login, sessions invalidated on logout.
- Passwords are BCrypt with the default strength. The demo passwords are weak on purpose and the README says so.
- Actuator endpoints are exposed only under the `observability` profile, and `/actuator/**` requires `ROLE_ADMIN` except for `health`.

## Tests

Every rule in the two tables above has a browserless test that asserts the refusal, not just the happy path. `SecurityRulesTest` walks the route table with each role and asserts the outcome, so adding a route without an annotation fails the build.
