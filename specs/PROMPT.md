# The brief

What was asked for, before any of this existed. Kept because a specification is easier to judge when you can see the request it came from.

---

We are running a hackathon on Vaadin 25.3.0-beta1 and I want a real application out of it, not a demo of widgets. Read these first:

- The 25.2 and 25.3 training and hackathon decks.
- <https://artur.app.fi/25.3.html>, the tour of what is new.
- <https://github.com/orgs/vaadin/projects/29>, what the teams actually shipped.
- <https://github.com/vaadin/bakery-app-starter-flow-spring/>, branch v25. This is the application we are rewriting. Read it for behaviour and copy nothing.
- <https://github.com/manolo/vaadin-showcase/>, for good practices worth stealing.

Then write the specifications for a bakery that sells online and runs its counter. Visitors browse and order as guests. Staff take telephone orders, work a board shared live between kitchen screens, and manage the catalogue, the people and the invoices. New since the old one: invoicing, and a shop a visitor can use without an account.

Four things I care about, in this order.

**Specifications first.** Everything needed to implement it: the domain, the dataset, the architecture, security, theming, testing, tooling. Every feature with its acceptance criteria and its test cases written in plain language, and every epic broken into stories. Enough that an agent can finish the application from them without asking me what I meant.

**A rewrite, not a port.** From scratch. No Lit templates. Use as much of 25.3 as the application honestly needs: signals, the new components, the Table family, breadcrumbs, the modular upload, observability. If a feature has no place in a bakery, leave it out and say so.

**The tooling story.** The coding agent dev loop, the AI components, observability. It should work in Copilot and hot swap while I watch.

**Tests and looks.** Broad coverage, browserless where it can be, a browser only where it must. Style it with the Aura CSS variables so it looks like a bakery and not like a database viewer.

A static SQL dataset, plenty of products with photos, customers and users. Mobile friendly, and a PWA.

And keep a record: anything missing, awkward or broken in 25.3 goes in a file for the Vaadin teams. Finding those is half the point.

**MUST**
 - use signals where possible with as much as possible of API on it
 - use all new features in 25.3
 - we have a license, use commercial components when possible
 - we have an OpenAI token, use AI in the views they are needed
 - implementation will be performed in one unattended session, after specs are written and validated
 - there would be a second interactive session for polishing the UI and behavior with 2 steps
    - code vibing: using dev-loop and implementation only (no run tests)
    - once polish is done, unattended session for updating specs, and adding tests
 - observability should be added at the end

**OPTION** 
 - it would be a second fase out of the hackaton to port de app to core components only.
