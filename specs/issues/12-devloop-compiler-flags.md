REPO: vaadin/flow
TITLE: The dev loop daemon compiles without the project's compiler flags

---
### Description

The daemon compiles through `javax.tools.JavaCompiler` with its own options. `spring-boot-starter-parent` sets `-parameters`, and the daemon does not, so every class it writes into `target/classes` loses its parameter names.

Two ways to meet it.

A session, then `./mvnw verify` without `clean`: Spring Data tests fail with

```
For queries with named parameters you need to provide names for method parameters;
Use @Param for query method parameters, or when on Java 8+ use the javac flag -parameters
```

Or edit `pom.xml`. The daemon reads that as a classpath change and recompiles the whole module, and then the running application breaks: every `@Query` method with a named parameter throws the same error, so any view that uses one fails to open. The `apply` was green.

### Why it matters

The failure names Spring Data and points at repository code nobody has touched, so the first guess is a flaky test, or a broken view.

The obvious fix does not work either: `./mvnw compile` compiles nothing, because the daemon's class files are newer than the sources and Maven considers them up to date. It takes `clean`.

### Expected

Read the compiler plugin's configuration, or compile into a directory of the daemon's own so a Maven build always wins. Either one removes both halves.

Until then, a warning when a pom edit triggers a full recompile would help.

### Reproduce

Any Spring Boot project with a repository method like

```java
@Query("select o from Order o where o.reference = :reference")
Optional<Order> findByReference(String reference);
```

1. `.vaadin/vaadin-dev start`
2. Touch `pom.xml`, then `.vaadin/vaadin-dev apply`
3. Open a view that calls that method

Recovery: `stop`, `./mvnw clean compile`, `start`.

Found on 25.3.0-beta1.
