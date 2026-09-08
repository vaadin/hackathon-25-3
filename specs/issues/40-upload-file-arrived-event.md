REPO: vaadin/flow-components
TITLE: No non-deprecated way to learn that a file reached an Upload whose handler belongs to a library

---
### Description

`Upload.addSucceededListener` is deprecated for removal, in favour of a `TransferProgressListener`. The only way to register one of those is to pass it into the `UploadHandler` you construct.

So a component whose handler belongs to somebody else has no supported way for the application to learn that a file arrived. That is exactly what `AIOrchestrator.withFileReceiver(upload)` installs: the orchestrator sets its own in memory handler, and the documentation says the component must not already have one.

What is left is less than that. `javap` on the 25.3.0-beta1 artifact, every listener `Upload` declares:

| Listener | Deprecated |
| --- | --- |
| `addAllFinishedListener` | no |
| `addProgressListener` | no |
| `addFileRemovedListener` | no |
| `addFailedListener` | `@Deprecated(since="24.8", forRemoval=true)` |
| `addFinishedListener` | the same |
| `addStartedListener` | the same |
| `addSucceededListener` | the same |
| `addFileRejectedListener` | the same |

So three survive, and neither of the two that fire on arrival says what arrived: `AllFinishedEvent` declares nothing but its constructor, so it means "the queue is empty now", after failures as much as after successes.

`ProgressUpdateEvent` is the one thing left with per file information: it carries `getFileName()`, `getReadBytes()` and `getContentLength()`, so `readBytes == contentLength` is a working, non deprecated "this file arrived". Comparing two longs on a progress event to learn that an upload finished is not an API, it is a trick, and it is what an application is left with.

### Why it matters

Knowing that a file is staged is what a screen needs to enable a button, show a count, or say "ready to send". We wanted it to enable one button, and ended up keeping our own `UploadHandler.inMemory` and prompting the orchestrator by hand, which also meant giving up the file receiver integration.

### Expected

Keep a non-deprecated "a file arrived" event on `Upload` itself, or let `withFileReceiver` report what it took.

### How this was checked

```
javap -v -cp vaadin-upload-flow-25.3.0-beta1.jar com.vaadin.flow.component.upload.Upload
javap -cp    vaadin-upload-flow-25.3.0-beta1.jar com.vaadin.flow.component.upload.AllFinishedEvent
```

Found on 25.3.0-beta1.
