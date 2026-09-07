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
| `01-grid-selectall.md` | web-components | A screen reader only sentence is painted into the grid and sizes the selection column to 199 pixels |
| `02-login-csrf.md` | web-components | The login form's CSRF field is empty until the component itself submits, so a scripted login posts no token and looks like it worked |
| `03-applayout-shift.md` | web-components | Content is painted at the full window width and reflows by the drawer's width, with the drawer's own attributes already final |
| `04-signalbinding.md` | flow | A signal bound text cannot be rebound and nothing releases it, so any dialog opened twice throws |
| `05-markdown-getcontent.md` | flow | A getter that refuses to get when the property is bound |
| `06-query-null-sort.md` | flow | A null sort order list NPEs inside the Spring Data helper rather than at the call |
| `07-lazydataview-getitems.md` | flow | The documented way to read a lazy grid and the documented way to fill one do not work together |
| `08-pagetitlegenerator-bean.md` | flow | A generator annotated `@Component` silently renames every page in the application |
| `09-charts-styled-mode.md` | web-components | A chart ignores the theme until styled mode is on, and the page listing the style properties does not say so |

## What is not drafted yet

The rest of `FEEDBACK-25.3.md` and `FEEDBACK-PLATFORM.md`. These nine were chosen because each has a reproduction somebody else can run in a minute. A finding whose reproduction is "this application, on this branch, on this screen" is not ready, and cutting it down is the work.

Two of them are worth opening even if nothing else ever is: `02` and `03`. The first makes a scripted login look successful when nobody is signed in, and the second moves every application's content sideways on every cold load.
