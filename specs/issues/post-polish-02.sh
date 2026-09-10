#!/usr/bin/env bash
#
# Posts the six drafts that came out of the second polish pass.
#
# Nothing is sent without --confirm. Run it once without, read what it prints,
# and only then run it again with.
#
#   ./specs/issues/post-polish-02.sh            # prints what it would do
#   ./specs/issues/post-polish-02.sh --confirm  # actually creates the issues
#
# Each draft starts with a REPO: line and a TITLE: line followed by ---. Those
# three lines are the instructions and are stripped from what is posted; the
# repository and the title become the arguments.
#
# Each draft links its own reproducer zip by commit SHA, written in the file as
# the placeholder COMMIT_SHA. This substitutes the SHA of HEAD, so the links
# work for anybody who opens the issue: that means the zips have to be
# committed and pushed first, which is what the checks below insist on.

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

DRAFTS=(
    49-upload-custom-add-button-latches.md
    50-upload-no-capture-setter.md
    51-grid-header-join-top-row-only.md
    52-grid-column-width-is-the-minimum.md
    53-lumo-badge-theme-has-no-aura-equivalent.md
    54-lumo-button-variants-inert-under-aura.md
)

ZIPS=(
    specs/issues/projects/49-upload.zip
    specs/issues/projects/51-grid-header.zip
    specs/issues/projects/53-theme.zip
)

SHA=$(git rev-parse HEAD)
REMOTE=$(git config --get remote.origin.url || echo "no remote")

echo "HEAD is $SHA"
echo "origin is $REMOTE"
echo

# A link by SHA only works if the SHA is on the remote. These three checks are
# the difference between an issue somebody can act on and an issue whose one
# useful link is a 404.
problems=0

if [ -n "$(git status --porcelain)" ]; then
    echo "! The working tree is dirty. Commit first, or the SHA below is not what the zips contain."
    problems=1
fi

for zip in "${ZIPS[@]}"; do
    if ! git cat-file -e "$SHA:$zip" 2>/dev/null; then
        echo "! $zip is not in HEAD. Commit it, or its link will 404."
        problems=1
    fi
done

if ! git branch -r --contains "$SHA" 2>/dev/null | grep -q .; then
    echo "! $SHA is not on any remote branch. Push first, or every zip link 404s."
    problems=1
fi

echo
if [ "$problems" -ne 0 ] && [ "$CONFIRM" = "yes" ]; then
    echo "Refusing to post while any of the above is true."
    exit 1
fi

for draft in "${DRAFTS[@]}"; do
    file="specs/issues/$draft"
    repo=$(sed -n 's/^REPO: //p' "$file" | head -1)
    title=$(sed -n 's/^TITLE: //p' "$file" | head -1)

    if [ -z "$repo" ] || [ -z "$title" ]; then
        echo "! $draft has no REPO: or TITLE: header, skipped"
        continue
    fi

    # Everything after the first --- line, with the SHA filled in.
    body=$(mktemp)
    sed -e '1,/^---$/d' -e "s/COMMIT_SHA/$SHA/g" "$file" > "$body"

    echo "--- $draft"
    echo "    repo:  $repo"
    echo "    title: $title"
    echo "    body:  $(wc -l < "$body" | tr -d ' ') lines, links pinned to $SHA"

    if [ "$CONFIRM" = "yes" ]; then
        url=$(gh issue create --repo "$repo" --title "$title" --body-file "$body")
        echo "    -> $url"
        add_to_tracker "$url"
        # The link goes back into the posting list, so a finding with no link
        # stays visibly a finding nobody outside this repository has seen.
        echo "$draft $url" >> specs/issues/posted-polish-02.txt
    else
        echo "    (dry run, nothing sent)"
    fi
    rm -f "$body"
    echo
done

if [ "$CONFIRM" != "yes" ]; then
    echo "Nothing was sent. Re-run with --confirm to create these six issues."
else
    echo "Links are in specs/issues/posted-polish-02.txt."
    echo "Put each one at the end of its row in specs/FEEDBACK-25.3.md and in TO-POST.md."
fi
