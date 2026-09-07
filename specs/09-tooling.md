# 09 Tooling

Everything an agent or a teammate needs so that the same command produces the same result on any machine.

## Versions

| Thing | Version | Note |
| --- | --- | --- |
| Vaadin | `25.3.0-beta1` | One `vaadin.version` property, never repeated |
| Spring Boot | 4.0.x | Parent pom |
| Spring AI | 2.0.0 | BOM import, only in the `ai` profile |
| Java | 21 | JetBrains Runtime recommended, see the dev loop below |
| Node | managed by Vaadin | Never pinned by hand |
| browserless test | managed by `vaadin-bom` | Currently 1.2.0-beta1, versioned independently of the platform. Never hand pin it |
| Observability Kit | managed by `vaadin-bom` | Currently 5.0.0-beta1, needs the prereleases repository |

Repositories: `central` plus `https://maven.vaadin.com/vaadin-prereleases`.

## Maven profiles

What the pom actually declares:

| Profile | Default | Turns on |
| --- | --- | --- |
| none | yes | The whole application, H2, no assistant. Runs and tests with no licence and no keys, see Licensing in `00-overview.md` |
| `ai` | no | `spring-ai-starter-model-openai` plus the `src/ai/java` source root. Without it there is no Spring AI on the classpath and the mock provider is used |
| `observability` | no | Observability Kit starter, actuator and the Prometheus registry |
| `postgres` | no | PostgreSQL driver and `data-postgresql.sql` |
| `production` | no | Production frontend bundle |

Specified here and not built:

| Profile | What was planned | What is there instead |
| --- | --- | --- |
| `bulk` | A generator that inflates the dataset for load and metrics demos | Nothing. The dataset is the static SQL and only that |
| `it` | Failsafe plus TestBench around the browser tests | Nothing: no Failsafe, no TestBench dependency, and no `*IT` class exists yet. See `08-testing.md` |

There is deliberately no `commercial` profile. The commercial components are part of this build, unconditionally, because this is the full featured version: see Licensing in `00-overview.md`. A core only version is a separate branch, not a switch here.

Rule: the default profile is the one that must always work, and it does, verified with no licence, no key and no network. That is a statement about the test suite, not a promise of fallbacks. The licence is enforced in the browser and in the production frontend build, and the suite uses neither, which is why it is green on a machine with no key.

## Feature flags

`src/main/resources/vaadin-featureflags.properties` is committed with:

```properties
com.vaadin.experimental.breadcrumbsComponent=true
com.vaadin.experimental.switchComponent=true
com.vaadin.experimental.aiComponents=true
```

`breadcrumbsComponent` and `switchComponent` both declare `requiresServerRestart`, so flipping them at runtime does nothing useful. They are set in the file from the first commit.

A startup check logs the state of every flag the application depends on and fails fast if a required one is off. The about view prints the same list, so a demo machine never lies about what is enabled.

Each flagged component sits behind a tiny factory (`Switches.create(...)`, `Breadcrumbs.trailFor(...)`) so that a renamed or removed flag is a one file fix and degrades to a themed Checkbox or an anchor trail instead of breaking the build.

## The coding agent dev loop

This is the 25.3 tooling story we want to exercise, and it has sharp edges in beta1.

### Installing it

In beta1 the `vaadin-maven-plugin` hides the goal. Reported in the hackathon channel on 4 September 2026 and expected to be fixed in beta2. Until then, declare the flow plugin explicitly:

```xml
<plugin>
    <groupId>com.vaadin</groupId>
    <artifactId>flow-maven-plugin</artifactId>
    <version>${vaadin.version}</version>
</plugin>
```

and install with `mvn flow:install-dev-cli`, not `mvn vaadin:install-dev-cli`. The spec keeps both commands documented with a note, so that when beta2 lands we delete the workaround rather than rediscover it.

### What it can and cannot do

