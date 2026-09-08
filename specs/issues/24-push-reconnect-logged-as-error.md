REPO: vaadin/flow
TITLE: A push reconnect during a restart is logged as an application error

---
### Description

Any application with `@Push`, one page open in a browser, and a restart. `PushHandler.onConnect` runs while the service is re-initialising and logs a full stack trace:

```
ERROR org.atmosphere.cpr.AtmosphereFramework : AtmosphereFramework exception
java.lang.IllegalStateException: Can not process requests before init() has been called
        at com.vaadin.flow.server.VaadinService.requestStart(VaadinService.java:1583)
```

One to four of them per restart, tracking how many times the client retried. The client recovers on its own.

### Why it matters

Nothing is broken, and the signal is. The dev loop's `status` command reports `app log: N error(s)`, and the guidance for agents is that `Stable` with an `app log:` line under it is a failure to investigate. So every restart with a browser open raises that alarm, and a reader learns to ignore the one signal the tool exists to give.

It is not every restart, which matters: a restart with a page open on a quiet route sometimes produces none. That makes it a race that looks intermittent rather than a condition.

### Expected

A connection arriving before `init()` is a normal race on any restart, not an error condition. Log it at debug, or close the connection and let the client retry, rather than an `ERROR` with a stack trace.

### Reproduce

1. Any Vaadin application annotated `@Push`
2. Open one page
3. Restart the server

Found on 25.3.0-beta1.
