# Where this is, and what to do next

Written at the end of a session, for whoever opens the next one. The specifications are the contract; this file only says where the work stopped and what is worth knowing before starting.

## State

`./mvnw verify` is green: 293 tests, with a JaCoCo floor of 70 percent of lines that the suite clears at 77, default profile, no licence, no OpenAI key, no network. `./mvnw verify -Pit` adds the browser tier: 14 tests across six `IT` classes, against a local Chrome on port 8081. Twelve more tests really call OpenAI and are excluded from both runs by their `live-ai` tag; they pass, and the command that runs them is below. The working tree is clean and nothing has ever been pushed. 137 of 210 acceptance criteria are ticked, and every feature document ends with a **Still open** section saying what is not done and why. Those sections are the real backlog: they were written by reading the code, not by remembering it.

Since the previous session: the browser tier exists, the twelve defects in the UI report became `15-ui-repair.md` and every one of them is fixed. People and closures are `Crud`, the catalogue kept `GridPro` and got the four things Crud would have brought by hand, the dashboard is a `Dashboard` of spanning widgets, the order board's selection column is a checkbox wide again, a product opens over the catalogue instead of replacing it, and the diagnostics panels share a row.

The nine criteria that were unbuilt rather than untested are now built: the kitchen flashes a ticket somebody else moved and admits when it may be behind, the conversation is live through a shared signal per order, the startup check names every required preview flag with the file that sets it, and the theme belongs to the person rather than to the session. What is left of that list is one thing, KIT-06, and it is left because it needs a notion of expected preparation time the domain does not have.

Three things closed after the rebuild: the dashboard's query budget, the slot test that proved nothing, and the coverage floor. Each was checked by breaking the thing it guards and watching it fail.

**The history was rebuilt.** The specifications had contradicted themselves from the first commit: `00-overview.md` ruled out free fallbacks beside commercial components and a core only build in this branch, while `11-dashboard.md` specified a `commercial` profile with table fallbacks and `06-ai.md` specified a mock provider answering from cassettes, seventeen lines below the paragraph saying nothing is replayed. Everything since had been discovering that, late. So the whole specification now lands in one commit, corrected and with every box unchecked, and the boxes fill in the commit that builds them: 0 at the specification, 31 after Epic 01, 298 after Epic 14, 335 at the end. One commit per specification, the repair pass last, and the last free substitute, the paste and parse parser on the phone order screen, is gone.

## What to do next, in order

1. **Open the findings as issues.** `specs/FEEDBACK-25.3.md` now holds more than a dozen and not one has been reported. Several have a reproduction small enough to file today, listed near the top of that file. Two arrived this session: a grid paints the screen reader only sentence it uses when select all is unavailable, and it sizes the column with it; and a component set as a grid column header is invisible to the browserless finder. This is the shortest path from this repository to something a Vaadin team can act on, and it is the only item here that needs somebody's permission rather than somebody's time.
2. **The eleven unwritten browserless classes**, listed honestly in `SpecConsistencyTest.NOT_WRITTEN_YET`. The live tracking page, concurrent edit, the stale kitchen ticket, the multi user conversation, four locale classes and the responsive board. Deleting a name from that set is how each gap closes.
3. **The rest of the closed `IT` list** in `specs/08-testing.md`. Six of the eighteen are built. `AuraDarkModeIT`, `KitchenBoardPushIT` and `SmokeIT` are the three worth the most: the first would have caught the Charts theming bug on its first run, the second is the only way to prove a shared signal reaches two browsers, and the third is the one that fails when a route moves.

After that the per feature backlog, heaviest first: the kitchen board's live claims (nine open), the order board (mostly browser tier), the language criteria in `01-foundation.md`, the conversation that was specified as live and written as a query, and invoicing's print and locale work.

## Things that will cost you an hour if you rediscover them

