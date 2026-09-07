package com.vaadin.bakery;

import com.vaadin.bakery.people.Role;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Signs a test in.
 *
 * `@WithMockUser` writes the authentication before the browserless environment
 * exists, and with the Vaadin aware security context strategy in play that
 * context is not always the one the navigation sees, which shows up as a test
 * that passes alone and lands on the login view in a full run. Setting the
 * authentication after the environment is up is deterministic.
 */
public final class TestLogin {

    private TestLogin() {
    }

    public static void as(String email, Role role) {
        var authentication = new UsernamePasswordAuthenticationToken(email, "password",
                List.of(new SimpleGrantedAuthority(role.authority())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void asAdmin() {
        as("admin@bakery.test", Role.ADMIN);
    }

    public static void asBarista() {
        as("barista@bakery.test", Role.BARISTA);
    }

    public static void asBaker() {
        as("baker@bakery.test", Role.BAKER);
    }

    public static void out() {
        SecurityContextHolder.clearContext();
    }
}
