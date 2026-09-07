# Epic 01: Foundation and build

**Goal:** a runnable, secured, themed, internationalised empty application on Vaadin 25.3.0-beta1, with the tooling every later epic depends on.
**Feature spec:** `specs/features/01-foundation.md`
**Dependencies:** none. Everything depends on this.

---

### US-1.1: Buildable skeleton

**As a** developer **I want** a Maven project on 25.3.0-beta1 **so that** every later story starts from a green build.

**Tasks:**
- [ ] `pom.xml`: Spring Boot 4 parent, `vaadin.version=25.3.0-beta1`, `vaadin-bom`, Java 21, prereleases repository, `spring-boot:run` as default goal
- [ ] Profiles `commercial`, `ai`, `observability`, `postgres`, `bulk`, `it`, `production` as declared in `09-tooling.md`
- [ ] Declare `flow-maven-plugin` with the beta1 comment, so `mvn flow:install-dev-cli` works
- [ ] `vaadin-featureflags.properties` with `breadcrumbsComponent`, `switchComponent`, `aiComponents`
- [ ] `.mcp.json` with the Vaadin docs server at `https://mcp.vaadin.com/docs-java/docs` and Playwright
- [ ] `.github/workflows/ci.yml` running `./mvnw verify` on the default profile
- [ ] Package skeleton with `package-info.java` and `@NonNullApi` per package

**Verified by:** `ApplicationSmokeBrowserlessTest`

---

### US-1.2: Application shell

**As a** user **I want** a consistent shell **so that** I always know where I am and what I can reach.

**Tasks:**
- [ ] `MainLayout` annotated `@Layout`, with `SideNav` from `MenuConfiguration`, grouped into Shop, Operations, Administration
- [ ] Page title bound to `UI.routerStateSignal()`
- [ ] Theme toggle bound to a signal, stored per user
- [ ] User menu with logout, language selector, cart badge placeholder. The logout goes through `AuthenticationContext.logout()`: navigating to `/logout` is answered 403, because Spring Security maps it as a POST, and leaves the session open
- [ ] Branded not found view inside the shell
- [ ] Hide navigation entries the current user cannot reach, using `AccessAnnotationChecker`

**25.3 APIs:** `Component.whenAttached` for every subscription in the layout.
**Verified by:** `ThemeToggleBrowserlessTest`, `ApplicationSmokeBrowserlessTest`

---

### US-1.3: Security

**As an** operator **I want** access controlled by annotation **so that** a forgotten annotation fails the build rather than leaking data.

**Tasks:**
- [ ] `SecurityConfiguration` with `VaadinSecurityConfigurer`, BCrypt, `Role` enum, `CurrentUser`
- [ ] Login view with the demo credentials and a distinct message for locked accounts
- [ ] Configure the safe URL scheme list, keep `X-Frame-Options` on
- [ ] `SecurityRulesTest` walking the whole route table per role, failing on any route with no access annotation

**Verified by:** `SecurityRulesTest`, `LoginBrowserlessTest`

---

### US-1.4: Theme foundation

**As a** visitor **I want** the application to look like a bakery **so that** it does not look like a database viewer.

**Tasks:**
- [ ] `@StyleSheet("styles.css")` on `Application`, and no theme declared there: the shell loads exactly one theme at runtime from the selector, so plain Lumo and plain Aura can be compared without one leaking into the other
- [ ] `META-INF/resources/styles.css` importing `base/layout.css` and `base/typography.css`
- [ ] Aura property block with the bakery palette for light and dark
- [ ] Dark mode through `Page.setColorScheme`. The `getThemeList().bind("dark", signal)` pattern that every Lumo era example shows is a silent no-op under Aura, see `FEEDBACK-25.3.md`

**Verified by:** `ThemeToggleBrowserlessTest`

---

### US-1.5: Internationalisation

**As a** visitor **I want** the interface in my language **so that** I can use it.

**Tasks:**
- [ ] `translations.properties`, `_en`, `_es` under `vaadin-i18n`
- [ ] A `LanguageSelector` in the shell writing `UI.setLocale`. No provider of our own: the platform loads the `vaadin-i18n` bundles
- [ ] Views react through `UI.localeSignal()`, no reload
- [ ] `NoHardcodedStringsTest` and `TranslationCompletenessTest`

**Verified by:** `LocaleSwitchBrowserlessTest`, `NoHardcodedStringsTest`

---

### US-1.6: Dev loop and agent scaffolding

**As an** agent **I want** the same commands to work on every machine **so that** I do not invent my own workflow.

**Tasks:**
- [ ] Run `mvn flow:install-dev-cli` and commit what it writes
- [ ] `CLAUDE.md` with the stack table, hard rules, the Kotlin caveat and the Definition of Done
- [ ] `.claude/commands/implement-story.md` and the four phase commands
- [ ] Document the beta1 goal prefix workaround and the JBR search paths in `09-tooling.md`

**Verified by:** manual, recorded in `DEMO.md`

---

## Left undone

- Three of the seven profiles were never written: `commercial`, `bulk` and `it`. See the table in `09-tooling.md`.
- No `package-info.java` anywhere, so no `@NonNullApi`.
- The navigation is not grouped into Shop, Operations and Administration, and nothing uses `AccessAnnotationChecker`. `MenuConfiguration` hides what a role cannot reach on its own, which covers the behaviour and not the grouping.
- The theme choice lives in a session scoped bean rather than on the user, so it does not survive a logout.
- There is no branded not found view.
- The safe URL scheme list and `X-Frame-Options` were never configured. The application takes whatever Spring Security defaults to.
- What the dev loop CLI wrote under `.vaadin/` is not committed, so the next machine installs it again.

## Definition of Done

- [ ] `./mvnw` starts the application, `./mvnw verify` is green with no licence and no network
- [ ] Every acceptance criterion in `features/01-foundation.md` is checked
- [ ] The about route exists, even if it only prints the version at this point
