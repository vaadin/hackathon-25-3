REPO: vaadin/flow
TITLE: Three deprecations that do not name a replacement

---
### Description

**`LoginI18n.createDefault()`**, deprecated for removal. The replacement path is to build `Header`, `Form` and `ErrorMessage` by hand, and the default English texts that `createDefault` provided are then gone, so every application re-supplies all of them even when it wanted to change one string.

**`VaadinAwareSecurityContextHolderStrategyConfiguration`**, deprecated for removal. The javadoc does not say what replaces it, and every existing Bakery style example still imports it. We dropped the import and the application works, which means we guessed that the starter now configures it.

**156 `VaadinIcon` constants**, including `MOON_O`. No mapping from a deprecated icon to its replacement is offered in the deprecation message. We guessed `MOON`.

Counted rather than estimated: 156 of the 656 constants in the enum carry the `Deprecated` attribute, which is one in four.

### Why it matters

A deprecation without a replacement turns into a guess, and a guess in a security configuration is the wrong place for one.

### Expected

Name the replacement in the message, and for the icons publish a table of old to new. That last one is mechanical and 156 lines long.

### How this was checked

Against the 25.3.0-beta1 artifacts, so the annotations are the shipped ones:

```
javap -v -cp vaadin-icons-flow-25.3.0-beta1.jar com.vaadin.flow.component.icon.VaadinIcon | grep -c "Deprecated: true"
156
javap -v -cp vaadin-login-flow-25.3.0-beta1.jar com.vaadin.flow.component.login.LoginI18n
  createDefault carries java.lang.Deprecated
javap -v -cp vaadin-spring-25.3.0-beta1.jar \
    com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration
  java.lang.Deprecated(since="25.0", forRemoval=true)
```

Found on 25.3.0-beta1.
