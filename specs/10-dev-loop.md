# 10 Dev loop

Measured on this application, on 25.3.0-beta1, on a JetBrains Runtime. Every number below is from a real edit to this repository, not from the documentation.

## Installing it

```
mvn flow:install-dev-cli
```

Not `vaadin:install-dev-cli`: in beta1 the `vaadin-maven-plugin` does not expose the goal, so `flow-maven-plugin` has to be declared and used instead. See `FEEDBACK-25.3.md`.

It writes, and all of it is committed like `mvnw`:

| Path | What it is |
| --- | --- |
| `.vaadin/vaadin-dev` plus `.cmd` and `.ps1` | The CLI, one per platform |
| `.agents/skills/vaadin-devloop/SKILL.md` and `reference.md` | The instructions, tool agnostic, for any coding agent |
| `.claude/skills/vaadin-devloop/SKILL.md` | A thin Claude Code binding that points at the shared file |

The instructions being a committed file rather than a setting is the good idea here: every agent and every teammate on the repository reads the same cycle.

## The cycle

`status` first, `start` if stopped, **open the browser before the first apply**, edit, `apply`, verify in the page that was already open. The exit code of `apply` is the verdict: 0 live, 1 failed, 4 superseded.

Never start the application through Maven at the same time. The daemon owns the process and a second one fights it for port 8080.

## What each kind of edit costs

| Edit | Outcome | Time |
| --- | --- | --- |
| A stylesheet under `META-INF/resources` | `pushed 1 stylesheet(s) in place`, no reload | 0.06 s |
| A Java method body | `redefineClasses(1); onHotswap completed=true`, no reload | 0.8 s |
| A brand new class with a new `@Route` | `redefineClasses(0)`, the route is reachable immediately, no reload | 0.6 s |
| A translation bundle under `vaadin-i18n` | Restart: bundles are read at startup | 7.0 s |
| Deleting a class | Restart: a loaded class cannot be un-defined | 6.9 s |
| A syntax error | `compiling → Failed` with the file, line and column, exit 1. The app keeps its last good bytes and stays up | instant |

The surprise is the third row. A new route registering without a restart is not what a decade of Flow has taught anybody to expect, and it is the single most useful thing the loop does.

The trap is the fourth. Batching a Java edit with a translation edit turns a 0.8 second hot swap into a 7 second restart, because the restart is decided by the most demanding file in the change set. When iterating on Java, leave the bundles alone until the end.

## What it does not do

- **Java only.** It compiles with `javax.tools.JavaCompiler`, so the Kotlin about view is invisible to it and needs a normal restart. In a mixed project this is worth knowing before you spend twenty minutes wondering why nothing happened.
- **Configuration needs `restart`.** `application.properties` is read at startup like any other resource.
- **A stylesheet added at runtime with `page.addStyleSheet` is not pushed.** `apply` still says it pushed one, and the page keeps the old rules until it is reloaded. This application loads its palette that way, so an edit to `styles/themes/bakery.css` needs a reload even though `apply` looks green.
- **A CSS push needs a page already open.** With no browser connected, `apply` says the resource reached the classpath and nothing about a push, which is honest and not the answer you wanted. Open the page first.
- **It does not print the `hmr:` line when a change set mixes CSS with Java.** CSS alone says `hmr: 1 resource(s) copied, pushed 1 stylesheet(s) in place`. The same CSS edit with a Java edit beside it says `hot-reload: redefineClasses(1)` and nothing about the stylesheet, and the stylesheet did reach the open page. The output undersells what happened, which is the wrong direction for a tool people trust one line from. Filed as `specs/issues/44-devloop-missing-hmr-line.md`.
- **It compiles without the project's compiler flags.** The daemon's own `javax.tools.JavaCompiler` invocation does not carry `-parameters`, which `spring-boot-starter-parent` sets and nobody writes down, so every class the daemon compiles loses its parameter names. A Spring Data repository method with a named parameter then fails at runtime, naming a helper the caller never mentioned. `javap -v` on the class the daemon wrote counts zero `MethodParameters`, and two on the same class after `mvn clean compile`. Recovery is `stop`, `mvn clean compile`, `start`, and it is `specs/issues/12-devloop-compiler-flags.md`.
- **A new Spring bean is reported `Stable` and is not registered.** The class hot swaps, the context does not learn about it, and the first injection throws `NoSuchBeanDefinitionException`. `restart` fixes it. `specs/issues/11-devloop-new-bean.md`.

## Verifying

`apply` proves the bytes are live. Only the browser proves the UI renders what was intended, and only a page that was already open proves the change landed without a reload. The session survives all of it: while testing this, a cart went from one item to two to three across a hot swap, a failed compile and a recovery, without ever being rebuilt.

## Diagnostics

`status --json` and `apply --json` return the full picture: state, the modules in the loop, the last transaction with its classification, its change set, its escalation reason and its timings split into detect, compile and runtime. That is what a script or an agent should read rather than parsing the human output.

## The instructions are a file, not a setting

The loop's guidance for agents lives in `.agents/skills/vaadin-devloop/SKILL.md`, committed in the repository, rather than in a tool configuration. That means it is reviewed, versioned and diffed like anything else, and it means an agent that never reads files never sees it.
