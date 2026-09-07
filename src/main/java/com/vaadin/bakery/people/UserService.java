package com.vaadin.bakery.people;

import com.vaadin.bakery.base.error.DomainException;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> all() {
        return users.findAll();
    }

    /**
     * The password field is empty when an editor opens, so an empty value means
     * "leave it alone" and only a typed value is re encoded.
     */
    @Transactional
    public User save(User user, String rawPasswordOrEmpty, User actor) {
        if (user.getId() != null) {
            var current = users.findById(user.getId())
                    .orElseThrow(() -> new DomainException.NotFound("people.user.notFound"));
            if (current.isLocked()) {
                throw new DomainException.RuleViolation("people.user.locked", current.getEmail());
            }
        }
        if (rawPasswordOrEmpty != null && !rawPasswordOrEmpty.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(rawPasswordOrEmpty));
        }
        return users.save(user);
    }

    @Transactional
    public void delete(User user, User actor) {
        if (actor != null && actor.getId() != null && actor.getId().equals(user.getId())) {
            throw new DomainException.RuleViolation("people.user.deleteSelf");
        }
        var current = users.findById(user.getId())
                .orElseThrow(() -> new DomainException.NotFound("people.user.notFound"));
        if (current.isLocked()) {
            throw new DomainException.RuleViolation("people.user.locked", current.getEmail());
        }
        users.delete(current);
    }

    @Transactional
    public User setLocked(User user, boolean locked) {
        var current = users.findById(user.getId())
                .orElseThrow(() -> new DomainException.NotFound("people.user.notFound"));
        current.setLocked(locked);
        return users.save(current);
    }
}
