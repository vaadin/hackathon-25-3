REPO: vaadin/flow
TITLE: CSS files pulled in with @import from a stylesheet are blocked by the default security setup

---
### Description

`@StyleSheet("styles.css")` is served. The files that stylesheet `@import`s are ordinary browser requests for static resources, and `VaadinSecurityConfigurer` does not permit them, so each one redirects to the login view, which redirects again, and the browser gives up:

```
ERR_TOO_MANY_REDIRECTS
```

The page then renders with the main stylesheet and none of its imports.

### Why it matters

Splitting CSS into files and pulling them together with `@import` from one entry point is ordinary, and it is what the Vaadin documentation suggests for organising styles. In a secured application it silently produces an unstyled page.

The failure names redirects, so the first guess is the security configuration for the view, not the stylesheet.

### Expected

Permit static CSS under the resource paths the framework already serves, the way `styles.css` itself is permitted, or say on the styling page that imported files need a matcher of their own.

### Workaround

Permit the directory explicitly:

```java
.requestMatchers("/styles/**").permitAll()
```

### Reproduce

[`39-imported-css/`](https://github.com/vaadin/hackathon-25-3/tree/9cf6235d4bf170002c44ac69c0d51063376828ba/specs/issues/39-imported-css): a secured application with one user in memory, `@StyleSheet("styles/main.css")` on an anonymous view, and `main.css` importing `parts.css`. The security setup permits `/styles/main.css` and nothing else, which is the state an application reaches after somebody notices the declared sheet redirecting and adds a matcher for it.

`mvn spring-boot:run`, then ask the server:

```
curl -o /dev/null -w "%{http_code} -> %{redirect_url}" http://localhost:8139/styles/main.css
200
curl -o /dev/null -w "%{http_code} -> %{redirect_url}" http://localhost:8139/styles/parts.css
302 -> http://localhost:8139/login
```

Open `http://localhost:8139/`. `main.css` applied, its import did not: the body font is `sans-serif` from `main.css`, and the heading is the default colour instead of the pink `parts.css` sets.

The console is the part worth fixing, whatever happens to the rest. It names the file that worked:

```
Failed to load resource: net::ERR_TOO_MANY_REDIRECTS   http://localhost:8139/login
Error loading http://localhost:8139/styles/main.css
'http://localhost:8139/styles/main.css' could not be loaded.
```

Comment the matcher out and the declared sheet fails the same way, which is the general form: with the default `VaadinSecurityConfigurer` no static CSS under `META-INF/resources` is permitted, entry point included.

### Getting the project

```
git clone --branch manolo --depth 1 https://github.com/vaadin/hackathon-25-3
cd hackathon-25-3/specs/issues/39-imported-css
mvn spring-boot:run
```

Found on 25.3.0-beta1.
