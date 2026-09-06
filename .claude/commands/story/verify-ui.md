# UI verification

Start the application and walk the story's acceptance criteria as a user would.

1. `./mvnw` in the background, or `.vaadin/vaadin-dev status` if the dev loop is running. Wait for the port to answer.
2. Log in with the account the story needs. Accounts are in `specs/02-data-set.md`.
3. For each acceptance criterion: navigate, act, and assert with `browser_snapshot`. Use the accessibility tree, not a screenshot, for anything you assert on. Take screenshots only as evidence for the review.
4. Check the console for errors after each step. A client side exception is a failure even when the screen looks right.
5. Check the same flow at phone width when the story touches a view listed as responsive in `05-theming.md`.

Report per criterion: pass, fail with what you saw, or not applicable with the reason. Do not write "probably fine". If you did not verify it, say you did not verify it.
