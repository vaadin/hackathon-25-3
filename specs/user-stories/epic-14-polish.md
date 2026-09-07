# Epic 14: Tests, i18n sweep, docs and demo polish

**Goal:** make the whole thing defensible, and make the demo work on a bad day.
**Feature spec:** `specs/features/14-polish.md`
**Dependencies:** everything.

---

### US-14.1: Spec and test consistency

**Tasks:**
- [ ] `SpecConsistencyTest` parsing every feature document and asserting each `Verified by` class exists
- [ ] Fill every gap it reports
- [ ] JaCoCo floor from `08-testing.md`, failing the build below it

**Verified by:** `SpecConsistencyTest`

---

### US-14.2: Multi user and browser coverage

**Tasks:**
- [ ] Multi user browserless coverage for the kitchen board and the conversation
- [ ] The closed TestBench list, bootstrapped with the Copilot Test Recorder and hardened by hand
- [ ] No `Thread.sleep` anywhere, no test depending on the wall clock

**Verified by:** the IT list in `08-testing.md`

---

### US-14.3: Translation sweep

**Tasks:**
- [ ] `NoHardcodedStringsTest` green across the whole source tree
- [ ] `TranslationCompletenessTest` green for both bundles
- [ ] Spanish reviewed by a human

**Verified by:** `NoHardcodedStringsTest`, `TranslationCompletenessTest`

---

### US-14.4: About page in Kotlin

**Tasks:**
- [ ] `kotlin-maven-plugin` for one source file
- [ ] `/about` listing version, profiles, flags, licence, AI provider and observability, each linking to its demo view
- [ ] Header comment and a `CLAUDE.md` note that the dev loop does not hot swap Kotlin

**25.3 APIs:** Copilot Kotlin support, exercised while writing it.
**Verified by:** `AboutViewBrowserlessTest`

---

### US-14.5: Theme polish

**Tasks:**
- [ ] Final Aura palette in both schemes, contrast checked
- [ ] Motion limited to user caused transitions, respecting `prefers-reduced-motion`
- [ ] Axe pass per view, recorded in the checklist

**Verified by:** `AuraDarkModeIT`

---

### US-14.6: Documentation and demo script

**Tasks:**
- [ ] `README.md`: what it is, how to run it, demo accounts, profiles, what needs a licence
- [ ] `DEMO.md`: a five minute and a fifteen minute script, with a fallback for every step that needs network, licence or key
- [ ] `specs/CHANGELOG-RISK.md`: every preview or beta API, where it is used, what to recheck at GA
- [ ] Branch README keeping the hackathon rules block on top, project write up below

**Verified by:** manual, on a clean clone

---

## Left undone

- `SpecConsistencyTest` reports its gaps rather than having none: thirteen classes named in the feature documents do not exist, and the test holds them in a list so they stay visible.
- No JaCoCo, so no coverage floor.
- The TestBench list is closed and empty. Nothing was recorded with the Copilot Test Recorder, and there is no `it` profile to run it under.
- Multi user coverage exists for the kitchen board. The conversation half is missing because the conversation is not live.
- The Spanish was never reviewed by a human.
- The about page rows do not link to the view that demonstrates them.
- Contrast, motion and axe were never checked: `prefers-reduced-motion` appears in one stylesheet, and no view has had an accessibility pass.
- Nobody has cloned this fresh and walked `DEMO.md` end to end.

## Definition of Done

- [ ] `./mvnw verify` green on the default profile with no licence, no key and no network
- [ ] A stranger can clone, run and demo the application using only the README and `DEMO.md`
