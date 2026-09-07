package com.vaadin.bakery.ordering;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderMessageAttachmentRepository extends JpaRepository<OrderMessageAttachment, Long> {
}
