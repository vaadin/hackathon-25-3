package com.vaadin.bakery.people;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.security.BakeryUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * FND-04. A locked account is refused as locked, not as bad credentials, so the
 * person at the counter knows to call an administrator instead of retyping.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class LoginBrowserlessTest {

    @Autowired
    private BakeryUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void demoAccountsAuthenticate() {
        var admin = userDetailsService.loadUserByUsername("admin@bakery.test");
        assertTrue(passwordEncoder.matches("admin", admin.getPassword()), "admin password should match");
        assertTrue(admin.isAccountNonLocked());
        assertEquals("ROLE_ADMIN", admin.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void lockedAccountIsReportedAsLocked() {
        var locked = userDetailsService.loadUserByUsername("locked@bakery.test");
        assertTrue(passwordEncoder.matches("locked", locked.getPassword()), "the password itself is correct");
        assertFalse(locked.isAccountNonLocked(), "and the account is still refused because it is locked");
    }
}
