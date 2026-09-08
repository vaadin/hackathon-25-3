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

`specs/issues/08-pagetitlegenerator-bean/` has the shape already: a route with a parameter and a generator. Bind a header to `getPageHeader` and compare it with the browser tab.

Found on 25.3.0-beta1.
