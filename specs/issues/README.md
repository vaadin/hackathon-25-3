# Issue drafts

One file per finding, ready to post. Nothing here has been published.

Each file starts with two lines that are not part of the body:

```
REPO: vaadin/flow
TITLE: What the issue is called
---
```

To post one:

```
gh issue create --repo <REPO> --title "<TITLE>" --body-file specs/issues/<file>
```

Then put the link at the end of that finding's row in `FEEDBACK-25.3.md`. A finding with no link is a finding nobody outside this repository has seen.

## Ready to post

Each of these has a reproduction somebody else can run in a few minutes.

| Draft | Repository | Reproduction |
| --- | --- | --- |
| `01-runtime-theme-shadow-dom.md` | flow | Two projects, `01-grid-selectall/` and `01b-test-runtime-theme/`, one line apart |
| `03-applayout-shift.md` | web-components | `03-applayout-shift/`, which measures itself and prints every change |
| `06-query-null-sort.md` | flow | `06-query-null-sort/`, two buttons |
| `08-pagetitlegenerator-bean.md` | flow | `08-pagetitlegenerator-bean/`, read the browser tab |
| `09-charts-styled-mode.md` | web-components | `09-charts-styled-mode/`, the same chart twice. Needs a Charts licence |
| `11-devloop-new-bean.md` | flow | `10-devloop/`, four steps, confirmed there |
| `12-devloop-compiler-flags.md` | flow | Any project with a `@Query` using a named parameter |
| `13-ai-tool-schema-dropped.md` | flow | A `ToolSpec` with an unescaped quote in its schema. Needs an OpenAI key |
| `14-browserless-find-slotted.md` | flow | `14-browserless-find/`, then `mvn test`: two failures with exact numbers |
| `15-formlayout-colspan-steps.md` | web-components | Six lines of `FormLayout` |
| `16-emailfield-validation-message.md` | flow-components | Two lines: the same property bound to each field |
| `17-navigate-rejects-query-string.md` | flow | One line |
| `18-downloadhandler-no-session-lock.md` | docs | A snippet, and the note that is easy to miss |
| `19-shared-signal-operation-never-completes.md` | flow | `19-shared-signal-env/`, then `mvn test`: two passing tests that say the write landed and the operation did not |

## Not ready

| Draft | Why |
| --- | --- |
| `04-signalbinding.md` | Real in the bakery, and `04-signalbinding/` does not demonstrate it after three attempts. The fix is certain: reverting it brings the exception back, with its stack |

## Withdrawn

Five findings died when somebody tried to reproduce them. Every one of the five was killed by building the project or repeating the steps, not by rereading the row.

| Finding | What happened |
| --- | --- |
| `02` login overlay CSRF field | The field really is empty, and it is harmless: a scripted submit with no token signs in anyway, in the bare project and in the bakery. CSRF is not enforced on that POST in a default setup |
| `05` `Markdown.getContent()` throws on a bound value | It does not. The exception was a `Button` rebound on its second attach, which is `04`. One cause, two symptoms, and the wrong one was written down |
| `07` `LazyDataView.getItems()` divides by zero | Not in a bare project, not on an unrendered grid, and not on the bakery's board, which counts 265 |
| A `@Menu` order change reports `Stable` and is not live | **Wrong, and it was the headline for a day.** The bare project applies the new order after a page reload, and so does the bakery. What is true is much smaller and the loop already prints it: an already rendered page keeps its old output until it renders again |
| `LicenseChecker` answers the same for a product that does not exist | Kept as a note in `FEEDBACK-25.3.md`, not as an issue: nothing in it is a defect |

## The reproducer projects

Each is a `pom.xml`, an `Application`, and one or two classes that do nothing else. Run with `mvn spring-boot:run`, or `mvn test` where the reproduction is a test.

| Project | Port | What it shows |
| --- | --- | --- |
| `01-grid-selectall/` | 8090 | A declared theme: the screen reader span is one pixel and the column is 35 |
| `01b-test-runtime-theme/` | 8091 | One line different, the theme added at runtime: the span is 140 and the column is 201 |
| `02-login-csrf/` | 8092 | A secured application whose scripted login succeeds with no token |
| `03-applayout-shift/` | 8093 | Prints its own width every twenty milliseconds |
| `04-signalbinding/` | 8094 | Open the dialog, close it, open it again |
| `06-query-null-sort/` | 8096 | Two buttons, one null sort list and one empty one |
| `08-pagetitlegenerator-bean/` | 8098 | A route with its own `@PageTitle`, renamed by a bean |
| `09-charts-styled-mode/` | 8110 | The same chart twice on a dark page |
| `10-devloop/` | 8100 | Two views with `@Menu`, and a view waiting for a bean that does not exist yet |
| `14-browserless-find/` | 8104 | Five checkboxes on screen, one in the tree. `mvn test` |
| `19-shared-signal-env/` | 8106 | A shared signal in a singleton, and one test that writes to it. `mvn test` |

Build output is ignored, so a project is sources only. `10-devloop` needs `mvn flow:install-dev-cli` first, and the `flow-maven-plugin` declaration that makes that prefix work is already in its pom.

## What is not drafted, and why

`FEEDBACK-25.3.md` holds the rest. A row is drafted here once its reproduction fits in a project or a snippet. A row whose reproduction is "this application, on this branch, on this screen" is not ready, and cutting it down is the work.

Two rows are worth cutting down next:

1. A tool call on the UI thread deadlocks `FormAIController` for ever, with no error and no timeout. A project needs an `LLMProvider` of its own that calls the fill tool on the thread it was given, and a test with a preemptive timeout so the reproduction ends instead of hanging.
2. The AI field marker's badge does not open its popover, which is what makes source tracking unreachable by a person. A project needs a licence and a filled field, so the reproduction is a browser one.

The shared signal row was cut down and answered something else. Reducing it showed the write always lands and the operation's future never completes, which is what had been read as a dropped write. One cause, one wrong diagnosis, and a day spent on the wrong one.
