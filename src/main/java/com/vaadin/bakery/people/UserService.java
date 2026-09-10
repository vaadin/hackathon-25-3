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
     * Who can be given a ticket, for the kitchen board's assignment picker.
     *
     * A locked account is somebody who cannot sign in, and a ticket assigned to
     * one is a ticket nobody is making. The order is the order the picker
     * offers them in, which is why it is decided here rather than on screen.
     */
    @Transactional(readOnly = true)
    public List<User> bakers() {
        return users.findAll().stream()
                .filter(user -> user.getRole() == Role.BAKER)
                .filter(user -> !user.isLocked())
                .sorted(java.util.Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
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
