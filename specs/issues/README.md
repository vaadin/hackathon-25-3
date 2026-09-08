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
| `13-ai-tool-schema-dropped.md` | flow | A `ToolSpec` with an unescaped quote in its schema. Needs a key |
| `14-browserless-find-slotted.md` | flow | `14-browserless-find/`, then `mvn test`: two failures with exact numbers |
| `15-formlayout-colspan-steps.md` | web-components | Six lines of `FormLayout` |
| `16-emailfield-validation-message.md` | flow-components | Two lines: the same property bound to each field |
| `17-navigate-rejects-query-string.md` | flow | One line |
| `18-downloadhandler-no-session-lock.md` | docs | A snippet, and the note that is easy to miss |
| `19-shared-signal-operation-never-completes.md` | flow | `19-shared-signal-env/`, then `mvn test` |
| `20-grid-select-all-lazy.md` | flow-components | Four lines, and a table of the six doors that are closed |
| `21-bindchildren-documented-and-absent.md` | docs | It does not compile |
| `22-browserless-differences.md` | flow | Four differences, two or three lines each |
| `23-addstylesheet-order-and-readd.md` | flow | Four lines: remove a sheet and add the same URL back |
| `24-push-reconnect-logged-as-error.md` | flow | Any `@Push` application, one page open, one restart |
| `25-featureflags-rewrites-source.md` | flow | One call, then `git status` |
| `26-signal-effect-exception-invisible.md` | flow | Three lines |
| `27-ai-form-discovery-composite.md` | flow | A field inside a `Composite`, and the form state the model reads |
| `28-ai-turn-with-no-trace.md` | flow | A failing query and one question. Needs a key |
| `29-ai-source-tracking-never-arrives.md` | flow | Source tracking on, one live turn, `getFieldSource`. Needs a key |
| `30-docs-gaps.md` | docs | Seven pages that exist and do not say the thing |
| `31-observability-docs.md` | docs | Three curl commands |
| `32-deprecations-without-replacement.md` | flow | Three deprecation messages |
| `33-container-tokens-not-interchangeable.md` | docs | Style a panel, switch the theme, look at it |
| `34-devloop-tooling-notes.md` | flow | Four small things, one of which blocks installation |
| `35-ai-api-requests.md` | flow | Three things the API cannot do |
| `36-server-side-gaps.md` | flow | An absolute URL, and asking what is licensed |
| `37-testbench-cdp-blocked.md` | testbench | The cast to `HasCdp` fails against the proxy |
| `38-dark-mode-two-mechanisms.md` | flow | Two lines: one theme goes dark, the other does not |
| `39-imported-css-blocked-by-security.md` | flow | An `@import` in a stylesheet, in a secured application |
| `40-upload-file-arrived-event.md` | flow-components | An `Upload` whose handler belongs to the orchestrator |
| `41-gridaicontroller-time-column.md` | flow | One question selecting a SQL `TIME`. Needs a licence and a key |
| `42-ai-marker-badge-does-not-open.md` | web-components | Fill a field, click the badge. Needs a licence and a key |
| `43-getpageheader-without-route-parameters.md` | flow | Compare the header with the browser tab |
| `44-devloop-missing-hmr-line.md` | flow | One Java file and one CSS file in the same change set |
| `45-older-than-25-3.md` | flow | Four older behaviours, from `FEEDBACK-PLATFORM.md` |

## Not ready

| Finding | Why, and what it needs |
| --- | --- |
| `04-signalbinding.md`, a signal bound text cannot be rebound | Real in the bakery, and its project does not demonstrate it after three attempts. The fix is certain: reverting it brings the exception back, with its stack |
| A tool call on the UI thread deadlocks `FormAIController` for ever | Needs an `LLMProvider` of its own that calls the fill tool on the thread it was handed, and a test with a preemptive timeout so the reproduction ends instead of hanging |
| A GridPro cell does not enter edit mode from a synthesised double click | Needs a browser, a licence, and a decision about whether it is a bug or the point |

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

## Every row accounted for

Both feedback files were read row by row and each row now has one of four destinations. Nothing is left unexamined.

| Destination | How many | Where it went |
| --- | --- | --- |
| Drafted as an issue | 41 drafts covering most of the rows, several of them bundling a family | This directory |
| Real, not yet reduced | 3 | The table above, each with what it needs |
| Not a report, it was our own rule | 9 | The specifications: `08-testing.md`, `10-dev-loop.md`, `03-architecture.md` |
| Withdrawn or not reproducible | 5 | Deleted, with the lesson from each in the table above |
| Positive, worth saying anyway | 10 | `REPORT.md`, under what worked exactly as advertised |

Some drafts deliberately carry several rows, because the rows shared one root: `22` is four browserless differences, `30` is seven documentation gaps, `34` is four dev loop notes, `35` is three AI API requests, `45` is four behaviours older than this release.

Three drafts need something the reader may not have, and each says so at the bottom: a Charts licence for `09`, an OpenAI key for `13`, `28`, `29` and `41`, and both for `42`.
