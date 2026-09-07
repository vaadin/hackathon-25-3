package com.vaadin.bakery.base;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;

/**
 * Identity is the id alone. The version must never take part in equality: an
 * optimistic lock bump does not make an entity a different entity.
 */
@MappedSuperclass
public abstract class AbstractEntity {

    @Id
    @SequenceGenerator(name = "entity_seq", sequenceName = "entity_seq", initialValue = 10000, allocationSize = 50)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_seq")
    private Long id;

    @Version
    @Column(nullable = false)
    private int version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public boolean isNew() {
        return id == null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AbstractEntity that) || getClass() != other.getClass()) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
