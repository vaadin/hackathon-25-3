# Vibe session 02: the issue pipeline

A second conversational session, on 7 and 8 September 2026, spent almost entirely on one question: is each thing we wrote down actually true, and can somebody else see it in a few minutes. The application changed only where the answer forced it to.

## The setup

The same as session 01: the dev loop running, a headed Chrome driven through Playwright for anything with a visual surface, and the specifications as the contract. What was different is the unit of work. Instead of a screen, the unit was a finding: read the row, build the smallest project that could show it, run it, and then write down what happened rather than what we remembered.

## What that produced

Nineteen reproducer projects, one or two classes each, and thirty nine issue drafts. Every draft that can be reproduced links a zip of its project, so a reader downloads one file and runs one command.

The interesting number is the other one. **Eleven findings died and four were corrected**, and every single one of them was killed or corrected by building the project, never by rereading the row.

| Finding | What the project showed |
| --- | --- |
| `bindChildren` does not exist | It exists. Our forty line adapter was deleted and three views now call the platform. The original compile error was about the mapper's argument type |
| `Markdown.getContent()` throws on a bound value | It does not. It was a `Button` rebound on its second attach, which is a different finding |
| `LazyDataView.getItems()` divides by zero | It counts. The board's own test now asserts 265 through it |
| A `@Menu` order change is not live | A page reload applies it. What is true is smaller and the loop already prints it |
| An exception inside `Signal.effect` is invisible | It is logged at error level with its stack, which a log capturing test showed in two lines |
| The login overlay's CSRF field is empty and dangerous | The field is empty and harmless: a scripted submit signs in either way, because CSRF is not enforced on that POST in a default setup |
| `LicenseChecker` answers the same for a product that does not exist | Kept as a note, not as an issue: nothing in it is a defect |
| TestBench blocks CDP | It does not. Unwrap the proxy with `WrapsDriver` and CDP works, which is what our own print test has been doing all along. Two tests, green, in a real Chrome |
| The AI field marker's badge does not open its popover | It opens, with a real mouse click through the driver |
| A GridPro cell does not enter edit mode from a double click | It does, with a real double click through the driver. Two findings, one cause, one afternoon: both were about a gesture nobody performed |

And the three corrections, which changed the application as well as the report:

| Finding | The correction |
| --- | --- |
| A lazy Grid cannot have a select all checkbox | Half right, and the half we got wrong was ours. `VISIBLE` does work, through `setItems` and through `setItemsPageable`, and selects every row the count callback reports, so the board offers select all now. The rest, that `HIDDEN` renders an inert checkbox, was closed by a maintainer as works as designed: hidden means `visibility: hidden`, which keeps the layout box, so what we measured was geometry and what we clicked was a script clicking something no user can reach. https://github.com/vaadin/flow-components/issues/10063 |
| `Upload` has no non-deprecated arrival event | Three listeners are not deprecated, and one of them, `ProgressUpdateEvent`, carries the file name and the byte counts, so `readBytes == contentLength` is a working arrival signal. Poor, and not nothing |
| Dark mode: the theme attribute does nothing | It does nothing under Aura. Under Lumo it is the mechanism that works, and the colour scheme moves only the text |
| A tool whose schema is not valid JSON is dropped | It is not. Both tools were offered and both were called, and the turn answered correctly. What is real is that the error names Jackson and never names the tool |

## What changed in the application

- **The board offers select all.** Every order the filter matches, 265 on this dataset, and a bulk action over more than twenty five of them asks first with the count in the sentence. A new browserless test asserts the selection equals what the grid lists, read through the grid's own data view.
- **The accessibility string came back.** `selectAllUnavailable` was emptied to buy a narrow column. It is a translated string again, because the grid never writes it while a checkbox is there.
- **`Children.java` is gone**, replaced by `bindChildren` in three views.
- The CSV export takes a snapshot on the UI thread and answers `DownloadResponse.error` when the export fails, which is unrelated to any bug we could reproduce and is right anyway.

## The live AI round

The six findings that had only ever been seen inside this application were run again in a bare project, against `gpt-4o-mini`, with a licence and a key on the machine. Four were confirmed with what the run printed, one was corrected and one was withdrawn, and the round turned up three findings nobody had written down:

- **A turn looped on `get_form_state` 150 times in 2 minutes 11 seconds** and stopped because the application was killed. There is no cap on tool call rounds. Ask a form for a field it does not have and this is what happens, and it is somebody's money.
- **`withResponseListener` holds no session lock**, so the natural body of the listener throws and the orchestrator swallows it. That is why our turn meter read zero for an afternoon.
- **`fill_form` called on the provider's own thread never returns.** It waits for the lock that thread already holds. Reduced with a fifteen line provider and a `@Timeout`, which is the only reason the test ends rather than hangs.

## What is still open

Two findings are real here and not reduced anywhere else: a push reconnect logged as an error during a restart, and a signal bound text that cannot be rebound.

Nothing has been posted. Thirty nine drafts, each with a repository and a title, are in `specs/issues/`, and the command that posts one is in that directory's README.

## The lesson, in one line

A finding that has never been reproduced is a hypothesis, and roughly a quarter of ours were wrong. The one that got through the pipeline anyway died the same way as three of the others: a gesture nobody performed. A layout box is not visibility, and a scripted click is not a click.
