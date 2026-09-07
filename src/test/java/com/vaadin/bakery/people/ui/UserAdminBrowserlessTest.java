package com.vaadin.bakery.people.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.base.security.BakeryUserDetailsService;
import com.vaadin.bakery.people.UserRepository;
import com.vaadin.bakery.people.UserService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.grid.Grid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/** ADM-09, ADM-10 and ADM-11. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "admin@bakery.test", roles = { "ADMIN" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserAdminBrowserlessTest extends SpringBrowserlessTest {

    @org.junit.jupiter.api.BeforeEach
    void signIn() {
        // After the browserless environment is up, see TestLogin for why.
        TestLogin.asAdmin();
    }

    @Autowired
    private UserRepository repository;

    @Autowired
    private UserService users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BakeryUserDetailsService userDetails;

    @Test
    void theGridListsEveryStaffMember() {
        navigate(UserAdminView.class);
        var grid = find(Grid.class).single();
        assertEquals(repository.count(), grid.getGenericDataView().getItems().count());
    }

    @Test
    void savingWithAnEmptyPasswordKeepsTheOldOne() {
        var user = repository.findByEmailIgnoreCase("baker@bakery.test").orElseThrow();
        var before = user.getPasswordHash();

        user.setFirstName("Heidi Maria");
        users.save(user, "", null);

        var reloaded = repository.findByEmailIgnoreCase("baker@bakery.test").orElseThrow();
        assertEquals(before, reloaded.getPasswordHash(), "the password is untouched");
        assertEquals("Heidi Maria", reloaded.getFirstName(), "and the change was saved");
        assertTrue(passwordEncoder.matches("baker", reloaded.getPasswordHash()), "so the baker can still log in");
    }

    @Test
    void typingAPasswordReplacesIt() {
        var user = repository.findByEmailIgnoreCase("jonas@bakery.test").orElseThrow();

        users.save(user, "brand-new-secret", null);

        var reloaded = userDetails.loadUserByUsername("jonas@bakery.test");
        assertTrue(passwordEncoder.matches("brand-new-secret", reloaded.getPassword()));
    }

    @Test
    void aLockedAccountCannotBeEditedOrDeleted() {
        var locked = repository.findByEmailIgnoreCase("locked@bakery.test").orElseThrow();

        var editFailure = assertThrows(DomainException.RuleViolation.class,
                () -> users.save(locked, "", null));
        assertEquals("people.user.locked", editFailure.translationKey());

        var deleteFailure = assertThrows(DomainException.RuleViolation.class,
                () -> users.delete(locked, null));
        assertEquals("people.user.locked", deleteFailure.translationKey());
    }

    @Test
    void deletingYourselfIsRefused() {
        var admin = repository.findByEmailIgnoreCase("admin@bakery.test").orElseThrow();

        var failure = assertThrows(DomainException.RuleViolation.class, () -> users.delete(admin, admin));
        assertEquals("people.user.deleteSelf", failure.translationKey());
    }
}
