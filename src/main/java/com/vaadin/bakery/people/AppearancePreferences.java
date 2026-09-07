package com.vaadin.bakery.people;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Where somebody's choice of theme is kept between visits.
 *
 * It was kept in the session, which meant it survived a navigation and not a
 * logout. A preference that has to be set again every morning is not one
 * anybody sets, so it belongs to the person and not to the session.
 *
 * Both values are nullable and null means never chosen, which is different from
 * chosen and happens to match the default: a person who deliberately picks the
 * default keeps it, and the day the default changes their choice does not.
 */
@Service
public class AppearancePreferences {

    /** What was stored, or nothing when this person has never chosen. */
    public record Choice(String theme, Boolean dark) {
    }

    private final UserRepository users;

    public AppearancePreferences(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Optional<Choice> of(String email) {
        return users.findByEmailIgnoreCase(email)
                .filter(user -> user.getTheme() != null || user.getDarkMode() != null)
                .map(user -> new Choice(user.getTheme(), user.getDarkMode()));
    }

    @Transactional
    public void remember(String email, String theme, boolean dark) {
        users.findByEmailIgnoreCase(email).ifPresent(user -> {
            user.setTheme(theme);
            user.setDarkMode(dark);
            users.save(user);
        });
    }
}
