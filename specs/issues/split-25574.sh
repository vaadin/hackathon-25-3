#!/usr/bin/env bash
#
# Splits vaadin/flow#25574 in two.
#
# It creates the licensing half as a new issue in vaadin/license-checker, and
# then comments on 25574 saying where that half went. 25574 itself is left
# exactly as it is: it keeps part 1, the absolute route URL, which is a Flow
# question and stays in Flow.
#
# Nothing is sent without --confirm.
#
#   ./specs/issues/split-25574.sh            # prints what it would do
#   ./specs/issues/split-25574.sh --confirm  # creates the issue and comments
#
# The order matters and is why this is a script rather than two commands: the
# comment has to carry the new issue's URL, and that URL does not exist until
# the first call has returned.

set -euo pipefail

cd "$(dirname "$0")/../.."

CONFIRM=no
[ "${1:-}" = "--confirm" ] && CONFIRM=yes

# Every hackathon issue also gets a line in the platform PiT tracking issue,
# which is where somebody looks to see what this release's testing produced.
# The Hackathon section runs to the end of that body, so a new entry appends.
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

DRAFT=specs/issues/55-isvalidlicense-ignores-the-product.md
COMMENT=specs/issues/55b-comment-on-flow-25574.md
ZIP=specs/issues/projects/55-licence-probe.zip
ORIGINAL=25574

repo=$(sed -n 's/^REPO: //p' "$DRAFT" | head -1)
title=$(sed -n 's/^TITLE: //p' "$DRAFT" | head -1)
SHA=$(git rev-parse HEAD)

echo "HEAD is $SHA"
echo

problems=0
if [ -n "$(git status --porcelain)" ]; then
    echo "! The working tree is dirty. Commit first, or the SHA above is not what the zip contains."
    problems=1
fi
if ! git cat-file -e "$SHA:$ZIP" 2>/dev/null; then
    echo "! $ZIP is not in HEAD. Commit it, or its link will 404."
    problems=1
fi
if ! git branch -r --contains "$SHA" 2>/dev/null | grep -q .; then
    echo "! $SHA is not on any remote branch. Push first, or the zip link 404s."
    problems=1
fi
[ "$problems" -ne 0 ] && echo

if [ "$problems" -ne 0 ] && [ "$CONFIRM" = "yes" ]; then
    echo "Refusing to post while any of the above is true."
    exit 1
fi

echo "--- new issue"
echo "    repo:  $repo"
echo "    title: $title"
echo "--- then an entry in the platform PiT tracking issue, platform#$TRACKER"
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
sed "s|NEW_ISSUE_URL|$url|g" "$COMMENT" > "$note"
gh issue comment "$ORIGINAL" --repo vaadin/flow --body-file "$note"
rm -f "$note"
echo "commented on vaadin/flow#$ORIGINAL"

echo
echo "Put $url in TO-POST.md, in the split section, and mark 25574 as part 1 only."