- It compiles with `javax.tools.JavaCompiler` and resolves the classpath through Maven. **Java only.** Kotlin, Groovy and friends are not compiled by the loop.
- Consequence for epic 14: the Kotlin about view does not hot swap. Editing the `.kt` file needs a normal restart. This is written into that epic and into `CLAUDE.md`, so no agent burns twenty minutes wondering why its Kotlin edit did nothing.
- It looks for a JetBrains Runtime in `~/.jdks`, `JAVA_HOME` and `JDK_HOME`. It does not currently look in `~/.vaadin/jdk`, which is where the IDE plugins put it. On a machine where `JAVA_HOME` already points at a JBR there is nothing to do.
- Structural changes hot swap on a JetBrains Runtime. On a stock JDK they restart instead.

### Commands

| Command | When |
| --- | --- |
| `.vaadin/vaadin-dev status` | Is it up, and what did the last change do |
| `.vaadin/vaadin-dev start` | Once per session |
| `.vaadin/vaadin-dev apply` | Make the edits on disk live |
| `.vaadin/vaadin-dev restart` | After a configuration change |

Never start the application through Maven at the same time as the daemon.

What to expect after a change: a Java method body hot swaps with the page untouched, a stylesheet is pushed into the open page, theme CSS is rebuilt and pushed, a frontend annotation forces a restart, a JPA mapping or a proxied bean forces a restart and the loop names the class, and a compile error is reported at once with file and line.

## MCP servers

`.mcp.json` at the repository root:

| Server | Endpoint | Purpose |
| --- | --- | --- |
| Vaadin docs | `https://mcp.vaadin.com/docs-java/docs` | The next generation Java docs server being trialled during the hackathon. The installation snippet published at `mcp.vaadin.com/docs-java` still points at the production server and is wrong, so the endpoint above is the one to use |
| Playwright | `npx @playwright/mcp@latest` | Visual verification and the manual acceptance pass |

Rule for agents: always query the docs server with the Java flavour and version 25.3 before writing code against a 25.3 API. This is a beta, and memory of 25.2 is not good enough.

## Copilot

Enabled with `com.vaadin.experimental.copilotExperimentalFeatures=true` for the duration of the hackathon. What we deliberately use, and record in the demo script:

- The FormLayout editor with responsive steps, on the checkout contact form.
- The Test Recorder, to bootstrap the TestBench ITs listed in `08-testing.md`, which are then hardened by hand.
- The All Components view, as the quickest way to check what a 25.3 component looks like in the Aura theme.
- The Aura theme editor, for the colour modes and accent colours of epic 12.
- Kotlin support, on the about view of epic 14.

## Local commands

| Command | Result |
| --- | --- |
| `./mvnw` | Runs the app on 8080 with the demo dataset. `spring-boot:run` is the default goal |
| `./mvnw verify` | Unit plus browserless tests. No licence, no browser, no network |
| `./mvnw -Pai` | Runs it against a real model, needs `OPENAI_API_KEY`. Without the key it boots with the assistant off rather than failing |
| `./mvnw test -Pai -Dtest=LiveAssistantTest -Dsurefire.excludedGroups=` | The one test that really calls OpenAI. Excluded from every other run by its `live-ai` tag |
| `./mvnw verify -Pit` | Adds the TestBench tests. Starts the application on 8081, runs every `*IT` against a local Chrome and stops it again |
| `docker compose up -d` | Postgres, Prometheus and Grafana from `compose.yaml`. Optional: the default build runs on H2 with no Docker at all |
| `mvn flow:install-dev-cli` | Installs the dev loop, commit what it writes |

## Continuous integration

`.github/workflows/ci.yml`: Java 21, `./mvnw verify` with the default profile only, so CI proves that the licence free path works. That one workflow is all there is. The manual `-Pit` run with an injected TestBench licence, and the nightly build against the newest 25.3 prerelease, are both still to be written, and neither can be useful before the `it` profile exists.
