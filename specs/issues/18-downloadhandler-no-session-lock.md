REPO: vaadin/docs
TITLE: Say on the download pages that the callback holds no session lock

---
### Description

The download pages show a callback that builds bytes and returns them:

```java
DownloadHandler.fromInputStream(event -> {
    var bytes = build();
    return new DownloadResponse(new ByteArrayInputStream(bytes), "file.csv", "text/csv", bytes.length);
});
```

Reading UI state inside it is the obvious next step, and ours did: two signals, to know what the filter showed.

A download is its own request and holds no session lock. That is documented, once, in a note under a different example about updating the UI afterwards: "`UI.access` is needed for updating the UI and also session locking if you want to access the session."

### Why it matters

Two things follow and neither is where somebody looks.

A read of UI state from the callback is a race, not a crash, so it survives testing: a test calls the callback on a thread that happens to hold the lock.

And a callback that throws has no screen to fail on. The browser gets whatever the container makes of the exception, the person gets nothing, and it reads as the application having crashed. Ours was reported that way.

### Expected

One sentence on the `DownloadHandler` page, next to the code somebody is about to copy: the callback runs without the session lock, so copy what you need into a snapshot, and catch inside it so a failure becomes `DownloadResponse.error(500, message)`.

### What makes it worth a sentence in the documentation

The callback is handed the session. `DownloadEvent` declares `getSession()` beside `getRequest()`, `getResponse()` and `getOwningComponent()`, so the session is right there in the parameter, and nothing on the page says the lock is not held with it. Reading a signal or a component's value through that session, which is the natural thing to do when the file has to reflect what is on screen, is the unsafe case.

What we do instead is take a snapshot on the UI thread, in fields the callback reads, which is in `InvoiceListView`.

Found on 25.3.0-beta1.
