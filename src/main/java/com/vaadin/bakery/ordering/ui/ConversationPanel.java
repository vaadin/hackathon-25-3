package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.ordering.OrderMessageAttachment;
import com.vaadin.bakery.ordering.OrderMessageLine;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.people.User;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.bakery.ordering.Conversations;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.messages.MessageListVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadDropZone;
import com.vaadin.flow.component.upload.UploadFileList;
import com.vaadin.flow.component.upload.UploadManager;
import com.vaadin.flow.server.streams.UploadHandler;
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
    private final Conversations conversations;
    private final String reference;
    private final boolean staffSide;
    private final User staffUser;
    private final String authorName;
    private final MessageList list = new MessageList();
    private final List<OrderMessageAttachment> pending = new ArrayList<>();

    public ConversationPanel(OrderService orders, Conversations conversations, String reference,
            boolean staffSide, User staffUser, String authorName, boolean closed) {
        this.orders = orders;
        this.conversations = conversations;
        this.reference = reference;
        this.staffSide = staffSide;
        this.staffUser = staffUser;
        this.authorName = authorName;

        getContent().addClassName("conversation");
        list.addThemeVariants(MessageListVariant.BUBBLE, MessageListVariant.ONE_TO_ONE);
        getContent().add(Translations.bindText(new H3(), "conversation.title"), list);

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
            conversations.forOrder(reference).get();
            refresh();
        });
        // The author column says "the bakery" for staff messages, so the whole
        // list has to be rebuilt when the language changes.
        Translations.onLocale(this, locale -> refresh());
    }

    private Div composer() {
        var input = new MessageInput();
        input.addSubmitListener(event -> {
            try {
                orders.post(reference, authorName, staffSide, staffUser, event.getValue(), List.copyOf(pending));
                pending.clear();
                // No refresh here: posting moves the shared signal, and the
                // effect above redraws this panel along with every other one.
            } catch (com.vaadin.bakery.base.error.DomainException failure) {
                Notification.show(getTranslation(failure.translationKey(), failure.arguments()));
            }
        });

        var handler = UploadHandler.inMemory((metadata, data) -> {
            var attachment = new OrderMessageAttachment();
            attachment.setFilename(metadata.fileName());
            attachment.setContentType(metadata.contentType());
            attachment.setSizeBytes(data.length);
            attachment.setData(data);
            pending.add(attachment);
        });

        var manager = new UploadManager(this, handler);
        manager.setMaxFiles(3);
        manager.setMaxFileSize(2L * 1024 * 1024);
        manager.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp", "application/pdf");

        var button = Translations.bindText(new UploadButton("", manager), "conversation.attach");
        var files = new UploadFileList(manager);
        var dropZone = new UploadDropZone(new Div(button, files), manager);
        dropZone.addClassName("conversation__dropzone");

        // A photo of a cake is usually already in the clipboard.
        Clipboard.onFilePaste(dropZone, handler);

        var composer = new Div(input, dropZone);
        composer.addClassName("conversation__composer");
        return composer;
    }

    public final void refresh() {
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
        }
        list.setItems(items);
    }

    public MessageList list() {
        return list;
    }
}
