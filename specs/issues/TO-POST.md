# What to open, and in what shape

Forty one drafts, none of them posted. This is the posting list: what each one is, whether somebody else can run it, and which zip it comes with. The zips live in `projects/` and each draft links its own by commit SHA.

Two of the forty one cannot be reproduced outside this application and are marked as such. Post them last, or not at all until somebody reduces them.

```
gh issue create --repo <REPO> --title "<TITLE>" --body-file specs/issues/<file>
```

## vaadin/flow, 30 issues

26 of them are defects, the rest are documentation or API requests.

| Draft | What it says | Kind | Reproducible | Project |
| --- | --- | --- | --- | --- |
| `01-runtime-theme-shadow-dom.md` | A theme loaded with Page.addStyleSheet does not reach component shadow roots, so screen reader only text is painted | Bug | Yes | `01-grid-selectall.zip + 01b-test-runtime-theme.zip` |
| `04-signalbinding.md` | A signal bound text cannot be rebound and there is no way to release the binding | Bug | No, only here | none needed |
| `06-query-null-sort.md` | Query with a null sort order list throws an NPE inside VaadinSpringDataHelpers | Bug | Yes | `06-query-null-sort.zip` |
| `08-pagetitlegenerator-bean.md` | A PageTitleGenerator annotated @Component silently becomes the whole application's generator | Bug | Yes | `08-pagetitlegenerator-bean.zip` |
| `11-devloop-new-bean.md` | Dev loop reports Stable for a new Spring bean, and the bean is not registered | Bug | Yes | `10-devloop.zip` |
| `12-devloop-compiler-flags.md` | The dev loop daemon compiles without the project's compiler flags | Bug | Yes | `10-devloop.zip` |
| `13-ai-tool-schema-dropped.md` | An invalid tool schema logs a Jackson error that never names the tool | Bug, diagnostics | Yes, needs a key | `ai-live.zip` |
| `14-browserless-find-slotted.md` | Browserless find() cannot see a component handed to another component | Bug | Yes, a failing test | `14-browserless-find.zip` |
| `17-navigate-rejects-query-string.md` | UI.navigate(String) rejects a URL with a query string | Feature | Yes, a passing test | `22-browserless-differences.zip` |
| `19-shared-signal-operation-never-completes.md` | SignalOperation.result() never completes for a shared signal in a browserless test | Bug | Yes, a hanging test | `19-shared-signal-env.zip` |
| `22-browserless-differences.md` | Four ways the browserless environment differs from a running application | Bug, four of them | Yes, tests | `22-browserless-differences.zip` |
| `23-addstylesheet-order-and-readd.md` | Page.addStyleSheet has two problems when sheets are swapped at runtime | Bug plus a feature | Yes, the re-add half | `38-dark-mode.zip` |
| `24-push-reconnect-logged-as-error.md` | A push reconnect during a restart is logged as an application error | Bug | No, only here | none needed |
| `25-featureflags-rewrites-source.md` | FeatureFlags.setEnabled rewrites the project's source file | Bug | Yes, a test | `25-featureflags.zip` |
| `27-ai-form-discovery-composite.md` | FormAIController does not walk into a Composite, so fields inside one are invisible to the model | Bug | Yes, needs a key | `ai-live.zip` |
| `28-ai-turn-with-no-trace.md` | An AI turn that produces nothing leaves no trace on any surface | Bug | Yes, needs a key | `ai-live.zip` |
| `29-ai-source-tracking-never-arrives.md` | Source tracking is enabled and no model ever reports a source | Bug | Yes, needs a key | `ai-live.zip` |
| `32-deprecations-without-replacement.md` | Three deprecations that do not name a replacement | Docs | javap on the artifacts | none needed |
| `34-devloop-tooling-notes.md` | Four small things about the dev loop CLI | Bug plus docs, four notes | Commands in the draft | none needed |
| `35-ai-api-requests.md` | Three things the AI API cannot do that an application needs | Feature, three of them | Nothing to run | none needed |
| `36-server-side-gaps.md` | Two server side gaps: an absolute route URL, and asking what is licensed | Feature, two of them | javap on the artifacts | none needed |
| `38-dark-mode-two-mechanisms.md` | Dark mode has two mechanisms, and the older one fails silently under Aura | Bug plus docs | Yes | `38-dark-mode.zip` |
| `39-imported-css-blocked-by-security.md` | CSS files pulled in with @import from a stylesheet are blocked by the default security setup | Bug | Yes | `39-imported-css.zip` |
| `41-gridaicontroller-time-column.md` | GridAIController cannot render a SQL TIME column | Bug | Yes, needs a key | `ai-live.zip` |
| `43-getpageheader-without-route-parameters.md` | MenuConfiguration.getPageHeader calls a title generator with no route parameters | Bug | Yes | `08-pagetitlegenerator-bean.zip` |
| `44-devloop-missing-hmr-line.md` | The dev loop drops its hmr line when a change set mixes Java with a stylesheet | Bug, output only | Yes | `10-devloop.zip` |
| `45-older-than-25-3.md` | Four older behaviours that cost a real application time | Bug, four older ones | Yes, tests | `45-older-behaviours.zip` |
| `46-ai-form-tool-call-loop.md` | A FormAIController turn can loop on get_form_state until the process is killed | Bug | Yes, needs a key | `ai-live.zip` |
| `47-ai-response-listener-without-session-lock.md` | withResponseListener is called without a session lock, and the failure is swallowed | Bug plus docs | Yes, needs a key | `ai-live.zip` |
| `48-ai-fill-on-the-calling-thread-hangs.md` | fill_form blocks for ever when a provider calls it on the thread it was handed | Bug | Yes, a test, no key | `ai-live.zip` |

