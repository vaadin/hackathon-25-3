REPO: vaadin/docs
TITLE: The container background tokens are not interchangeable between Lumo and Aura

---
### Description

The shared token layer suggests the same names work in both themes. For the container backgrounds they do not: a panel styled with one theme's token is invisible or wrong in the other, because the two themes give those names different roles.

We found it by styling a panel that looked right in Lumo and disappeared in Aura, and ended up choosing colours that work in both rather than trusting the names.

### Why it matters

Choosing a theme at runtime is a supported thing to do, and an application that offers both themes cannot use a token whose meaning changes with the theme. The failure is visual and silent: nothing warns, and the panel is simply the wrong colour in one of them.

### Expected

Say which tokens are safe across themes and which are not, on the page that lists them. A short table with the two themes side by side would do it.

### Reproduce

Style any container with a container background token, then switch the theme at runtime between Lumo and Aura and look at it.

### How this was checked

Reading the shipped stylesheet rather than guessing, `/aura/aura.css` as the running application serves it:

```
--vaadin-background-color: var(--aura-surface-color-solid)
--vaadin-background-container: color-mix(in srgb, var(--_color-base) ...)
--vaadin-background-container-strong: color-mix(in srgb, var(--_color-base) ...)
```

That is the difference in one line: the plain background token is a colour, and the container tokens are a mix computed from a private base that the surrounding context sets. Substituting one for the other looks like a rename and is not one.

Found on 25.3.0-beta1.
