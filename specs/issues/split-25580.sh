#!/usr/bin/env bash
#
# Splits vaadin/flow#25580 into three.
#
# Two of its four items belong in vaadin/flow-components and are created there;
# 25580 keeps the two that are Flow's, item 1 (UI.navigate) and item 4
# (BeanValidationBinder), and gets a comment saying where the others went.
#
# Nothing is sent without --confirm.
#
#   ./specs/issues/split-25580.sh            # prints what it would do
#   ./specs/issues/split-25580.sh --confirm  # creates both and comments
#
# The comment carries both new URLs, so it has to run last. That ordering is
# the reason this is a script rather than three commands.

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
        printf -- "- [ ] %s\n" "$url" >> "$body"
        gh issue edit "$TRACKER" --repo vaadin/platform --body-file "$body" > /dev/null
        echo "    added to platform#$TRACKER"
    fi
    rm -f "$body"
}

BINDITEMS=specs/issues/56-binditems-only-on-two-components.md
VERTICAL=specs/issues/57-verticallayout-does-not-stretch-its-children.md
COMMENT=specs/issues/57b-comment-on-flow-25580.md
ZIP=specs/issues/projects/45-older-behaviours.zip
ORIGINAL=25580

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

create() {
    local draft=$1
    local repo title body
    repo=$(sed -n 's/^REPO: //p' "$draft" | head -1)
    title=$(sed -n 's/^TITLE: //p' "$draft" | head -1)
    echo "--- $(basename "$draft")" >&2
    echo "    repo:  $repo" >&2
    echo "    title: $title" >&2
    if [ "$CONFIRM" != "yes" ]; then
        echo "    (dry run, nothing sent)" >&2
        echo "DRY-RUN-URL"
        return
    fi
    body=$(mktemp)
    sed -e '1,/^---$/d' -e "s/COMMIT_SHA/$SHA/g" "$draft" > "$body"
    local url
    url=$(gh issue create --repo "$repo" --title "$title" --body-file "$body")
    rm -f "$body"
    echo "    created $url" >&2
    add_to_tracker "$url" >&2
    echo "$url"
}

binditems_url=$(create "$BINDITEMS")
vertical_url=$(create "$VERTICAL")

echo "--- then a comment on vaadin/flow#$ORIGINAL naming both"
echo

if [ "$CONFIRM" != "yes" ]; then
    echo "Nothing was sent. Re-run with --confirm."
    exit 0
fi

note=$(mktemp)
sed -e "s|BINDITEMS_URL|$binditems_url|g" -e "s|VERTICALLAYOUT_URL|$vertical_url|g" "$COMMENT" > "$note"
gh issue comment "$ORIGINAL" --repo vaadin/flow --body-file "$note"
rm -f "$note"
echo "commented on vaadin/flow#$ORIGINAL"

echo
echo "Put both URLs in TO-POST.md, in the split section."
