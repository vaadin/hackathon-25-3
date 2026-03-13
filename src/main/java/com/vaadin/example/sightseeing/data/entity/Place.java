package com.vaadin.example.sightseeing.data.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;

@Entity
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_place")
    @SequenceGenerator(name = "seq_place", initialValue = 1000)
    private Long id;

    private String name;
    private Double x;
    private Double y;

    final String[] tagNames = { "name:es", "name:en", "name", "description:es",
            "description:en", "historic", "religion" };

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    // @OrderColumn // doesn't work anymore here
    @JoinColumn
    private Set<Tag> tags;

    private Long oid;

    private LocalDateTime updated;

    private boolean enabled = true;

    public Place() {
    }

    public Place(Long id, double x, double y, Map<String, String> tags,
            Instant instant) {
        oid = id;
        if (instant != null) {
            updated = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        }
        this.x = x;
        this.y = y;
        for (String k : tagNames) {
            if (tags.get(k) != null) {
                name = Jsoup.clean(tags.get(k), Safelist.basic());
                break;
            }
        }
        this.tags = tags.entrySet().stream()
                .map(e -> new Tag(this,
                        Jsoup.clean(e.getKey(), Safelist.basic()),
                        Jsoup.clean(e.getValue(), Safelist.basic())))
                .collect(Collectors.toSet());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public Long getOid() {
        return oid;
    }

    public void setOid(Long oid) {
        this.oid = oid;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "Place [id= " + getId() + "name=" + name + ", x=" + x + ", y="
                + y + ", tags=" + tags + ", oid=" + oid + ", updated=" + updated
                + ", enabled=" + enabled + "]";
    }

}
