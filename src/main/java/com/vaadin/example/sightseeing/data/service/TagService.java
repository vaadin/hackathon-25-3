package com.vaadin.example.sightseeing.data.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.vaadin.example.sightseeing.data.entity.Tag;

@Service
public class TagService {

    private final TagRepository repository;

    @Autowired
    public TagService(TagRepository repository) {
        this.repository = repository;
    }

    public Optional<Tag> get(Long id) {
        return repository.findById(id);
    }

    public List<Tag> update(Collection<Tag> entities) {
        return repository.saveAll(entities);
    }

    public Tag update(Tag entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Tag> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
