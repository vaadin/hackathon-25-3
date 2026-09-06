# Tests for a story

Every row of the feature document's test case table becomes a real test, in the tier the table names, in a class with the name the table names. That name is a contract: `SpecConsistencyTest` fails the build if the class is missing.

## Tiers

| Tier | Suffix | Location | Runs with |
| --- | --- | --- | --- |
| Unit | `*Test` | beside the feature package | `./mvnw test` |
| Browserless | `*BrowserlessTest` | beside the feature package | `./mvnw test` |
| End to end | `*IT` | `src/test/java/.../it` | `./mvnw verify -Pit` |

A test only belongs in the IT tier if it is on the closed list in `specs/08-testing.md`. If you think you need a new one, change that document first and say why.

## Rules

- No `Thread.sleep`. Use the framework's waiting, or `runPendingSignalsTasks()` after mutating a shared signal from another session.
- No dependence on the wall clock. The `Clock` bean is fixed in the test profile.
- The assistant is always the mock provider with cassettes.
- Multi user tests use `BrowserlessApplicationContext`. Close windows, then users, then the context.
- A test owns the data it creates and cleans it up. Do not mutate the seeded dataset unless the test is about the seeded dataset.
- Assert the refusal, not only the happy path. Every rule in `04-security.md` has a test that proves it says no.

## Gate

`./mvnw verify` passes.
