package com.vaadin.bakery.ordering;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;

/**
 * Serves what people attached to a message. Like the product images, the bytes
 * come from the database and never from a path on disk.
 */
@RestController
public class AttachmentController {

    private final OrderMessageAttachmentRepository attachments;

    public AttachmentController(OrderMessageAttachmentRepository attachments) {
        this.attachments = attachments;
    }

    @GetMapping("/attachments/{id}")
    public ResponseEntity<byte[]> attachment(@PathVariable Long id) {
        return attachments.findById(id)
                .map(attachment -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(attachment.getContentType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                        .body(attachment.getData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
