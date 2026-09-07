package com.vaadin.bakery.people;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    public List<Customer> search(String term, int limit) {
        if (term == null || term.isBlank()) {
            return List.of();
        }
        return customers.search(term.trim().replace(" ", ""), PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public Optional<Customer> byEmail(String email) {
        return customers.findByEmailIgnoreCase(email);
    }

    /**
     * One person, one row. A repeat order updates the name and the phone from
     * the newest submission, because that is the information the customer just
     * confirmed.
     */
    @Transactional
    public Customer findOrCreate(String firstName, String lastName, String email, String phone) {
        var existing = customers.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            var customer = existing.get();
            customer.setFirstName(firstName);
            customer.setLastName(lastName);
            if (phone != null && !phone.isBlank()) {
                customer.setPhone(phone);
            }
            return customers.save(customer);
        }
        var customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setCreatedAt(Instant.now());
        return customers.save(customer);
    }

    @Transactional
    public Customer save(Customer customer) {
        return customers.save(customer);
    }
}
