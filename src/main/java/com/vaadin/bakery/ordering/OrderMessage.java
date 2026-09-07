package com.vaadin.bakery.ordering;

import com.vaadin.bakery.base.AbstractEntity;
import com.vaadin.bakery.people.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The conversation with the customer, kept apart from the state history so the
 * message list has one clean source.
 */
@Entity
public class OrderMessage extends AbstractEntity {

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String authorName;

    @Column(nullable = false)
    private boolean fromStaff;

    @ManyToOne
    private User author;

    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String text;

    @NotNull
    @Column(nullable = false)
    private Instant sentAt = Instant.now();

    @Column(nullable = false)
    private boolean readByStaff;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "message_id")
    @OrderColumn(name = "position")
    private List<OrderMessageAttachment> attachments = new ArrayList<>();

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public boolean isFromStaff() {
        return fromStaff;
    }

    public void setFromStaff(boolean fromStaff) {
        this.fromStaff = fromStaff;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isReadByStaff() {
        return readByStaff;
    }

    public void setReadByStaff(boolean readByStaff) {
        this.readByStaff = readByStaff;
    }

    public List<OrderMessageAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<OrderMessageAttachment> attachments) {
        this.attachments = attachments;
    }
}
