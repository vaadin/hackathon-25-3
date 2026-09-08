REPO: vaadin/flow
TITLE: FeatureFlags.setEnabled rewrites the project's source file

---
### Description

Calling

```java
FeatureFlags.get(lookup).setEnabled("aiComponents", true);
```

writes `src/main/resources/vaadin-featureflags.properties`, in the project, from a running application.

### Why it matters

A test that enables a flag edits the repository. Ours did, and the change turned up in `git status` next to the code we were writing, which is how we found it.

It also means a flag enabled programmatically is not scoped to the run: it persists for everybody afterwards, including CI.

### Expected

Either keep a programmatic override in memory for the life of the process, or say plainly on the feature flags page that this method edits the source file, so nobody calls it from a test.

### Workaround

Commit the file with the flags the application needs, and never call `setEnabled` from code.

### Reproduce

[`25-featureflags.zip`](https://github.com/vaadin/hackathon-25-3/raw/1e50ad1f28e0c668ef34f8a87d9cec8ad8ab4626/specs/issues/projects/25-featureflags.zip), then `mvn test`. The test asserts the project has no flags file, calls `setEnabled`, and finds one. It deletes it again afterwards.

### Getting the project

[`25-featureflags.zip`](https://github.com/vaadin/hackathon-25-3/raw/1e50ad1f28e0c668ef34f8a87d9cec8ad8ab4626/specs/issues/projects/25-featureflags.zip), 3 KB, sources only. Unzip it and:

```
mvn test
```

Found on 25.3.0-beta1.
