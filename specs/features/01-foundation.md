# Feature 01: Foundation and build

## Overview

A runnable, secured, themed, empty application on Vaadin 25.3.0-beta1 that every later epic builds on. Nothing user visible except a login, a shell and an about page, but everything that makes the following thirteen epics safe to write.

## Behaviour

### Build

Single Maven module, `com.vaadin.bakery:bakery`, Java 21, Spring Boot 4, Vaadin `25.3.0-beta1`, prereleases repository, `spring-boot:run` as the default goal. Profiles exactly as listed in `09-tooling.md`. The `flow-maven-plugin` is declared so the dev loop CLI can be installed, with a comment explaining that this is a beta1 workaround.

### Shell

`MainLayout` is annotated `@Layout`, so no view declares a layout. It contains: brand, a `SideNav` built from `MenuConfiguration`, the cart badge, the language selector, the theme toggle, and the user menu. The page title is bound to `UI.routerStateSignal()`, not set by each view.

Navigation entries are grouped: Shop, Operations, Administration. A group whose entries are all inaccessible to the current user does not render.

### Security

Spring Security with `VaadinSecurityConfigurer`, the `Role` enum, BCrypt, a login view showing the demo credentials, session fixation protection, and the route table of `04-security.md`. Locked users are refused at login with their own message.

### Theme

Aura, dark mode bound to a signal, the CSS layout of `05-theming.md` with an empty per view file per planned view. The theme choice is stored per user and restored at login.

### Language

English and Spanish, chosen from the language selector in the shell, and the choice takes effect on the spot.

Changing the language retranslates what is already on screen. The reader does not navigate, does not reload, and does not lose their place: the screen they were looking at is the same screen, in the other language. Anything they had started keeps its value, so a half filled form, a basket, a chosen filter and a scroll position all survive the switch.

Everything visible follows the language, not only the headings and the buttons that are easy to remember. Field labels, placeholders, helper texts, accessible names, table and grid column headings, the options inside a dropdown, empty states, badges and the navigation entries all change with it. So does text the screen works out for itself rather than reads from a bundle: a formatted date, a weekday or a month name, an amount of money, and the cells of a list whose text is derived instead of stored.

Two things do not change, and both are deliberate. Text composed at the instant it is shown keeps the language it appeared in, because a notification or a question raised by a click belongs to the moment somebody acted, and rewriting it underneath them reads as a glitch rather than a translation. And an invoice keeps the language it was issued in, because it is a document of record and not a screen.

From this epic on, a user visible string written into the source rather than into the bundles is a defect, wherever it is written, an annotation included. That last word is not a detail: a title written into an annotation is as visible as one written into a heading, and is the easiest place for an untranslated string to hide.

One exemption, and it is narrow. A view that Copilot generates under a `__copilot/` route, keeping it out of the startup registry, is developer tooling rather than a screen of this application, and it says in its own text that regenerating replaces the file. Translating it would be undone by the next click and would put tooling text into the bundles a customer reads. `NoHardcodedStringsTest` and `AttachLifecycleTest` skip such files and print which ones they skipped, so the exemption cannot quietly widen.

### Lifecycle

Any component that subscribes to something outside itself registers in `Component.whenAttached` and releases through the returned registration. No `onAttach` or `onDetach` overrides in the codebase.

### About page

`/about` lists the Vaadin version, the active Maven profiles, the state of every feature flag the application depends on, which assistant is answering and whether the observability backend is on. It does not report a licence: this build is licensed, see Licensing in `00-overview.md`. It is the first thing to open when a demo behaves strangely. It is written in Kotlin, see epic 14.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A required feature flag is off | Startup logs an error naming the flag and the file, and the about page shows it in red. The application still boots |
| No commercial licence | Development runs and the tests pass, because the licence is enforced in the browser and in the production build. A production build fails, which is the correct answer for a build of this version |
| No OpenAI key | Boots. The assistant is off, said in red on every surface that offers one, and everything else on those screens works |
| A locked user logs in | Refused with "This account is locked", not with "bad credentials" |
| An unknown route | A branded not found page inside the shell, with a link home |
| The browser is in a locale we do not have | Falls back to English |
| A screen was open before the language changed | It is retranslated where it stands. Nothing navigates, nothing reloads, and the reader keeps their place |
| A half filled form when the language changes | Every value already typed is still there afterwards |
| A list cell whose text is derived, such as a date, an amount or a translated status | Follows the new language too, not only the fixed labels around it |
| A notification or a confirmation is on screen when the language changes | It keeps the language it appeared in |

## Acceptance criteria

