# Build report

How this application was built: what was done, what it cost, what beta1 got in the way of, and what is not finished.

Specifications written 08:45 to 09:30 UTC. Implementation 09:34 to 11:40 UTC on 4 September 2026. A second day, 5 September, went on what the first day left: the look of the application, the assistant that was never wired, and bringing the specifications back in line with the code.

## Timeline

| Epic | Wall clock | Result |
| --- | --- | --- |
| Specifications | 45 min | 11 cross cutting documents, 14 feature documents, 14 epics |
| 01 Foundation | 18 min | Boots, secured, themed, two languages, 8 tests |
| 02 Domain and dataset | 38 min | 17 entities, 8 services, 1400 order dataset, 38 tests |
| 03 Storefront | 35 min | Landing, catalogue, product pages, 48 photos, 59 tests |
| 04 Cart and slots | 25 min | Cart signals, calendar with capacity metadata, 71 tests |
| 05 Checkout and tracking | 35 min | Routed steps, breadcrumbs, validation groups, 80 tests |
| 06 Order board | 40 min | Lazy grid, the hidden column proof, bulk actions, 96 tests |
| 07 Admin | 40 min | GridPro, switches, modular upload, clipboard, users, closures, 112 tests |
| 08 Kitchen board | 20 min | Shared signal board and Table summary, 116 tests |
| 13 Observability | 30 min | Service event bus recorder, diagnostics, profile, 122 tests |
| 12 AI | 35 min | Mock provider, policy layer, turn meter, understudy, 138 tests |
| 10 and 11 Invoicing and dashboard | 30 min | Print document, CSV, four panels with a free rendering, 148 tests |
| 09 Conversation | 15 min | One to one bubbles, attachments, escaping, 152 tests |
| 14 Polish | 25 min | Kotlin about page, consistency checks, docs, CI, 161 tests |

Total: about six hours, of which two are specifications and four are the application.

## What exists

| Thing | Count |
| --- | --- |
| Main sources | 116 files, 10456 lines, of which one Kotlin file and one compiled only under the `ai` profile |
| Tests | 58 files, 5451 lines, 218 tests, all green |
| Specifications | 42 documents, 4018 lines |
| Stylesheets | 14, all under `META-INF/resources/styles` |
| Demo dataset | 15990 lines of generated SQL: 1400 orders, 48 products, 220 customers, about 1000 invoices |
| Product photos | 48 freely licensed WebP images with their credits, about 2 MB |
| Commits | 59 |

Every test runs with no commercial licence, no OpenAI key, no browser and no network.

## Decisions taken without asking

| Decision | Reason |
| --- | --- |
| `spring.jpa.defer-datasource-initialization=false` | The schema is explicit and Hibernate validates against it, so the scripts must run before validation. With the Boot default the boot fails with "missing table app_user" |
| `drop all objects` at the top of the H2 schema | An in memory database outlives a Spring context reload, so without it a `@DirtiesContext` test takes 53 unrelated tests down with it |
| The `User` entity landed in epic 01 rather than 02 | Security needs a user store and the alternative was throwaway in memory users |
| A `Translations.bindText` helper | Binding text to `UI.localeSignal()` is the only way to switch language without a reload. Calling `getTranslation` in a constructor freezes the language at construction time |
| Read models everywhere a view shows an aggregate | Views were walking lazy collections. Three separate `LazyInitializationException` bugs came from it, all caught by tests rather than by a demo |
| A `TestLogin` helper instead of `@WithMockUser` | `@WithMockUser` is not reliable once a servlet container test has run in the same JVM. See the feedback file |
| Photos from Wikimedia Commons rather than generated | Clean licence, attributable, and real food photography is the first impression of the storefront |
| Commercial AI artifacts on the default classpath | They check their licence when a controller is constructed, not when they are on the classpath, so the default build compiles and the fallback path runs |

## Bugs the tests caught in our own code

| Bug | How it would have looked in a demo |
| --- | --- |
| Row details and the order timeline walked lazy collections from the view | `LazyInitializationException` the first time somebody expanded a row |
| Issuing an invoice from a detached order | Marking an order picked up threw instead of producing the invoice |
| `List.of` with an inactive filter | A `NullPointerException` inside a signal effect, and the board rendered empty with one bare line on stderr |
| The paste and parse fallback matched too much | "croissants" matched two products and "cake" matched three, so a phone order gained items nobody asked for |
| `navigate(String)` with a query string | Placing an order threw instead of navigating to the tracking page |
| The dark mode toggle set a theme attribute Aura does not read | The button did nothing, and the test asserting the attribute passed. Found by clicking it in a browser, fixed with `Page.setColorScheme`, and the test now asserts the scheme |
| The card grid did not fill its container | A `VerticalLayout` aligns children to the start, so the grid shrank to its content and showed two columns on a 1600 pixel screen instead of six |

