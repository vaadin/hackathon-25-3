# Vibe coding, session 1

What was changed while working the application view by view with the dev loop running and a browser open, and what that way of working found. Written at the end of the session. The specifications remain the contract: this file records the session, not the requirements.

## The conditions

Four things were set up before any code was touched, and every entry below depends on them.

**The dev loop was the only way the application ran.** `.vaadin/vaadin-dev start` once, then `apply` after every edit, `restart` when the loop said a restart was needed, and `status` whenever the screen disagreed with the code. Never Maven and the daemon at the same time. The three rules that mattered in practice: open the browser before the first `apply`, because a CSS push needs a page already connected; never batch a translation bundle with a Java edit while iterating, because one resource turns a 0.8 second hot swap into a 7 second restart; and remember that the loop compiles Java only, so the Kotlin about view is invisible to it.

**A single headed browser was shared with the user.** The user clicked, typed and looked at the screen. The agent navigated, took screenshots, read the accessibility tree and measured computed styles in the same tab. That is what made the loop conversational: the user could say "there is an empty band on the right" and the agent could answer with the box model rather than with a guess. It also produced the session's first false lead, when a page appeared to navigate itself and the dev loop was blamed for what was the user clicking, so the rule was written down: an unexplained state change in a shared browser is a person first. `browser_resize` was the second trap, because it pins the page viewport for that page's whole life, so responsive checks happen in a tab of the agent's own and never in the tab somebody is watching.

**Every oddity was recorded as it happened.** A running observations log during the session, promoted into `FEEDBACK-25.3.md` when a finding held up, with the reproduction, what was expected, what happened, and the file the workaround lives in. Nothing was recorded as reproducible without reproducing it, and two findings were withdrawn when a reproducer killed them.

**The user set the tempo and the standards.** Changes in seconds, not minutes. No tests until everything is implemented, even at the cost of a red suite, tests last. Ask before declaring something impossible, and when it really is impossible, report it rather than working around it quietly. Correct the agent hard when it builds the wrong thing: it happened once, over a column chooser, and the correction was the right one.

## What the working method turned out to be

**Measure, do not look.** Almost every UI fix in this session came from reading a number in the browser and not from a screenshot. A badge whose colour rule was half applied was Lumo's `[theme~="badge"][theme~="contrast"]` at specificity 0,2,0 beating a single class. A message input that looked misaligned had 40 pixels of its own padding. A form that looked cramped had 20 pixels of vertical padding per field, 120 pixels across six fields in a phone width panel, and taking it away cost no label room because the label lives inside the field's own block: 77 pixels a field became 57. A responsive step that looked wrong was measured against the window instead of against the component, which is the wrong number by the width of an open drawer.

**Ask the naive question first.** Proving that a control cannot live in a grid's select all header cell took six experiments and a `javap`, and the conclusion was correct and useless, because the real question was "how does a grid offer a menu at all" and the answer, `grid.addContextMenu()`, was one call away in the documentation server. Every probe written after forming a theory tests the theory rather than the requirement. The rule is now in `.claude/commands/story/verify-ui.md`.

**Read the log after every AI turn.** The assistant's own account of what it did is not evidence. Two of this session's real defects were invisible on screen and plain in `target/devloop/app.log`.

## What was implemented

### The shell and the drawer

Shop, Operations and Administration became parent items with their children nested under them, in one `SideNav`. Which parent is open follows the role rather than the route: nobody logged in gets everything closed, a baker or a barista gets Operations, an admin gets Administration. Visiting `/` no longer opens Shop by itself, which was the behaviour the user asked about and it turned out to be the platform opening the group that contains the current route.

### The storefront

Product photographs at 0.8 opacity. The filter row became a `FormLayout` with two responsive steps, wide and narrow, after the user established that there is no column span in the old API worth using here. Two bugs fell out of it: the badge rule needed `.product-card__badge[theme~="badge"]` to beat Lumo's own two attribute selector, and the sticky filter bar leaked photographs through the scroll container's padding band, fixed with a negative `top` matching that padding. A photograph now opens its product, which is a new acceptance criterion and a new test name.

