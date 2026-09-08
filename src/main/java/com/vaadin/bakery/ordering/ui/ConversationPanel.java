package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.ordering.OrderMessageLine;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.people.User;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.bakery.ordering.OrderActivity;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.messages.MessageListVariant;
import com.vaadin.flow.component.notification.Notification;
import java.util.ArrayList;
import java.util.List;

/**
 * The conversation about one order, used by both sides.
 *
 * The one to one bubble variants turn what would be a log into something that
 * reads like a conversation, which matters when the person on the other end is
 * a customer worried about a cake. Attachments arrive by drop, by button or
 * from the clipboard, because a reference photo is usually already copied.
 */
public class ConversationPanel extends Composite<Div> {

    private final OrderService orders;
    private final OrderActivity activity;
    private final String reference;
    private final boolean staffSide;
    private final User staffUser;
    private final String authorName;
    private final MessageList list = new MessageList();
    private final java.util.function.Supplier<List<Event>> events;

    /**
     * Something that happened to the order rather than something somebody said,
     * merged into the same list: a state change is part of an order's history
     * exactly as a message is, and two lists side by side made a reader join
     * them up by timestamp in their head.
     */
    public record Event(java.time.Instant when, String author, String text) {
    }

    public ConversationPanel(OrderService orders, OrderActivity activity, String reference,
            boolean staffSide, User staffUser, String authorName, boolean closed) {
        this(orders, activity, reference, staffSide, staffUser, authorName, closed, null);
    }

    public ConversationPanel(OrderService orders, OrderActivity activity, String reference,
            boolean staffSide, User staffUser, String authorName, boolean closed,
            java.util.function.Supplier<List<Event>> events) {
        this.events = events;
        this.orders = orders;
        this.activity = activity;
        this.reference = reference;
        this.staffSide = staffSide;
        this.staffUser = staffUser;
        this.authorName = authorName;

        getContent().addClassName("conversation");
        if (events == null) {
            // A conversation on its own reads as a conversation: bubbles, two
            // sides. Merged with an order's history it reads as a log, and
            // bubbles around "In the oven" would be pretending otherwise.
            list.addThemeVariants(MessageListVariant.BUBBLE, MessageListVariant.ONE_TO_ONE);
            getContent().add(Translations.bindText(new H3(), "conversation.title"));
        } else {
            getContent().addClassName("conversation--merged");
        }
        getContent().add(list);

        if (closed) {
            getContent().add(Translations.bindText(new Paragraph(), "conversation.closed"));
        } else {
            getContent().add(composer());
        }

        if (staffSide) {
            orders.markMessagesRead(reference);
        }
        // The list follows the conversation's shared signal, so a reply typed
        // at the counter appears on the customer's tracking page without either
        // of them reloading anything. The effect reads the signal, so it tracks
        // it; the messages themselves still come from the database, because
        // that is where the attachments and the read marks are.
        Signal.effect(this, () -> {
            activity.forOrder(reference).get();
            refresh();
        });
        // The author column says "the bakery" for staff messages, so the whole
        // list has to be rebuilt when the language changes.
        Translations.onLocale(this, locale -> refresh());
    }

    /**
     * A message and a Send.
     *
     * The attachment half was removed rather than left looking usable: the
     * button opened a file chooser and nothing arrived. A control that does
     * nothing is worse than an absent one, and the messages themselves still
     * render attachments, so anything already posted still shows.
     */
    private Div composer() {
        var input = new MessageInput();
        input.addSubmitListener(event -> {
            try {
                orders.post(reference, authorName, staffSide, staffUser, event.getValue(), List.of());
                // No refresh here: posting moves the shared signal, and the
                // effect above redraws this panel along with every other one.
            } catch (com.vaadin.bakery.base.error.DomainException failure) {
                Notification.show(getTranslation(failure.translationKey(), failure.arguments()));
            }
        });

        var composer = new Div(input);
        composer.addClassName("conversation__composer");
        return composer;
    }

    private static final java.time.format.DateTimeFormatter STAMP =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm d MMM");

    /** One line: who, when, what. No year, because every order is this year. */
    private MessageListItem line(java.time.Instant when, String author, String text) {
        var stamp = when.atZone(java.time.ZoneId.systemDefault()).format(STAMP.withLocale(getLocale()));
        // The time goes in the text rather than in the item's own time field:
        // that field is formatted by the component as a full date, "Sep 6,
        // 2026, 11:10 AM", which is three quarters noise on a list where every
        // row is from this week.
        return new MessageListItem(stamp + "  " + text, null, author);
    }

    public final void refresh() {
        var rows = new ArrayList<java.util.Map.Entry<java.time.Instant, MessageListItem>>();
        if (events != null) {
            for (Event event : events.get()) {
                rows.add(java.util.Map.entry(event.when(), line(event.when(), event.author(), event.text())));
            }
        }
        var items = new ArrayList<MessageListItem>();
        for (OrderMessageLine message : orders.messages(reference)) {
            var item = new MessageListItem(message.text(), message.sentAt(),
                    message.fromStaff() ? getTranslation("conversation.bakery") : message.authorName());
            // Colour index separates the two sides in the one to one variant.
            item.setUserColorIndex(message.fromStaff() ? 1 : 2);
            if (!message.attachments().isEmpty()) {
                // Served by the application from the database, never from a path
                // somebody uploaded.
                item.setAttachments(message.attachments().stream()
                        .map(attachment -> new MessageListItem.Attachment(attachment.filename(),
                                attachment.contentType(), "attachments/" + attachment.id()))
                        .toList());
            }
            items.add(item);
            rows.add(java.util.Map.entry(message.sentAt(), events == null ? item
                    : line(message.sentAt(), message.fromStaff()
                            ? getTranslation("conversation.bakery")
                            : OrderDetailView.firstName(message.authorName()), message.text())));
        }
        if (events == null) {
            list.setItems(items);
            return;
        }
        // One order, one chronology.
        rows.sort(java.util.Map.Entry.comparingByKey());
        list.setItems(rows.stream().map(java.util.Map.Entry::getValue).toList());
    }

    public MessageList list() {
        return list;
    }
}
