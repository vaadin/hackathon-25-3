# Demo script

Two walks: five minutes for a room that wants to see 25.3, fifteen for a room that wants to see the application. Everything except the assistant works on a laptop with no key and no network. The assistant needs both, and there is nothing standing in for it: where it is off, the screen says so in red, which is itself worth showing.

Before you start: `./mvnw`, then open `/about`. It tells you what this machine can actually do, and it takes five seconds to read.

## Five minutes

1. **The calendar knows the bakery** (`/shop`, add a celebration cake, then `/checkout/slot`). Sundays are closed, holidays are closed with the reason, days inside the cake's lead time are gone, and every remaining day shows how many places are left. That is disabled weekdays, disabled dates, the date metadata provider and a custom part name, in one screen.
   *No network needed.*

2. **Hidden columns cost nothing** (`/orders`, then `/admin/diagnostics`). Reset the counters, load a page of the board, note the query count. Turn on the expensive items column and load again: the count moves. Turn it off: it stops. There is a test that asserts this, `HiddenColumnCostBrowserlessTest`, and the panel is the same numbers coming off the service event bus.
   *No licence needed: the diagnostics view is free platform.*

3. **One board, every screen** (`/kitchen` in two browsers, signed in as `baker@bakery.test`). Move a ticket in one and watch it move in the other. There is no push code in this application: it is a shared list signal typed with a Jackson `TypeReference`.
   *No network needed.*

4. **The assistant** (`/orders/new`, needs `./mvnw -Pai` and `OPENAI_API_KEY`). Paste "hola, soy Marta, quiero dos Carrot cake y 6 Butter croissant, mi telefono es 600 123 456". The assistant fills the form and the meter shows what the turn cost. Then try "what is the salary of the baker": the policy layer refuses it before anything leaves the machine.
   *Needs a key and a network. Started without them, the panel is red and says which of the two is missing, and the form is still a form: that is the honest version and it is the one to show if the wifi is bad.*

## Fifteen minutes

Everything above, plus:

5. **Order as a customer** (`/`, add two or three things, check out). Three routed steps with a breadcrumb trail that nobody wrote by hand, draft rules while you type and submit rules at review. Finish, and follow the tracking link.

6. **Then serve that order** (`/orders` as `barista@bakery.test`). Find it by reference, expand the row to read the comments without changing the selection, confirm it. Move it through the kitchen, mark it picked up, and watch the invoice appear.

7. **The invoice** (`/admin/invoices`, print the one you just made). Real table markup, VAT summarised per rate, a print stylesheet that hides the application. Print to PDF is the deliverable: there is no PDF library in this project and there does not need to be.

8. **Change the shop from the back office** (`/admin/products`). Edit a price in the cell with GridPro, flip the availability switch and watch the product leave `/shop`. Drop a photo on the editor, or paste one from the clipboard. Then `/admin/closures`: add a closure and go back to the checkout calendar. The day is gone.

9. **Both languages, no reload** (globe icon). Everything switches, including the grid's accessible names and the dates.

10. **What it costs** (`/admin/dashboard`). Four widgets, one signal, one effect. A chart takes the columns a chart needs and a counter does not, and at phone width the whole thing becomes one column without a media query.

## If something goes wrong

| Symptom | What to do |
| --- | --- |
| The assistant panel is red | It is off, and the panel says which switch: the `ai` profile, or the key. Start it with `./mvnw -Pai` from a shell that has exported `OPENAI_API_KEY` |
| A commercial component refuses | This build expects a Vaadin Pro subscription and has no fallbacks by design. `/about` says what the machine is missing |
| The kitchen board is empty | The dataset shifts by whole weeks, so a demo late in the week can be quiet. `DemoDataShifter` already nudges a few of today's orders, but you can confirm one from `/orders` |
| Metrics are not there | They need `-Pobservability` and `docker compose up -d`. The diagnostics view works without either |
| Nothing is styled | Check that `/styles.css` and `/styles/base/layout.css` both return 200. This bit us once, and it is written up in `specs/FEEDBACK-25.3.md` |

## What to say about the beta

This application found real problems in 25.3.0-beta1 while it was being built, and they are all written down in `specs/FEEDBACK-25.3.md` with the workaround and the file it lives in. The three worth mentioning out loud are `bindChildren` being documented but absent, `peek()` throwing on a computed signal, and static CSS imports being redirected to the login view with nothing in the log to say why.
