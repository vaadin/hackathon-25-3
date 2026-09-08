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

Found on 25.3.0-beta1.