### Opening hours and about

Both tables fill the width of their column, and the set of columns fills the width of the page.

### The order board

Opening a row's detail band closes any other: one band at a time, and Escape collapses it. The right hand panel no longer needs a double click. It opens from a new first data column, as narrow as an icon, frozen and centred, with an edit icon in it. Each state has a colour, applied as a data attribute on a chip so the palette lives in CSS. The column chooser moved out of a control of ours and into the table's own header, through `GridContextMenu` with a dynamic content handler that answers only when the menu was opened on the header rather than on a row. Putting it in the select all cell instead is not possible, and that is written up door by door in `FEEDBACK-25.3.md`: `GridSelectionColumn` extends `Component` rather than `AbstractColumn`, so it has no header API, and no header row has a cell for it.

### The order panel, rewritten

The panel is a phone width column with a body that scrolls and a footer that does not, and the footer carries the live total and one Save. The customer's details became chips that flow on one line with an icon each, short dates, first names, no repeated labels. Product rows are compact, never overflow, their delete crosses line up, and the trailing empty row shows a disabled zero price. The state became a combo box whose field takes the colour of the chosen state, and it stages the change rather than committing it: the user found four separate faults in the first version, that it saved on selection, that it offered fewer states each time, that it could not go back, and that it showed the raw enum name and lost the colour, and all four are gone. The item label generator is now set before the value, because `Translations.onLocale` runs on attach, which is after `setValue`. The history and the messages are one `MessageList`, one line per entry, with a transition written as what changed. Saving the note by reference in its own service call removed an `OptimisticLockingFailureException` that appeared the moment there was a single Save. A cancelled order can be reopened to `CONFIRMED`. Attachments were removed from the composer because they did not work, and are recorded as not done.

### The assistant

The assistant is on by default: the `ai` profile activates on the presence of `OPENAI_API_KEY` rather than on a flag somebody has to remember. The board assistant answered nothing until H2 was told `CASE_INSENSITIVE_IDENTIFIERS=TRUE`, because the model writes lower case identifiers and H2 folds unquoted names to upper case, which PostgreSQL never did. The panel is wider and its Send is an icon, which took `::part(label)::after` because a shadow host's own `::after` is not slottable and never paints.

### The counter order screen

The screen was called a phone order and is now a counter order, because the same form takes a call, an email or somebody dictating at the counter, and how the order arrived is a field on it: Phone, Email or Counter, defaulting to the telephone. `EMAIL` was added to `Channel` and to both schemas. That field sits outside the container the form controller walks, so the model neither reads nor writes it.

Everything else on that screen followed five observations from the user, all of them fair.

| What was wrong | What it is now |
| --- | --- |
| Nothing filled the form from what the customer said | A primary Fill the form button under the paste area. The area was inert before: the only thing that reached the model was a chat box |
| No way to hand over a photograph of a handwritten note | A camera button beside it. The image is taken in memory and rides with the pasted words in one turn through `prompt(message, attachments)`. With nothing typed, the prompt says to read the note |
| The paste area was a fixed block and not full width | Full width, one row growing to eight as they talk. An explicit height defeats the growing, so there is none |
| A Message field and a Send button nobody could explain | Gone. One place to type, one control that acts on it. The message list stays as the transcript, which is where the assistant's refusals are read |
| The provider named in a badge over the panel | Gone. It is on the about page, which is where a reader looks for what the build is made of |

Two real defects surfaced while proving that screen works, both of which had been invisible.

The parameter schema of `propose_pickup_slot` was not valid JSON, because `\"soonest\"` in a Java text block emits an unescaped quote. Spring AI logged `Failed to parse tool schema` and dropped that tool on every request, so the model never had it: it filled the name, the telephone and both lines, announced a collection time, and left the pickup empty. Fixed with single quotes, and reported, because a permanent misconfiguration deserves to fail when the orchestrator is built rather than to be logged once per request with nothing reaching the screen.

