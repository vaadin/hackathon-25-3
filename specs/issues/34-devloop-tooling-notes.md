REPO: vaadin/flow
TITLE: Four small things about the dev loop CLI

---
### Description

**The documented install command has the wrong prefix.** `mvn vaadin:install-dev-cli` does not exist in beta1. The goal lives on `flow-maven-plugin`, so it is `mvn flow:install-dev-cli`, and that only works if the plugin is declared in the project. Nothing says so, and the error is a `NoPluginFoundForPrefixException` that does not hint at the other plugin.

**It compiles Java only, and says nothing about what it skipped.** A Kotlin file in the change set produces no entry at all, so `apply` reports success and nothing happened. One line, "3 file(s) not compiled: Kotlin is not in the loop", turns a silent no operation into an explanation.

**It does not look for a JetBrains Runtime in `~/.vaadin/jdk`.** That is where Vaadin's own tooling puts one, and the loop wants a JBR for enhanced class redefinition. We pointed it at ours by hand.

**The reference table is pessimistic.** It says structural Java, "new fields, new beans, changed routes or annotations", needs a restart. A new `private static final` field plus a brand new `private static` method hot swapped in 0.94 seconds, `redefineClasses(1)`, no restart. Being told to expect a restart makes people batch edits to save restarts that were never going to happen, and it is the same table a reader checks when something really does need one.

### Why it matters

The first one blocks installation. The others cost trust: a tool whose output cannot be taken literally gets checked by hand, which is the thing it was bought to avoid.

### Expected

Fix the prefix in the documentation, name skipped files in the output, look in `~/.vaadin/jdk`, and split the reference table row by what enhanced redefinition really handles.

Found on 25.3.0-beta1 with `flow-devloop-daemon` 25.3.0-beta1.
