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
| `03-applayout-shift.md` | web-components | Content is painted at the full window width and reflows by the drawer's width, with the drawer's own attributes already final |
| `04-signalbinding.md` | flow | A signal bound text cannot be rebound and nothing releases it, so any dialog opened twice throws |
| `06-query-null-sort.md` | flow | A null sort order list NPEs inside the Spring Data helper rather than at the call |
| `08-pagetitlegenerator-bean.md` | flow | A generator annotated `@Component` silently renames every page in the application |
| `09-charts-styled-mode.md` | web-components | A chart bakes Highcharts' palette into its SVG until styled mode is on, and the page listing the style properties does not say so |

## Reproducers

Each issue that needs one has a mini project beside it: `pom.xml`, an `Application`, and a single view that does nothing else. Run one with `mvn spring-boot:run` and open the port its `application.properties` names.

| Project | Port | What it shows |
| --- | --- | --- |
| `01-grid-selectall/` | 8090 | The same grid with a declared theme: the span is hidden and the column is 35 pixels |
| `01b-test-runtime-theme/` | 8091 | One line different, the theme added at runtime: the span is 140 pixels and the column is 201 |
| `03-applayout-shift/` | 8093 | Prints its own width every twenty milliseconds and reports each change |
| `09-charts-styled-mode/` | 8110 | The same chart twice on a dark page, one line apart |
| `04-signalbinding/` | 8094 | Open the dialog, close it, open it again |
| `06-query-null-sort/` | 8096 | Two buttons, one with a null sort list and one with an empty one |
| `08-pagetitlegenerator-bean/` | 8098 | Read the browser tab on a route that declared its own title |

**What the reproducers have already changed.** Three of the nine have been run, and two of those three were wrong.

`01` was filed against `Grid` and is about `Page.addStyleSheet`: a bare grid with a declared theme hides that span correctly, and only a theme loaded at runtime leaves it visible. The issue moved from `web-components` to `flow`.

`07` was withdrawn. `LazyDataView.getItems()` was recorded as throwing `/ by zero` on a pageable grid, and it does not: not in a bare project, not on a grid nobody rendered, and not on this application's own board, which counts 265. The row in `FEEDBACK-25.3.md` is struck through and kept, because a finding that turned out to be wrong is worth knowing about.

`05` was withdrawn too, and it is the most embarrassing of the three: `Markdown.getContent()` returns a bound value without complaint. The `BindingActiveException` behind that row was real and came from a `Button` rebound on its second attach, which is `04`. One cause, two symptoms, and the wrong one got written down.

`06` reproduced exactly, with the same stack trace.

`04` is real, and its reproducer does not yet demonstrate it: the dialog in it did not open under automation, so the project needs another pass. What is certain is the fix, because removing it brings the exception back in this application.

`02` was withdrawn once its project existed. The hidden field really is rendered with no name and no value, and a scripted `form.submit()` that carries no token **signs in anyway**: "Signed in as: user" in the bare project, and `curl -X POST -d "username=...&password=..." /login` answers 302 to `/` in this application. CSRF is not enforced on that POST. The browser tier's flake had been blamed on it, and the fix that actually worked was asking the server whether anybody is signed in.

`03` and `08` reproduced in bare projects. `03` prints its own measurements, and at a 1400 pixel viewport the content goes from 1386 to 1302, the drawer's width, with `drawer-opened` and `overlay` identical in both samples. `08` puts "We cannot find that product" in the browser tab of a route that declared `@PageTitle("About")`.

### Where the nine ended up

| | State |
| --- | --- |
| `01` runtime theme and shadow DOM | Reproduced, in two projects that differ by one line. The cause was corrected: it is not `Grid` |
| `03` AppLayout paints before it reserves | Reproduced, by a project that measures itself |
| `06` Query with a null sort list | Reproduced, same stack trace |
| `08` PageTitleGenerator bean | Reproduced |
| `02` login CSRF field | **Withdrawn.** The project got built and killed it: a scripted submit with no token signs in perfectly well, in the bare project and in this application. The empty field is real and harmless in a default setup |
| `04` a signal bound text cannot be rebound | Real in this application, and the bare project does not demonstrate it after three attempts. **Not ready to open.** What is certain is the fix: reverting it brings the exception back here, with its stack |
| `09` Charts styled mode | Reproduced. Two identical charts on a dark page, one line apart: the default one bakes `#ffffff` and `#2caffe` into its SVG, the styled one carries no fills and takes the theme's colours |
| `05`, `07` | Withdrawn. See above |

Five of the nine are ready: `01`, `03`, `06`, `08`, `09`. Three were wrong and are withdrawn: `02`, `05`, `07`. One is real here and not yet reduced: `04`. 

Three of the nine turned out to be wrong, and every one of the three was found by building the project rather than by rereading the row. `02` is the one worth remembering: it had a stack of circumstantial evidence, a plausible mechanism, and a fix that appeared to work, and the mechanism was not real.

`02-login-csrf` has no project: it needs Spring Security wired up, and the reproduction is three lines of JavaScript against any secured Vaadin application. `09-charts-styled-mode` needs a Charts licence to run, which anybody at Vaadin has.

## What is not drafted yet

The rest of `FEEDBACK-25.3.md` and `FEEDBACK-PLATFORM.md`. These nine were chosen because each has a reproduction somebody else can run in a minute. A finding whose reproduction is "this application, on this branch, on this screen" is not ready, and cutting it down is the work.

Two of them are worth opening even if nothing else ever is: `02` and `03`. The first makes a scripted login look successful when nobody is signed in, and the second moves every application's content sideways on every cold load.
