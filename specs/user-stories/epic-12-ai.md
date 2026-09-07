# Epic 12: AI

**Goal:** make one boring job fast, and make it honest about what it did.
**Feature spec:** `specs/features/12-ai.md`, full design in `specs/06-ai.md`
**Dependencies:** epics 02, 04, 06 and 07.

---

### US-12.1: Provider wiring

**Tasks:**
- [ ] `SpringAILLMProvider` over the auto configured OpenAI `ChatModel`, built under the `ai` profile when a key is present, one instance per orchestrator
- [ ] `MockLLMProvider` implementing `LLMProvider`, replaying cassettes keyed by prompt hash
- [ ] Record the cassettes once from a real run, commit them under `ai/cassettes`
- [ ] Runtime provider selector in the assistant panel

**Verified by:** `MockLLMProviderTest`

---

### US-12.2: Phone order form filling

**As a** barista **I want** the form filled from what the customer said **so that** I can listen instead of type.

**Tasks:**
- [ ] `/orders/new` with a `FormLayout` and a `FormAIController`
- [ ] Field rules: hidden internal note, read only total, no customer creation, products and quantities constrained
- [ ] Field marker with custom popover content showing the source snippet
- [ ] Source tracking on, confidence per field displayed
- [ ] Rejected writes reported back so the model self corrects

**25.3 APIs:** `FormAIController`, AI field marker, source tracking, confidence levels.
**Verified by:** `FormFillingBrowserlessTest`, `FormFillingGuardrailsBrowserlessTest`

---

### US-12.3: Policy layer and meter

**Tasks:**
- [ ] `RequestInterceptor` masking card shaped strings, rejecting off topic prompts, postponing over budget ones
- [ ] `ToolException` for tool failures, safe message to the model, stack trace to the log
- [ ] Turn meter from `ResponseMetadata`: tokens, estimated cost, finish reason, cut off warning
- [ ] `setBackgroundExecution(true)` so the form stays usable

**25.3 APIs:** request interception, response metadata, `ToolException`, background execution.
**Verified by:** `RequestInterceptorTest`, `TurnMeterBrowserlessTest`

---

### US-12.4: Grid and chart assistants

**Tasks:**
- [ ] `GridAIController` on the order board, answering only about what the grid holds
- [ ] Per turn session context: route, closures, open slots, current order or cart
- [ ] `ChartAIController` on the dashboard

**25.3 APIs:** `GridAIController`, orchestrator reading grid state, per turn session context.
**Verified by:** `GridAssistantBrowserlessTest`

---

### US-12.5: Free fallback

**Tasks:**
- [ ] Paste and parse filler using regular expressions and the shared product matcher
- [ ] Say on the about page which provider is answering
- [ ] Every AI test green against the mock with no network

**Verified by:** `AiFallbackBrowserlessTest`

---

## Left undone

- No runtime provider selector. Switching between the mock and the live model is a restart.
- US-12.2 and US-12.4 are not started. `FormAIController`, `GridAIController` and `ChartAIController` are all commercial, `vaadin-ai-extensions-flow` is a dependency that nothing imports, and the field markers, source tracking, confidence and per turn session context come with them.
- `ToolException` and `setBackgroundExecution(true)` are unused.
- Off topic prompts are recognised and rejected. Postponing an over budget one is not built.
- The about page names the provider that is answering. It does not say why an assistant is absent, because none of the commercial ones was ever shown.

## Definition of Done

- [ ] Every acceptance criterion in `features/12-ai.md` is checked
- [ ] The demo works with the network unplugged
