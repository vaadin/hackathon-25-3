package com.vaadin.bakery.catalogue;

import com.vaadin.bakery.base.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Uploaded images live in the database so a restart does not lose them. Seeded
 * images stay on disk as static resources, and the resolver prefers this row.
 */
@Entity
public class ProductImage extends AbstractEntity {

    @NotNull
    @OneToOne(optional = false)
    private Product product;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String filename;

    @NotBlank
    @Pattern(regexp = "image/(png|jpeg|webp)")
    @Column(nullable = false, length = 64)
    private String contentType;

    @Max(2097152)
    @Column(nullable = false)
    private long sizeBytes;

    @Lob
    @Column(nullable = false)
    @jakarta.persistence.Basic(fetch = FetchType.LAZY)
    private byte[] data;

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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
