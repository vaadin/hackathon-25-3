package com.vaadin.bakery.billing;

import com.vaadin.bakery.base.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Size;

/**
 * The bakery's own details, as they appear at the top of every invoice.
 *
 * One row, written in one place. Before this, the printed invoice carried the
 * application's name and nothing else, so the address, the company number and
 * the VAT registration that a real invoice has to state were nowhere: there was
 * no way to put them on the document short of editing a view.
 *
 * Markdown rather than a set of fields, because what belongs in a letterhead is
 * different in every country and a form with the wrong boxes on it is worse
 * than a free block of text somebody writes once.
 */
@Entity
public class BakeryDetails extends AbstractEntity {

    @Size(max = 2000)
    @Column(length = 2000)
    private String headerMarkdown;

    public String getHeaderMarkdown() {
        return headerMarkdown;
    }

    public void setHeaderMarkdown(String headerMarkdown) {
        this.headerMarkdown = headerMarkdown;
    }
}
