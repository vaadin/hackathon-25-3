#!/usr/bin/env bash
#
# Splits vaadin/flow#25575.
#
# The documentation half goes to vaadin/docs as a new issue; 25575 keeps the
# stale ThemeList bug, which is Flow's and already has a fix attached, and gets
# a comment carrying the re-measurement its triage asked for.
#
# Nothing is sent without --confirm.
#
#   ./specs/issues/split-25575.sh            # prints what it would do
#   ./specs/issues/split-25575.sh --confirm  # creates the issue and comments

set -euo pipefail

cd "$(dirname "$0")/../.."

CONFIRM=no
[ "${1:-}" = "--confirm" ] && CONFIRM=yes

TRACKER=9204

add_to_tracker() {
    local url=$1
    local body
    body=$(mktemp)
    gh issue view "$TRACKER" --repo vaadin/platform --json body --jq .body > "$body"
    if grep -qF "$url" "$body"; then
        echo "    already in platform#$TRACKER"
    else
        # $(cat) strips the trailing newlines, which is the whole point: gh
        # prints the body plus one of its own, so a plain >> leaves a blank
        # line before every entry and the list grows gappier each time.
        printf -- "%s\n- [ ] %s\n" "$(cat "$body")" "$url" > "$body.new"
        mv "$body.new" "$body"
        gh issue edit "$TRACKER" --repo vaadin/platform --body-file "$body" > /dev/null
        echo "    added to platform#$TRACKER"
    fi
    rm -f "$body"
}

DRAFT=specs/issues/58-one-dark-mode-mechanism-say-so.md
COMMENT=specs/issues/58b-comment-on-flow-25575.md
ZIP=specs/issues/projects/38-dark-mode.zip
ORIGINAL=25575

repo=$(sed -n 's/^REPO: //p' "$DRAFT" | head -1)
title=$(sed -n 's/^TITLE: //p' "$DRAFT" | head -1)
SHA=$(git rev-parse HEAD)

echo "HEAD is $SHA"
echo

problems=0
[ -n "$(git status --porcelain)" ] && { echo "! The working tree is dirty."; problems=1; }
git cat-file -e "$SHA:$ZIP" 2>/dev/null || { echo "! $ZIP is not in HEAD."; problems=1; }
git branch -r --contains "$SHA" 2>/dev/null | grep -q . || { echo "! $SHA is not on any remote branch."; problems=1; }
[ "$problems" -ne 0 ] && echo

if [ "$problems" -ne 0 ] && [ "$CONFIRM" = "yes" ]; then
    echo "Refusing to post while any of the above is true."
    exit 1
fi

echo "--- new issue"
echo "    repo:  $repo"
echo "    title: $title"
echo "--- then an entry in platform#$TRACKER"
echo "--- then a comment on vaadin/flow#$ORIGINAL naming it"
echo

if [ "$CONFIRM" != "yes" ]; then
    echo "Nothing was sent. Re-run with --confirm."
    exit 0
fi

body=$(mktemp)
sed -e '1,/^---$/d' -e "s/COMMIT_SHA/$SHA/g" "$DRAFT" > "$body"
url=$(gh issue create --repo "$repo" --title "$title" --body-file "$body")
rm -f "$body"
echo "created $url"
add_to_tracker "$url"

note=$(mktemp)
sed "s|DOCS_URL|$url|g" "$COMMENT" > "$note"
gh issue comment "$ORIGINAL" --repo vaadin/flow --body-file "$note"
rm -f "$note"
echo "commented on vaadin/flow#$ORIGINAL"
