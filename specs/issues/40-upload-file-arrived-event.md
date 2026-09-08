REPO: vaadin/flow-components
TITLE: No non-deprecated way to learn that a file reached an Upload whose handler belongs to a library

---
### Description

`Upload.addSucceededListener` is deprecated for removal, in favour of a `TransferProgressListener`. The only way to register one of those is to pass it into the `UploadHandler` you construct.

So a component whose handler belongs to somebody else has no supported way for the application to learn that a file arrived. That is exactly what `AIOrchestrator.withFileReceiver(upload)` installs: the orchestrator sets its own in memory handler, and the documentation says the component must not already have one.

What is left are the negative events, `addFileRejectedListener` and `addFileRemovedListener`.

### Why it matters

Knowing that a file is staged is what a screen needs to enable a button, show a count, or say "ready to send". We wanted it to enable one button, and ended up keeping our own `UploadHandler.inMemory` and prompting the orchestrator by hand, which also meant giving up the file receiver integration.

### Expected

Keep a non-deprecated "a file arrived" event on `Upload` itself, or let `withFileReceiver` report what it took.

Found on 25.3.0-beta1.