## vaadin/flow-components, 3 issues

2 of them are defects, the rest are documentation or API requests.

| Draft | What it says | Kind | Reproducible | Project |
| --- | --- | --- | --- | --- |
| `16-emailfield-validation-message.md` | EmailField shows no message for a bean validation failure | Bug | Yes, a failing test | `16-emailfield-message.zip` |
| `20-grid-select-all-lazy.md` | A lazy Grid renders a select all checkbox that selects nothing | Bug | Yes | `20-grid-selectall-lazy.zip` |
| `40-upload-file-arrived-event.md` | No non-deprecated way to learn that a file reached an Upload whose handler belongs to a library | Feature | javap on the artifact | none needed |

## vaadin/web-components, 3 issues

2 of them are defects, the rest are documentation or API requests.

| Draft | What it says | Kind | Reproducible | Project |
| --- | --- | --- | --- | --- |
| `03-applayout-shift.md` | AppLayout paints its content at the full window width before reserving the drawer's space | Bug | Yes | `03-applayout-shift.zip` |
| `09-charts-styled-mode.md` | A Chart ignores the theme until styled mode is turned on, and the docs that list the style properties do not say so | Bug | Yes, needs a Charts licence | `09-charts-styled-mode.zip` |
| `15-formlayout-colspan-steps.md` | A field's colspan cannot vary across FormLayout responsive steps | Feature | Yes | `15-formlayout-colspan.zip` |

## vaadin/docs, 4 issues

0 of them are defects, the rest are documentation or API requests.

| Draft | What it says | Kind | Reproducible | Project |
| --- | --- | --- | --- | --- |
| `18-downloadhandler-no-session-lock.md` | Say on the download pages that the callback holds no session lock | Docs | Nothing to run | none needed |
| `30-docs-gaps.md` | Seven documentation gaps found while building a real application on 25.3 | Docs, seven gaps | Nothing to run | none needed |
| `31-observability-docs.md` | Three things the Observability Kit pages do not say | Docs | Three curl commands | none needed |
| `33-container-tokens-not-interchangeable.md` | The container background tokens are not interchangeable between Lumo and Aura | Docs | Read aura.css | none needed |

## vaadin/testbench, 1 issues

0 of them are defects, the rest are documentation or API requests.

| Draft | What it says | Kind | Reproducible | Project |
| --- | --- | --- | --- | --- |
| `37-testbench-cdp-blocked.md` | Say that the driver has to be unwrapped before CDP can be reached | Docs | Our own InvoicePrintIT | none needed |

