# Feature 09: Order conversation and attachments

## Overview

A customer and the bakery talking about one order, on the tracking page and on the staff order detail. The 25.3 MessageList variants make this look like a conversation rather than a log.

Covers C2, C3, B10.

## Behaviour

- The conversation is a `MessageList` in the one to one bubble variant: the customer's messages on one side, the bakery's on the other, each with an avatar and a time.
- Staff see the customer's name. The customer sees "Bakery" and the first name of whoever replied.
- The composer is a text area with a send action. Enter sends, shift enter adds a line.
- Attachments use the same modular Upload set as feature 07, limited to images and PDF, at most 2 MB. Images render as previews inside the message bubble, PDFs as a named link.
- An image can be pasted from the clipboard straight into the composer, which is how a customer sends a reference photo of a cake.
- Delivery is live through the shared signal of feature 08. A message written by staff appears on the customer's open tracking page without a reload, and the other way round.
- The staff order detail shows an unread badge, bound to a computed signal. Opening the conversation marks messages read.
- Messages are sanitized with the shared Safelist before rendering. A message is plain text, it is never markdown, because it comes from an untrusted party.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| An attachment exceeds the limit | Rejected in the file list with the size named, the message is not sent |
| A paste contains no image | Ignored with a hint |
| The order is picked up | The conversation stays readable but the composer is disabled with an explanation |
| A message arrives while the page is scrolled up | A "new message" pill appears rather than yanking the scroll position |
| Two staff reply at once | Both messages appear in order, no lost update |
| The customer's link token is wrong | The conversation is not reachable at all |

## Acceptance criteria

### AC1: It reads like a conversation
- [ ] Customer and bakery messages are visually distinct, with author and time
- [ ] The list uses the one to one bubble variant

### AC2: Attachments work
- [ ] An uploaded image appears as a preview in the bubble
- [ ] A pasted image behaves the same as an uploaded one
- [ ] An oversized or wrong type file is rejected with a reason

### AC3: It is live
- [ ] A staff reply appears on the customer's open page without a reload
- [ ] A customer message appears on the staff detail and increments the unread badge

### AC4: It is safe
- [ ] HTML in a message is rendered as text, never as markup
- [ ] The composer is disabled once the order is picked up

### Still open

Nothing in this document is built yet.


## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| MSG-01 | An order with two messages | Opening the tracking page | Both render as bubbles on the correct sides | browserless | `ConversationBrowserlessTest` |
| MSG-02 | A customer page and a staff detail open | Staff sending a reply | The customer page shows it without a reload | browserless | `ConversationMultiUserBrowserlessTest` |
| MSG-03 | A staff detail | A customer message arriving | The unread badge increments | browserless | `ConversationMultiUserBrowserlessTest` |
| MSG-04 | The composer | Attaching a 3 MB image | Rejected with the size named | browserless | `ConversationBrowserlessTest` |
| MSG-05 | The composer | Pasting an image | It is attached and previews in the bubble | testbench | `ClipboardPasteIT` |
| MSG-06 | A message containing HTML | Rendering it | The markup shows as text | browserless | `SanitizationTest` |
| MSG-07 | A picked up order | Opening the conversation | Readable, composer disabled with an explanation | browserless | `ConversationBrowserlessTest` |
