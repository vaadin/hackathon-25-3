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

[`10-devloop/`](https://github.com/vaadin/hackathon-25-3/tree/9cf6235d4bf170002c44ac69c0d51063376828ba/specs/issues/10-devloop) carries it: a `Ticket` entity, a `Tickets` repository with the query above, and a `/ticket` view that calls it. H2 in memory, no configuration.

1. `mvn flow:install-dev-cli`, then `.vaadin/vaadin-dev start`
2. `.vaadin/vaadin-dev apply` after any edit to those classes. The outcome is green: `compiling -> runtime -> Stable`
3. Ask the bytecode who has the names:

```
javap -v -p target/classes/com/example/Tickets.class | grep -c MethodParameters
0
```

4. Open `http://localhost:8100/ticket`. The view fails, and `target/devloop/app.log` says

```
Caused by: java.lang.IllegalStateException: For queries with named parameters you need to
provide names for method parameters; Use @Param for query method parameters, or when on
Java 8+ use the javac flag -parameters
```

5. The same file compiled by Maven, for the contrast:

```
.vaadin/vaadin-dev stop && mvn -q clean compile
javap -v -p target/classes/com/example/Tickets.class | grep -c MethodParameters
2
```

The pom in that project inherits `-parameters` from `spring-boot-starter-parent` and never mentions it, which is the usual case.

One correction to the paragraph above, from running this: editing a pom property alone prints `no changes (pom.xml changed; nothing to recompile or restart)`. It takes a change that moves the classpath, adding a dependency for instance, to make the daemon recompile the whole module. The single file path is enough on its own, and it is the one most people will hit.

Recovery: `stop`, `mvn clean compile`, `start`.

### Getting the project

```
git clone --branch manolo --depth 1 https://github.com/vaadin/hackathon-25-3
cd hackathon-25-3/specs/issues/10-devloop
mvn spring-boot:run
```

Found on 25.3.0-beta1.