Then, with the tool alive, the model called it before adding the lines, and the cake's lead time made the picker throw the date away. That is fixed three ways: the prompt says the lines go in first, the tool reads the fields back after writing them and refuses when the write did not take, and the end of every turn says so in a notification when there are lines and no pickup. Verified end to end: Friday the 11th at 17:00 in the form and in the answer, one turn, 3,879 tokens, 1.94 cents.

Both AI transcripts also render markdown now, because every model writes it whether or not it was asked to, and a message list shows text.

## Findings recorded this session

Each of these is a row or a section in `specs/FEEDBACK-25.3.md`, with its reproduction.

| Finding | Why it matters |
| --- | --- |
| A `@Menu` change reports `Stable` and is not live | The headline finding. The tool's promise is that it never over claims and escalates to a restart instead, and here it hot swapped, reported success, and the drawer kept the old order until a restart |
| The dev loop's compiler does not use the project's compiler flags | Recorded earlier as three failing tests after a session without `clean`. Upgraded this session, because a `pom.xml` edit made the daemon recompile the whole module and the running application started answering `/orders/new` with a Spring Data failure about `-parameters`. `./mvnw compile` does not fix it, since the daemon's classes are newer than the sources: it takes `clean` |
| A tool with an invalid parameter schema is dropped and the turn carries on | Nothing reaches the UI or the response listener, and the model narrates the work it could not do |
| An `hmr:` line goes missing when a change set mixes Java with a stylesheet | The push happened, verified in the page, and the output never said so |
| The reference table is pessimistic about what a JetBrains Runtime absorbs | Being told to expect a restart makes you batch edits to save restarts that were never going to happen |
| A push reconnect during a restart is logged as an application error | Existing row, softened from "every restart" to "often", with a clean restart as the counter example |
| A lazy grid cannot have a select all checkbox, and cannot be given one | A section rather than a row, written door by door, and corrected in place once `GridContextMenu` proved the wider claim wrong |
| A `MessageList` handed to the orchestrator renders markdown as text | Every documented example passes a bare `new MessageList()`, and every model writes markdown |
| Knowing that a file reached an `Upload` needs the handler a library owns | The non deprecated path is a `TransferProgressListener` on the handler you construct, so a component whose handler belongs to the orchestrator has no supported way to say a file arrived |
| `FormLayout` colspan and `setMaxColumns` | Recorded while building the storefront filter row |
| A documentation gap and a suggestion about the loop's output vocabulary | Both from the sessions above |

Two entries were withdrawn when their reproducers failed, and both are kept as struck through rather than deleted, because a finding that turned out to be wrong is worth as much as one that was right.

## What is not done

**Tests have not been run since the first Java edit of this session, deliberately.** That was the user's instruction and it stands. Expected fallout, from reading the diff: the board's action button assertions, `SanitizationTest` against the `SafeHtml` change, `BoardToolbarBrowserlessTest` against the chooser move, anything asserting the old panel structure, and `AssistantOffBrowserlessTest`, whose "offers nothing to type into" now has to mean the fill button and the paste area rather than a message input. The safe order for the gate is `stop`, then `clean verify`, then `start`, because a `clean` also throws away the frontend dev bundle and the first page afterwards takes about two minutes.

**Owed spec updates.** The cancelled to confirmed transition in `specs/04-security.md`, the conversation's attachments and clipboard paste as not done, the conversation no longer being closed on the staff side, and the panel's fixed phone width. The AI documents are already updated to the screen as it now behaves.

**One rename.** The counter order screen is still `PhoneOrderView`. The change is mechanical and touches six test files, so it belongs in the test pass.

**One row still owed to the feedback file**, the gap where an assistant answers nothing and the turn meter stays at zero with no error anywhere.
