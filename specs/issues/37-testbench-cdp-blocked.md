REPO: vaadin/testbench
TITLE: TestBench's driver proxy blocks CDP, which is the only way to emulate print media

---
### Description

`Emulation.setEmulatedMedia` is a Chrome DevTools Protocol call, and CDP is how print styles are tested in a browser. TestBench wraps the driver in a proxy that does not expose the CDP interface, so a test cannot reach it: the cast to `HasCdp` or `ChromiumDriver` fails against the proxy.

### Why it matters

An invoice, a receipt, a report: anything with a print stylesheet has no way to assert that the stylesheet does what it says. Ours has one and the assertion lives in a comment.

### Expected

Expose the underlying driver, or forward the CDP interface through the proxy.

### Workaround

None found. We assert the print rules exist in the stylesheet and stop there.

Found on 25.3.0-beta1 with TestBench for Vaadin 25.