## Problems in 25.3.0-beta1

All of them, with the workaround and the file it lives in, are in `specs/FEEDBACK-25.3.md`. The ones worth naming here:

| Problem | Impact |
| --- | --- |
| `bindChildren` is documented for 25.3 and absent from the artifact | Every list rendering goes through an adapter written for the purpose |
| `peek()` on a computed signal throws | Reading a computed value outside a reactive context needs `Signal.untracked` at every call site |
| `LazyDataView.getItems()` divides by zero on a pageable backed grid | The documented way to read a lazy grid in a test does not work with the documented way to fill one from Spring Data |
| Static CSS imported from a stylesheet is redirected to the login view | The application renders completely unstyled and nothing appears in the log |
| An exception inside `Signal.effect` is one line on stderr with no stack | A broken view looks merely wrong rather than broken |
| `mvn vaadin:install-dev-cli` does not exist | Declare `flow-maven-plugin` and use `mvn flow:install-dev-cli` |

`specs/FEEDBACK-25.3.md` also lists what worked exactly as advertised, which is most of it.

## Deviations from the specifications

| Specification | What was built | Why |
| --- | --- | --- |
| `features/07-admin.md` asks for category, allergen and location editors | Products, users and closures were built. The other three were not | Closures are the ones that visibly change the public calendar. The rest is ordinary CRUD over three small tables |
| `features/06-order-board.md` asks for sticky relative date group headers | The board sorts by pickup slot and shows the day in the slot column | Group headers need a renderer that browserless cannot assert on |
| `features/12-ai.md` asks for the Form AI controller filling the phone order | The orchestrator, the policy layer, the meter and the understudy are wired. The controller is not attached to the form yet | The free half proves the interesting parts. Attaching the controller is a small step on top and it is still open |
| `features/09-conversation.md` asks for live delivery through the shared signal | Messages are stored, rendered and escaped. They refresh on navigation rather than instantly | The shared signal work is done in epic 08 and this is one wiring step away |

## What is not done

Thirteen tests the specifications name are not written, and they are listed explicitly in `SpecConsistencyTest.NOT_WRITTEN_YET` so the gap cannot be forgotten: live tracking updates, concurrent edit, responsive board, stale ticket, multi user conversation, four locale checks for derived text, transient text and titles, and the three that need the commercial form controller.

The browser tier, twelve `IT` classes, is specified and not implemented. Those are the ones that need a licence and a display: upload drop zone, clipboard paste, GridPro cell editing, date picker rendering, print CSS, PWA install, Charts, dark mode contrast, two browsers on the kitchen board, and a smoke test.

## Resources

| Measure | Value |
| --- | --- |
| Wall clock | 2 hours 6 minutes for the application, plus 45 minutes of specifications, plus a second day |
| Model | Claude Opus 5, one session per day, no parallel agents |
| Tests | 218, all green, about 90 seconds for a full run |
| External services | Wikimedia Commons for 48 photos, and OpenAI on the second day, from one tagged test and by hand in the browser. The default build still calls nothing |

Token usage for the session is in the client's own ledger rather than here, because nothing inside the build measures it. What the build itself cost is above.

## Second day, 5 September

| Work | Result |
| --- | --- |
| Design pass | Shared `.page-block`, `.panel` and `.page-grid` replacing five per view card definitions. Opening hours and about rebuilt as panels in a grid. The kitchen board rebuilt around `MasterDetailLayout`: full height columns that scroll inside themselves, counts in the headers, and the production summary as an overlay rather than below a fold nobody scrolls past |
| The assistant that was never wired | The `ai` profile, `src/ai/java`, and the three line factory that turns Spring AI's `ChatModel` into the `SpringAILLMProvider` that ships free inside `vaadin-ai-core-flow`. `@Push`, without which streamed tokens never reach the browser. `LiveAssistantTest`, tagged `live-ai` and excluded by default, which really calls OpenAI |
| The licence row on the about page | It said the same thing on a licensed machine and on one with no key, because it worked by constructing a GridPro and 25.3 does not check a licence there. Now two rows: what is in the build, and what the checker says about this machine, in three states. Verified by running with `user.home` pointed at an empty directory, and again with a deliberately invalid key |
| Specifications brought back in line | Every acceptance criterion and every epic task read against the code and the suite. 100 of 184 criteria and 177 of 272 tasks are ticked, and each document now carries a section naming what is not done and why. Several claims that were simply false, three profiles that do not exist, a JaCoCo floor that is not in the pom, a nightly CI job nobody wrote, are corrected in place rather than quietly dropped |

The honest summary of that last row: the first day built more than it proved, and the specifications had drifted into describing an application slightly better than the one in the repository. They now describe this one.