- **This is the full featured version and it expects a Pro subscription.** No licence checks, no fallbacks, no dual implementations. See Licensing in `specs/00-overview.md` before proposing a `commercial` profile: that decision is made, and a core only version would be a separate branch.
- **There is no mock assistant, and that is deliberate.** A provider that replayed recorded conversations was removed this session. It had "proved" two acceptance criteria that turn out to be false against a real model, and it had never exercised the case a real model actually produces. Before writing anything that stands in for the model, read Providers in `specs/06-ai.md`.
- **A live AI test is asserted by shape, never by wording.** The model picks its words, its column names and how much it manages in one turn. Pinning those is testing OpenAI's mood, and it will flake. What is a contract: a line is a product the bakery sells in a quantity it allows, a query reads only the three views, a chart has a series and a title.
- **Findings are not finished when they are written down.** Every row in `specs/FEEDBACK-25.3.md` is meant to become an issue in the repository that owns it, and the file feeds the hackathon report. The file says which repository.
- **No hacks.** Where the platform leaves no reasonable way, the workaround is small, written down in `specs/FEEDBACK-25.3.md`, and reported. Read that file before fighting something: several traps are already in it, including three that cost a morning each.
- **Browserless has blind spots**, and they are not theoretical. A `PageTitleGenerator` bean renamed every page in the application and the browserless tier resolved the right titles throughout. Components inside a Grid component column are invisible to `find`, and so are components set as a column's header: reach those through `column.getHeaderComponent()`. A grid can have exactly the columns you assert and still render nothing, because the renderer only runs when a row is drawn: use `GridKt._getFormattedRow`. When a claim is about what a browser shows, browserless agreeing is not evidence.
- **A CSS grid inside a `VerticalLayout` is one column wide.** The layout aligns its children to the start, so a grid child is as wide as its own contents and `auto-fit` has nothing to fit into. It cost the same half hour twice, on the dashboard and on the diagnostics view. `width: 100%` on the grid, every time.
- **The dev loop**: read `.agents/skills/vaadin-devloop/SKILL.md` first. Never run the application through Maven and through the daemon at the same time, and remember it compiles Java only, so the Kotlin about view needs a restart. Its `status` reports errors after every restart that has a browser open; they are a Flow push reconnect race and not yours, and the row explaining that is in the feedback file.
- **After a dev loop session, `./mvnw clean verify` before believing a red build.** The daemon compiles without the `-parameters` flag the Spring Boot parent sets, so `target/classes` goes stale and three Spring Data tests fail pointing at repository code nobody touched. The row is in the feedback file.
- **Never `git add -A` at or before `Set up the build and the agent tooling`.** `.gitignore` arrives in that commit, so at any earlier point a bulk add sweeps in `node_modules`, `target` and the generated Vite files. Rewriting history across that boundary is where this bites.
- **Three branches carry this work.** `manolo` is the one to build on: twenty seven commits, one for the brief, one for the whole specification, two for the build and the dataset, one per epic, seven for the repair pass and one for the records. `manolo-before-spec-rewrite` is the history as it was actually built, before the specification was made consistent, and `manolo-dirty-history` is older still. Both are archaeology and both are safe to delete.
- **A shared signal whose environment has been replaced accepts writes and drops them.** It is the browserless tier, not the application: two classes touching the same signal bean, the second inheriting the first's dead signal environment, and every `update` and `remove` from the second is silently ignored while reporting success. It presents as application bugs, convincingly enough that the board was rewritten twice before the cause was found. Any class that writes to a shared signal needs `@DirtiesContext(BEFORE_CLASS)`. The row in `FEEDBACK-25.3.md` has the measurements.
- **The browser tier's sign in was the flake, and it is fixed.** The login overlay's hidden CSRF field is empty until the component submits the form itself, so `form.submit()` posted no token, Spring declined it, and the browser landed on the open landing page looking signed in. `BrowserIT` now fills the token from the `_csrf` metas, confirms the session with a request to a protected route, retries once, and signs in again if a later navigation bounces to the login screen. Two failed runs in three before, eight clean runs in a row after. The row is in `FEEDBACK-25.3.md`.
- Nothing has been pushed. `manolo` is a normal branch off `main`, so the first push is an ordinary one.

## Commands worth having

| What | Command |
| --- | --- |
| The gate | `./mvnw verify` |
| The assistant, for real | `export OPENAI_API_KEY=...` then `./mvnw -Pai` |
| Every live AI test | `./mvnw test -Pai -Dsurefire.excludedGroups=` |
| One of them | `./mvnw test -Pai -Dtest=GridAssistantBrowserlessTest -Dsurefire.excludedGroups=` |
| The browser tier | `./mvnw verify -Pit` |
| One browser test | `./mvnw verify -Pit -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=DashboardLayoutIT` |

Without the profile, or without the key, the assistant is off: one warning at startup and a red panel on each of the three surfaces. Everything else on those screens works.
