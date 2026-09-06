---
description: Implement one user story end to end, with gates
argument-hint: US-<epic>.<story> [--skip-visual] [--resume-from=implement|verify|tests|review]
---

Implement the story named in `$ARGUMENTS`, following the five phases below. Do not skip a phase. Do not mark a phase done without running its gate.

## Phase 0: parse

1. Read `specs/user-stories/epic-<NN>-*.md` and locate the story.
2. Read the feature document it points at, in `specs/features/`.
3. Read `specs/00-overview.md` hard rules and `CLAUDE.md`.
4. Create one todo per remaining phase.

Stop and ask if the story does not exist or its feature document has no acceptance criteria.

## Phase 1: implement

Follow `.claude/commands/story/implement.md`.

Gate: `./mvnw compile` and `./mvnw test` both pass. On failure, fix it here. Do not continue with a red build.

## Phase 2: verify the UI

Follow `.claude/commands/story/verify-ui.md`. Start the application, walk the acceptance criteria with Playwright, prefer the accessibility snapshot over screenshots for assertions.

Gate: soft. Record what fails and continue, but list every problem in the summary.

## Phase 3: tests

Follow `.claude/commands/story/create-tests.md`. Every row of the feature document's test case table must exist as a real test, in the tier the table names.

Gate: `./mvnw verify` passes.

## Phase 4: review

Follow `.claude/commands/story/visual-review.md`. Only when the story changes something a user sees.

Output goes to `specs/reviews/<feature>/REVIEW.md`.

## Phase 5: summary

Report: what was built, which acceptance criteria are now checked, which tests were added, what the UI verification found, and anything left undone. Tick the checkboxes in the specification files for what is genuinely done, and nothing else.
