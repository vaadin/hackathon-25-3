# Vibe session 02: the issue pipeline

A second conversational session, on 7 and 8 September 2026, spent almost entirely on one question: is each thing we wrote down actually true, and can somebody else see it in a few minutes. The application changed only where the answer forced it to.

## The setup

The same as session 01: the dev loop running, a headed Chrome driven through Playwright for anything with a visual surface, and the specifications as the contract. What was different is the unit of work. Instead of a screen, the unit was a finding: read the row, build the smallest project that could show it, run it, and then write down what happened rather than what we remembered.

## What that produced

Nineteen reproducer projects, one or two classes each, and thirty nine issue drafts. Every draft that can be reproduced links a zip of its project, so a reader downloads one file and runs one command.

The interesting number is the other one. **Eight findings died and three were corrected**, and every single one of them was killed or corrected by building the project, never by rereading the row.

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

And the three corrections, which changed the application as well as the report:

| Finding | The correction |
| --- | --- |
| A lazy Grid cannot have a select all checkbox | `VISIBLE` works, through `setItems` and through `setItemsPageable`, and selects every row the count callback reports. `HIDDEN` is the setting that is not honoured: the checkbox is rendered anyway and ticks without selecting. The board offered exactly that inert control for as long as it asked for it to be hidden |
| `Upload` has no non-deprecated arrival event | Three listeners are not deprecated, and one of them, `ProgressUpdateEvent`, carries the file name and the byte counts, so `readBytes == contentLength` is a working arrival signal. Poor, and not nothing |
| Dark mode: the theme attribute does nothing | It does nothing under Aura. Under Lumo it is the mechanism that works, and the colour scheme moves only the text |

## What changed in the application

- **The board offers select all.** Every order the filter matches, 265 on this dataset, and a bulk action over more than twenty five of them asks first with the count in the sentence. A new browserless test asserts the selection equals what the grid lists, read through the grid's own data view.
- **The accessibility string came back.** `selectAllUnavailable` was emptied to buy a narrow column. It is a translated string again, because the grid never writes it while a checkbox is there.
- **`Children.java` is gone**, replaced by `bindChildren` in three views.
- The CSV export takes a snapshot on the UI thread and answers `DownloadResponse.error` when the export fails, which is unrelated to any bug we could reproduce and is right anyway.

## What is still open

Four findings are real here and not reduced anywhere else: a push reconnect logged as an error during a restart, a signal bound text that cannot be rebound, a tool call on the UI thread deadlocking `FormAIController`, and a GridPro cell that ignores a synthesised double click. Six more need an OpenAI key or a commercial licence to see at all, and each says so at the bottom of its draft.

Nothing has been posted. Thirty nine drafts, each with a repository and a title, are in `specs/issues/`, and the command that posts one is in that directory's README.

## The lesson, in one line

A finding that has never been reproduced is a hypothesis, and roughly a quarter of ours were wrong.