### AC1: The application boots clean
- [ ] `./mvnw` starts the application on 8080 with no error and no warning about missing licences
- [ ] `./mvnw verify` passes with no commercial licence, no browser and no network
- [ ] The startup log prints the version, the active profiles and the state of every required feature flag

### AC2: The shell works
- [ ] The side navigation shows only what the current user may reach
- [ ] The page title comes from the router state, and changing view changes it
- [ ] The theme toggle switches light and dark without a reload, and the choice survives a logout and a login

### AC3: Security is enforced by annotation
- [ ] Every route carries an access annotation. A route without one fails `SecurityRulesTest`
- [ ] An anonymous request to a staff route lands on the login view and returns to the target after login
- [ ] A locked user cannot log in
- [ ] Log out clears the authentication rather than navigating to a path Spring Security answers with 403

### AC4: Internationalisation is real
- [ ] Switching language retranslates the screen that is already open, with no reload and no navigation of any kind in between
- [ ] Labels, placeholders, accessible names, column headings, dropdown options, empty states and navigation entries follow the language, not only headings and buttons
- [ ] Dates, times, weekday and month names and money follow the selected language, including inside list cells whose text is derived rather than stored
- [ ] Nothing already started is lost by switching: form values, basket and filters all survive
- [ ] Text composed at the moment it was shown, such as a notification already on screen, keeps the language it appeared in
- [ ] The view title in the shell and the browser tab title follow the language
- [ ] No user visible string is written into the source rather than the bundles, annotation values included, asserted by a test that greps the sources

### AC5: The about page tells the truth
- [ ] It lists version, profiles, flags, the assistant and the observability state. It names the provider that is answering rather than reporting whether a key is set, which is the more useful of the two
- [ ] Turning off a flag changes what it reports

### Still open

Nothing in this document is built yet.


## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| FND-01 | The default profile | The application starts | Context loads and the storefront route resolves | browserless | `ApplicationSmokeBrowserlessTest` |
| FND-02 | An anonymous visitor | Navigating to `/orders` | Redirected to the login view | browserless | `SecurityRulesTest` |
| FND-03 | Each role in turn | Navigating to every route in the table | Access matches `04-security.md` exactly | browserless | `SecurityRulesTest` |
| FND-04 | A locked user | Logging in with correct credentials | Refused with the locked message | browserless | `LoginBrowserlessTest` |
| FND-05 | A logged in user in English | Choosing Spanish and then opening a screen | Menu, buttons and dates are Spanish, no reload happened | browserless | `LocaleSwitchBrowserlessTest` |
| FND-09 | A screen already open in English | Choosing Spanish, and asserting without navigating afterwards | That same screen, still open, reads Spanish, its navigation entries included | browserless | `LocaleSwitchInPlaceBrowserlessTest` |
| FND-10 | A list whose cells are derived: a formatted date, an amount, a translated status | Choosing Spanish, without navigating afterwards | Those cells are Spanish too, not only the fixed labels around them | browserless | `LocaleSwitchDerivedTextBrowserlessTest` |
| FND-11 | A notification on screen, and a form half filled in | Choosing Spanish | The notification keeps the language it appeared in, and every typed value is still there | browserless | `LocaleSwitchTransientTextBrowserlessTest` |
| FND-12 | Any screen in English | Choosing Spanish | The view title in the shell and the browser tab title are Spanish | browserless | `PageTitleLocaleBrowserlessTest` |
| FND-13 | A signed in user | Choosing Log out in the user menu | The authentication context is cleared, rather than the browser being sent to a path that answers 403 | browserless | `LogoutBrowserlessTest` |
| FND-14 | Any route in a real browser | Reading the tab and the breadcrumb trail | Both say the route's own title, not the application name | testbench | `PageTitleIT` |
| FND-06 | The source tree | Scanning for user visible literals | None found outside the bundles | unit | `NoHardcodedStringsTest` |
| FND-07 | Light mode | Toggling the theme | The `dark` theme attribute is present on the UI element | browserless | `ThemeToggleBrowserlessTest` |
| FND-08 | Any component that subscribes to a shared source | Detaching the view | The registration is released, asserted by a counter | browserless | `AttachLifecycleTest` |

FND-05 and FND-09 look alike and are not. FND-05 opens a screen after the switch, so it is satisfied by a screen built fresh in the new language. FND-09 asserts on a screen that was already there and never navigates in between, which is the only way to tell a screen that follows the language from one that merely happened to be built after it changed. A test that navigates between the switch and the assertion does not prove FND-09 and must not be named as its evidence.
