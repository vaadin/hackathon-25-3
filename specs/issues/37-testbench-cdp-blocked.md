REPO: vaadin/testbench
TITLE: Say that the driver has to be unwrapped before CDP can be reached

---
### Description

`Emulation.setEmulatedMedia` is a Chrome DevTools Protocol call, and CDP is the only way to ask a browser what a page looks like on paper: Selenium has no API for the print medium.

`getDriver()` in a TestBench test answers a proxy. Casting it to `ChromeDriver`, or to any of the CDP interfaces, throws, because the proxy does not implement them:

```
public class com.vaadin.testbench.TestBenchDriverProxy implements
    org.openqa.selenium.WebDriver,
    org.openqa.selenium.WrapsDriver,
    com.vaadin.testbench.HasTestBenchCommandExecutor,
    org.openqa.selenium.HasCapabilities,
    org.openqa.selenium.TakesScreenshot,
    org.openqa.selenium.JavascriptExecutor
```

`WrapsDriver` in that list is the way through, and it is the whole request: unwrap until nothing wraps any more, and the real driver is there.

```java
var driver = getDriver();
while (driver instanceof WrapsDriver wrapper) {
    driver = wrapper.getWrappedDriver();
}
((ChromeDriver) driver).executeCdpCommand("Emulation.setEmulatedMedia",
        Map.of("media", "print"));
```

### Why it matters

Nothing says this. A test that needs print media, or any other CDP call, meets a `ClassCastException` on a class named like a proxy, and the natural conclusion is that TestBench blocks CDP. We drew exactly that conclusion and wrote the assertion off as impossible for a while.

### Expected

One paragraph on the TestBench page about driver access, with the loop above. Forwarding the CDP interfaces through the proxy would be better still, since the loop is boilerplate every project repeats.

### A correction to an earlier version of this report

This was first written as "TestBench's driver proxy blocks CDP", with no workaround. That is wrong. The unwrap works, in a real Chrome, and the print media test it was blocking now runs green in this repository: `InvoicePrintIT`, two tests, asserting that the print page carries no application shell and that the document is black on white under print media.

Found on 25.3.0-beta1 with TestBench for Vaadin 25, verified against `vaadin-testbench-shared-25.2.1`.
