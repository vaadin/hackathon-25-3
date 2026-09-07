# Epic 09: Order conversation and attachments

**Goal:** a customer and the bakery talking about one order, live and safe.
**Feature spec:** `specs/features/09-conversation.md`
**Dependencies:** epics 05, 07 and 08.

---

### US-9.1: The conversation

**Tasks:**
- [ ] `MessageList` in the one to one bubble variant on the tracking page and on the staff order detail
- [ ] Composer with a text area, enter to send, shift enter for a line break
- [ ] Author naming rules: staff see the customer, customers see the bakery and a first name

**25.3 APIs:** MessageList bubble and one to one variants.
**Verified by:** `ConversationBrowserlessTest`

---

### US-9.2: Attachments

**Tasks:**
- [ ] Modular upload in the composer, images and PDF, 2 MB cap
- [ ] Image previews inside the bubble, PDFs as named links
- [ ] Clipboard paste of an image straight into the composer

**25.3 APIs:** MessageList attachments, Clipboard paste.
**Verified by:** `AttachmentValidationBrowserlessTest`, `ClipboardPasteIT`

---

### US-9.3: Live delivery and unread state

**Tasks:**
- [ ] Delivery through the shared signal of epic 08, both directions
- [ ] Unread badge from a computed signal, cleared when the conversation is opened
- [ ] New message pill instead of scroll jumping when the reader is scrolled up

**Verified by:** `ConversationMultiUserBrowserlessTest`

---

### US-9.4: Safety and lifecycle

**Tasks:**
- [ ] Sanitize every message with the shared Safelist, render as text and never as markdown
- [ ] Disable the composer once the order is picked up, with an explanation
- [ ] Conversation unreachable without a valid tracking token

**Verified by:** `SanitizationTest`, `ConversationBrowserlessTest`

---

## Left undone

- The conversation is not live. Messages are read when the panel is built, so a reply arrives when the other side navigates rather than when it is sent. This is the one place where a shared signal was specified and a plain query was written, and it is why `ConversationMultiUserBrowserlessTest` does not exist.
- No new message pill: with delivery not live there is nothing to announce, and a reader scrolled up would be jumped if there were.

## Definition of Done

- [ ] Every acceptance criterion in `features/09-conversation.md` is checked
