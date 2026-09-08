REPO: vaadin/flow
TITLE: A PageTitleGenerator annotated @Component silently becomes the whole application's generator
---
### Description

`@DynamicPageTitle(MyGenerator.class)` on one view registers that generator for that view. Annotating the same class `@Component` as well, which any Spring developer does without thinking, registers it **application wide** and overrides `@PageTitle` on every route.

Nothing warns, and the two registrations look independent.

### Reproduction

```java
@Component
public class ProductPageTitle implements PageTitleGenerator {
    @Override
    public String generatePageTitle(PageTitleContext context) {
        return context.routeParameters().get("slug")
                .flatMap(catalogue::bySlug).map(Product::getName)
                .orElse("We cannot find that product");
    }
}
```

Every page in the application is then named by the `orElse` branch: the browser tab, and every label in every router driven `Breadcrumbs`, which resolves titles the same way.

### Why it matters

It is invisible to a server side test suite. Ours had 224 green tests resolving the right titles while every page in the browser was called by the fallback.

### Expected

A warning when a generator bean overrides an explicit `@PageTitle`, or a scope on the annotation.

### Workaround

Do not make the generator a bean. `@DynamicPageTitle` instantiates it through the Spring aware instantiator, constructor injection included.

### Getting the project

[`08-pagetitlegenerator-bean.zip`](https://github.com/vaadin/hackathon-25-3/raw/5c2b74faff32ae05c01b235ca421a1c1c486bd12/specs/issues/projects/08-pagetitlegenerator-bean.zip), 4 KB, sources only: two routes, one with its own `@PageTitle`, and a generator annotated `@Component`.

```
mvn spring-boot:run
```

Then read the browser tab on `http://localhost:8098/`. It carries finding `43` as well, on the product route.

Found on 25.3.0-beta1.
