# Issue drafts

One file per finding, ready to post. Nothing here has been published.

Each file starts with two lines that are not part of the issue body:

```
REPO: vaadin/flow
TITLE: What the issue is called
---
```

Everything below the `---` is the body. To post one, once it reads the way you want it to:

```
gh issue create --repo <REPO> --title "<TITLE>" --body-file specs/issues/<file>
```

Then put the link at the end of that finding's row in `FEEDBACK-25.3.md` or `FEEDBACK-PLATFORM.md`. A finding with no link is a finding nobody outside this repository has seen.

## What is drafted

| File | Repository | What it is about |
| --- | --- | --- |
| `01-runtime-theme-shadow-dom.md` | flow | A theme added at runtime never reaches component shadow roots, so the grid's screen reader only text is painted and sizes the column |
| `02-login-csrf.md` | web-components | The login form's CSRF field is empty until the component itself submits, so a scripted login posts no token and looks like it worked |
| `03-applayout-shift.md` | web-components | Content is painted at the full window width and reflows by the drawer's width, with the drawer's own attributes already final |
| `04-signalbinding.md` | flow | A signal bound text cannot be rebound and nothing releases it, so any dialog opened twice throws |
| `06-query-null-sort.md` | flow | A null sort order list NPEs inside the Spring Data helper rather than at the call |
| `08-pagetitlegenerator-bean.md` | flow | A generator annotated `@Component` silently renames every page in the application |
| `09-charts-styled-mode.md` | web-components | A chart ignores the theme until styled mode is on, and the page listing the style properties does not say so |

## Reproducers

Each issue that needs one has a mini project beside it: `pom.xml`, an `Application`, and a single view that does nothing else. Run one with `mvn spring-boot:run` and open the port its `application.properties` names.

| Project | Port | What it shows |
| --- | --- | --- |
| `01-grid-selectall/` | 8090 | The same grid with a declared theme: the span is hidden and the column is 35 pixels |
| `01b-test-runtime-theme/` | 8091 | One line different, the theme added at runtime: the span is 140 pixels and the column is 201 |
| `03-applayout-shift/` | 8093 | Prints its own width every twenty milliseconds and reports each change |
| `04-signalbinding/` | 8094 | Open the dialog, close it, open it again |
| `06-query-null-sort/` | 8096 | Two buttons, one with a null sort list and one with an empty one |
| `08-pagetitlegenerator-bean/` | 8098 | Read the browser tab on a route that declared its own title |

**What the reproducers have already changed.** Three of the nine have been run, and two of those three were wrong.

`01` was filed against `Grid` and is about `Page.addStyleSheet`: a bare grid with a declared theme hides that span correctly, and only a theme loaded at runtime leaves it visible. The issue moved from `web-components` to `flow`.

`07` was withdrawn. `LazyDataView.getItems()` was recorded as throwing `/ by zero` on a pageable grid, and it does not: not in a bare project, not on a grid nobody rendered, and not on this application's own board, which counts 265. The row in `FEEDBACK-25.3.md` is struck through and kept, because a finding that turned out to be wrong is worth knowing about.

`05` was withdrawn too, and it is the most embarrassing of the three: `Markdown.getContent()` returns a bound value without complaint. The `BindingActiveException` behind that row was real and came from a `Button` rebound on its second attach, which is `04`. One cause, two symptoms, and the wrong one got written down.

`06` reproduced exactly, with the same stack trace.

`04` is real, and its reproducer does not yet demonstrate it: the dialog in it did not open under automation, so the project needs another pass. What is certain is the fix, because removing it brings the exception back in this application.

The remaining views are written and have not been run.

`02-login-csrf` has no project: it needs Spring Security wired up, and the reproduction is three lines of JavaScript against any secured Vaadin application. `09-charts-styled-mode` has none either, because Charts needs a licence to run and the code is four lines.

## What is not drafted yet

The rest of `FEEDBACK-25.3.md` and `FEEDBACK-PLATFORM.md`. These nine were chosen because each has a reproduction somebody else can run in a minute. A finding whose reproduction is "this application, on this branch, on this screen" is not ready, and cutting it down is the work.

Two of them are worth opening even if nothing else ever is: `02` and `03`. The first makes a scripted login look successful when nobody is signed in, and the second moves every application's content sideways on every cold load.
