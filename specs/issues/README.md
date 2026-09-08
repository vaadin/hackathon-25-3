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

A GitHub issue cannot carry a file attachment through the API, so a draft that has a project links it instead, by full commit SHA under `github.com/vaadin/hackathon-25-3`. A SHA rather than a branch, so the link keeps working when the branch moves, and each of those drafts ends with the three lines that clone just that project. Repoint them if this work ever lands on the default branch.

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
| `12-devloop-compiler-flags.md` | flow | `10-devloop/`, and `javap` answering 0 where Maven answers 2 |
| `13-ai-tool-schema-dropped.md` | flow | A `ToolSpec` with an unescaped quote in its schema. Needs a key |
| `14-browserless-find-slotted.md` | flow | `14-browserless-find/`, then `mvn test`: two failures with exact numbers |
| `15-formlayout-colspan-steps.md` | web-components | `15-formlayout-colspan/`, two routes and a table of measured widths |
| `16-emailfield-validation-message.md` | flow-components | Two lines: the same property bound to each field |
| `17-navigate-rejects-query-string.md` | flow | One line |
| `18-downloadhandler-no-session-lock.md` | docs | A snippet, and the note that is easy to miss |
| `19-shared-signal-operation-never-completes.md` | flow | `19-shared-signal-env/`, then `mvn test` |
| `20-grid-select-all-lazy.md` | flow-components | `20-grid-selectall-lazy/`, one grid alone on a page, one click |
| `22-browserless-differences.md` | flow | Four differences, two or three lines each |
| `23-addstylesheet-order-and-readd.md` | flow | `38-dark-mode/`, route `/swap`, four buttons in order |
| `25-featureflags-rewrites-source.md` | flow | One call, then `git status` |
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
| `38-dark-mode-two-mechanisms.md` | flow | `38-dark-mode/`, a button per mechanism and the measured colours |
| `39-imported-css-blocked-by-security.md` | flow | `39-imported-css/`, two curl commands and one heading that stays black |
| `40-upload-file-arrived-event.md` | flow-components | An `Upload` whose handler belongs to the orchestrator |
| `41-gridaicontroller-time-column.md` | flow | One question selecting a SQL `TIME`. Needs a licence and a key |
| `42-ai-marker-badge-does-not-open.md` | web-components | Fill a field, click the badge. Needs a licence and a key |
| `43-getpageheader-without-route-parameters.md` | flow | `08-pagetitlegenerator-bean/`, the tab and the navbar side by side |
| `44-devloop-missing-hmr-line.md` | flow | `10-devloop/`, both outcomes printed side by side |
| `45-older-than-25-3.md` | flow | Four older behaviours, from `FEEDBACK-PLATFORM.md` |

## Not ready

| Finding | Why, and what it needs |
| --- | --- |
| `24-push-reconnect-logged-as-error.md`, a push reconnect is logged as an error | Reliable in the bakery, and it did not happen once in the minimal project: three tries, with a fast start, with a start deliberately slowed to five and a half seconds, and with the dev tools panel open so a push connection existed. `grep -c ERROR` answered 0 every time. What it needs is whatever the bakery has that the small one does not, most likely a client that reconnects while the server is still coming up |
| `04-signalbinding.md`, a signal bound text cannot be rebound | Real in the bakery, and its project does not demonstrate it after three attempts. The fix is certain: reverting it brings the exception back, with its stack |
| A tool call on the UI thread deadlocks `FormAIController` for ever | Needs an `LLMProvider` of its own that calls the fill tool on the thread it was handed, and a test with a preemptive timeout so the reproduction ends instead of hanging |
| A GridPro cell does not enter edit mode from a synthesised double click | Needs a browser, a licence, and a decision about whether it is a bug or the point |

## Withdrawn

Seven findings died when somebody tried to reproduce them. Every one was killed by building the project or repeating the steps, not by rereading the row.

