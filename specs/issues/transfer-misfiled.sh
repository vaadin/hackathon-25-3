#!/usr/bin/env bash
#
# Moves the three issues that were filed in vaadin/flow and belong in
# vaadin/flow-components.
#
# Nothing moves without --confirm. Run it once without, read what it prints,
# and only then run it again with.
#
#   ./specs/issues/transfer-misfiled.sh            # prints what it would do
#   ./specs/issues/transfer-misfiled.sh --confirm  # transfers them
#
# Why these three and not the others. Six of their siblings were already moved
# to vaadin/flow-components by the triage that ran over the batch:
#
#   25570 -> flow-components#10071    25581 -> flow-components#10074
#   25573 -> flow-components#10072    25582 -> flow-components#10075
#   25577 -> flow-components#10073    25583 -> flow-components#10076
#
# and one of those transfers carries a comment saying so in as many words:
# "the code lives in vaadin/flow-components (vaadin-ai-extensions-flow), not
# in this repo". The three below are the same artefacts and were missed.
#
# The ownership is not a guess. In the local repository:
#
#   vaadin-ai-core-flow       parent vaadin-ai-components-flow-parent
#   vaadin-ai-extensions-flow parent vaadin-ai-components-flow-parent
#   vaadin-ai-components-flow-parent  parent vaadin-flow-components
#
# which is the root of the vaadin/flow-components monorepo, the same parent
# chain vaadin-grid-flow has.
#
# `gh issue transfer` needs push access to the target repository. Without it
# the command fails and nothing is half done.

set -euo pipefail

cd "$(dirname "$0")/../.."

CONFIRM=no
[ "${1:-}" = "--confirm" ] && CONFIRM=yes

TARGET=vaadin/flow-components

# issue number : the class that decides the repository : the artefact holding it
MOVES=(
    "25561:LLMProvider.ToolSpec, AIOrchestrator:vaadin-ai-core-flow"
    "25568:FormAIController:vaadin-ai-extensions-flow"
    "25569:AIOrchestrator:vaadin-ai-core-flow"
)

for move in "${MOVES[@]}"; do
    number=${move%%:*}
    rest=${move#*:}
    owner=${rest%%:*}
    artefact=${rest##*:}

    title=$(gh issue view "$number" --repo vaadin/flow --json title --jq .title 2>/dev/null || echo "?")
    here=$(gh issue view "$number" --repo vaadin/flow --json url --jq .url 2>/dev/null || echo "")

    echo "--- vaadin/flow#$number"
    echo "    title:    $title"
    echo "    owner:    $owner, in $artefact"
    echo "    move to:  $TARGET"

    if [ -z "$here" ]; then
        echo "    already gone from vaadin/flow, nothing to do"
        echo
        continue
    fi

    if [ "$CONFIRM" = "yes" ]; then
        gh issue transfer "$number" "$TARGET" --repo vaadin/flow
        echo "    transferred"
    else
        echo "    (dry run, nothing moved)"
    fi
    echo
done

if [ "$CONFIRM" != "yes" ]; then
    echo "Nothing was moved. Re-run with --confirm."
    echo
    echo "Three more are misfiled by content rather than by repository, and a"
    echo "transfer would not fix any of them, because each one is two findings"
    echo "in one issue and they belong in different places. They need splitting"
    echo "by hand, and the triage comments already say so:"
    echo "  25580  four older behaviours: two of them are flow-components"
    echo "  25574  two server side gaps: the licensing half is not Flow"
    echo "  25575  two dark mode mechanisms: one is a Flow bug, one is Aura"
fi
