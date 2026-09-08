# UI verification

Start the application and walk the story's acceptance criteria as a user would.

1. `./mvnw` in the background, or `.vaadin/vaadin-dev status` if the dev loop is running. Wait for the port to answer.
2. Log in with the account the story needs. Accounts are in `specs/02-data-set.md`.
3. For each acceptance criterion: navigate, act, and assert with `browser_snapshot`. Use the accessibility tree, not a screenshot, for anything you assert on. Take screenshots only as evidence for the review.
4. Check the console for errors after each step. A client side exception is a failure even when the screen looks right.
5. Check the same flow at phone width when the story touches a view listed as responsive in `05-theming.md`. **Resize in a tab of your own, never in the tab somebody is watching.** `browser_resize` sets the page viewport rather than the window, and it pins it for that page's whole life: the window stops following when it is dragged, and until the viewport is put back the window shows a band of dead space with no page in it. Neither is undone by resizing back, only by a fresh page, so open a second tab with `browser_tabs`, measure there, close it. Both symptoms look exactly like application bugs and cost us two false leads in one session.
6. Measure a responsive breakpoint on the component, not on the window. A `FormLayout`'s responsive steps and an `AppLayout`'s drawer both answer to the width of the element itself, so with a drawer open the window is the wrong number by the drawer's width. Read `getBoundingClientRect().width` on the component and divide by 16 to compare it against a step declared in `em`.

7. Before concluding that the platform cannot do something, ask one deliberately naive question about the capability. Every probe you write after forming a theory tests the theory, not the requirement, so a wrong theory produces a row of correct answers and one wrong conclusion. Asking the MCP "how does a Grid offer a menu at all" finds `grid.addContextMenu()` in one call; asking it four narrow questions about header cells confirms, correctly and uselessly, that a header cell cannot host a control. The rule: name the capability in the plainest words available, search for that, and only then go back to the specific path. It cost six experiments and a `javap` to learn this once.

8. Verify a download by fetching it, never by clicking it. `fetch(anchor.getAttribute('href'))` from the page gives you the status, the content type, the `content-disposition`, the length and the bytes, which is everything a criterion can ask for. A real click in an automated browser is a hazard: the browser it drives is not configured for downloads, and one export in this session coincided with that Chrome dying and the MCP server disconnecting, which cost the only copy of the log that would have explained it. Clicking is the user's job, in their own browser.

Report per criterion: pass, fail with what you saw, or not applicable with the reason. Do not write "probably fine". If you did not verify it, say you did not verify it.
