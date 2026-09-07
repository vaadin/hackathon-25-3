package com.vaadin.bakery.people;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** DOM-05 and the partial match search behind the counter picker. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Transactional
class CustomerServiceTest {

    @Autowired
    private CustomerService customers;

    @Autowired
    private CustomerRepository repository;

    @Test
    void theSameEmailInAnyCaseIsTheSamePerson() {
        long before = repository.count();

        var first = customers.findOrCreate("Ana", "Ruiz", "Ana.Ruiz@Example.com", "+34 600 111 222");
        var second = customers.findOrCreate("Ana Maria", "Ruiz", "ana.ruiz@example.com", "+34 600 333 444");

        assertEquals(first.getId(), second.getId(), "one person, one row");
        assertEquals(before + 1, repository.count());
        assertEquals("Ana Maria", second.getFirstName(), "the newest submission updates the name");
        assertEquals("+34 600 333 444", second.getPhone());
    }

    @Test
    void searchMatchesAnyFragmentOfNameEmailOrPhone() {
        var customer = customers.findOrCreate("Bartolome", "Escalante", "bart.escalante@example.com",
                "+34 611 222 333");

        assertTrue(customers.search("scalan", 10).contains(customer), "a fragment from the middle of a surname");
        assertTrue(customers.search("bart.esc", 10).contains(customer), "part of an email");
        assertTrue(customers.search("611222", 10).contains(customer), "part of a phone number, spaces ignored");
        assertFalse(customers.search("", 10).size() > 0, "an empty term finds nothing rather than everything");
    }
}
