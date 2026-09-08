REPO: vaadin/flow
TITLE: MenuConfiguration.getPageHeader calls a title generator with no route parameters

---
### Description

A view with `@DynamicPageTitle`, a generator that reads a route parameter, and a shell that binds its header to `MenuConfiguration.getPageHeader(view)`.

The browser tab is right, because the router resolves the title with the parameters. The header is wrong, because `getPageHeader` calls the same generator with a `PageTitleContext` whose `routeParameters()` is empty.

### Why it matters

The two titles come from one generator and disagree, and nothing suggests why. Ours reads a product slug, so a product page printed "we cannot find that product" across the top of the product it was showing.

### Expected

Pass the active route parameters to the generator, the way the router does.

### Workaround

Fall back to the parameter in `UI.getCurrent().getInternals().getActiveViewLocation()` when the context has none, and delete that the day the context is complete.

### Reproduce

`08-pagetitlegenerator-bean/` in this directory carries it: a `product/:slug` route with `@DynamicPageTitle`, a generator that reads the slug, and a `Shell` whose navbar prints `MenuConfiguration.getPageHeader(getContent())` after every navigation.

`mvn spring-boot:run`, then open `http://localhost:8098/product/sourdough`:

| Where | What it says |
| --- | --- |
| Browser tab | `Product sourdough` |
| Navbar header | `We cannot find that product` |

One generator, one page, two answers. The second one is the generator's fallback for a slug it did not receive.

That project also carries finding `08`, the generator being applied to every route because it is a `@Component`. The two are independent: this one shows on the product route, that one on `/`.

Found on 25.3.0-beta1.
