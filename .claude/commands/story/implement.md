# Story implementation

## Before writing anything

- Read the feature document and the story. The acceptance criteria are the contract.
- Query the Vaadin MCP for every 25.3 API you are about to use, with `ui_language: "java"` and `vaadin_version: "25.3"`. Do not write from memory, this is a beta.
- Check `specs/CHANGELOG-RISK.md` if the API is preview. Use the adapter, add the fallback.

## Package layout

Feature packages under `com.vaadin.bakery`. A feature owns its entity, repository, service, views and tests. Every package has a `package-info.java` with `@NonNullApi`.

## Rules that get code rejected

- A Lit or Polymer template.
- Anything copied from the old Bakery.
- A user visible string literal in Java.
- A view calling a repository.
- An `onAttach` or `onDetach` override where `whenAttached` would do.
- A `Thread.sleep` in a test.
- CSS outside `src/main/resources/META-INF/resources`.
- A commercial API used without a free fallback.

## Patterns to follow

- Constructor injection. No field injection in views.
- `BeanValidationBinder` with `bindInstanceFields`, validation groups per `01-domain-model.md`.
- Cross view state in a `@VaadinSessionScope` bean holding signals.
- Derived values with `Signal.computed`, lists with `bindChildren`, grids with `Signal.effect(grid, () -> grid.setItems(...))`.
- Fields that feed a signal use `ValueChangeMode.EAGER`.
- Records for read models. Never a display DTO with preformatted strings.
- Money through the `Money` type, formatted at the edge with the active locale.

## Gate

`./mvnw compile` and `./mvnw test`. Red build means stop and fix, not continue and hope.
