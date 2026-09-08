REPO: vaadin/flow
TITLE: Two server side gaps: an absolute route URL, and asking what is licensed

---
### Description

**There is no server side way to build a route's absolute URL.** `RouterLink` and `RouteConfiguration.getUrl` produce paths relative to the context root, which is right for navigating and useless for a link that leaves the browser: an email, an invoice, something copied to the clipboard. The only absolute answer, `Page.fetchCurrentURL`, is a round trip to the client and asynchronous, so it cannot be used while building the component that needs the value.

Every application that emails a link writes the same thing: read the scheme, authority and context path off the `VaadinServletRequest` the call arrived on, and decide for itself what to do behind a reverse proxy.

**Nothing lets an application ask what it is licensed for.** `GridPro.class` contains no reference to the licence checker: the check happens elsewhere, and `LicenseChecker.isValidLicense` answers the same for a product name that does not exist as for one that does. So an application cannot adapt, and cannot even tell whether adapting would be needed.

We do not want a licence probe in application code, and we hit this while deciding whether a core only branch was possible. The answer had to come from reading jars.

### Expected

`RouteConfiguration.getAbsoluteUrl(Class, parameters)`, or an absolute form of the existing helpers.

And either a component level `isLicensed()`, or a documented product identifier per component so the existing checker can be asked a real question.

Found on 25.3.0-beta1.