| Finding | What happened |
| --- | --- |
| `02` login overlay CSRF field | The field really is empty, and it is harmless: a scripted submit with no token signs in anyway, in the bare project and in the bakery. CSRF is not enforced on that POST in a default setup |
| `05` `Markdown.getContent()` throws on a bound value | It does not. The exception was a `Button` rebound on its second attach, which is `04`. One cause, two symptoms, and the wrong one was written down |
| `07` `LazyDataView.getItems()` divides by zero | Not in a bare project, not on an unrendered grid, and not on the bakery's board, which counts 265 |
| A `@Menu` order change reports `Stable` and is not live | **Wrong, and it was the headline for a day.** The bare project applies the new order after a page reload, and so does the bakery. What is true is much smaller and the loop already prints it: an already rendered page keeps its old output until it renders again |
| `LicenseChecker` answers the same for a product that does not exist | Kept as a note in `FEEDBACK-25.3.md`, not as an issue: nothing in it is a defect |
| An exception inside `Signal.effect` is close to invisible | **Wrong.** A test that collects everything logged while an effect throws finds it: `ERROR com.vaadin.flow.server.DefaultErrorHandler: Unexpected error: ...`, with the exception attached to the event. It is logged, at error level, with its stack. Whatever we were reading when we concluded otherwise, it was not the log |
| `bindChildren` is documented and does not exist | **Wrong, and it cost this application a workaround it carried for the whole build.** `container.bindChildren(list, item -> component)` compiles and runs on 25.3.0-beta1. The original conclusion came from a compile error that was really about the mapper's argument, a `ValueSignal<T>` and not a `T`. The forty line adapter is deleted and three views now call the platform |

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
| `08-pagetitlegenerator-bean/` | 8098 | A route with its own `@PageTitle`, renamed by a bean, and a navbar header that disagrees with the tab |
| `09-charts-styled-mode/` | 8110 | The same chart twice on a dark page |
| `10-devloop/` | 8100 | Two views with `@Menu`, a view waiting for a bean that does not exist yet, a stylesheet for the `hmr:` line, and a repository query that needs parameter names |
| `15-formlayout-colspan/` | 8115 | A colspan of three at 4, 2 and 1 columns, and `--_max-columns` computing to 1 |
| `14-browserless-find/` | 8104 | Five checkboxes on screen, one in the tree. `mvn test` |
| `16-emailfield-message/` | 8116 | The same property in two fields. `mvn test` |
| `20-grid-selectall-lazy/` | 8120 | Three grids and their selection counts, and one inert checkbox on its own page |
| `38-dark-mode/` | 8138 | Both dark mechanisms under both themes, and a stylesheet lost when it is re-added |
| `39-imported-css/` | 8139 | A secured application where the declared sheet loads and its import redirects to the login |
| `25-featureflags/` | 8125 | One test: the call writes a file into the project |
| `19-shared-signal-env/` | 8106 | A shared signal in a singleton, and one test that writes to it. `mvn test` |
| `22-browserless-differences/` | 8112 | Two of the four differences, one passing test and one failing. `mvn test` |
| `45-older-behaviours/` | 8145 | Two tests, and a view whose CSS grid collapses to one column |

Build output is ignored, so a project is sources only. `10-devloop` needs `mvn flow:install-dev-cli` first, and the `flow-maven-plugin` declaration that makes that prefix work is already in its pom.

## Every row accounted for

Both feedback files were read row by row and each row now has one of four destinations. Nothing is left unexamined.

| Destination | How many | Where it went |
| --- | --- | --- |
| Drafted as an issue | 39 drafts covering most of the rows, several of them bundling a family | This directory |
| Real, not yet reduced | 3 | The table above, each with what it needs |
| Not a report, it was our own rule | 9 | The specifications: `08-testing.md`, `10-dev-loop.md`, `03-architecture.md` |
| Withdrawn or not reproducible | 7 | Deleted, with the lesson from each in the table above |
| Positive, worth saying anyway | 10 | `REPORT.md`, under what worked exactly as advertised |

Some drafts deliberately carry several rows, because the rows shared one root: `22` is four browserless differences, `30` is seven documentation gaps, `34` is four dev loop notes, `35` is three AI API requests, `45` is four behaviours older than this release.

Three drafts need something the reader may not have, and each says so at the bottom: a Charts licence for `09`, an OpenAI key for `13`, `28`, `29` and `41`, and both for `42`.
