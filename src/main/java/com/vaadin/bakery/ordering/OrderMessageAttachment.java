package com.vaadin.bakery.ordering;

import com.vaadin.bakery.base.AbstractEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
public class OrderMessageAttachment extends AbstractEntity {

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String filename;

    @NotBlank
    @Pattern(regexp = "image/(png|jpeg|webp)|application/pdf")
    @Column(nullable = false, length = 64)
    private String contentType;

    @Max(2097152)
    @Column(nullable = false)
    private long sizeBytes;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] data;

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public byte[] getData() {
        return data == null ? null : data.clone();
    }

    public void setData(byte[] data) {
        this.data = data == null ? null : data.clone();
    }
}
