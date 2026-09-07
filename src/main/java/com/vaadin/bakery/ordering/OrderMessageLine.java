package com.vaadin.bakery.ordering;

import java.time.Instant;
import java.util.List;

/** One message in the conversation, ready to render. */
public record OrderMessageLine(Long id, String authorName, boolean fromStaff, String text, Instant sentAt,
        boolean readByStaff, List<Attachment> attachments) {

    public record Attachment(Long id, String filename, String contentType, boolean image) {
    }
}
