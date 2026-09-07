package com.vaadin.bakery.base.security;

import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.people.User;
import com.vaadin.bakery.people.UserRepository;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** The signed in staff member, or empty for an anonymous visitor. */
@Component
public class CurrentUser {

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<String> username() {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.ofNullable(authentication.getName());
    }

    public Optional<User> get() {
        return username().flatMap(userRepository::findByEmailIgnoreCase);
    }

    public User require() {
        return get().orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    public boolean hasRole(Role role) {
        return get().map(user -> user.getRole() == role).orElse(false);
    }
}
