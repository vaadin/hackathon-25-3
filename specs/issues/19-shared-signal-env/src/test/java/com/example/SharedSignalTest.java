package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.vaadin.browserless.SpringBrowserlessTest;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Two questions about one write to a shared signal, asked separately, because
 * the answers are different: the write lands, and the operation never says so.
 */
@SpringBootTest(classes = Application.class)
class SharedSignalTest extends SpringBrowserlessTest {

    @Autowired
    private Tickets tickets;

    @Test
    void theValueWriteLands() {
        navigate(CounterView.class);

        tickets.waiting().update(value -> value + 1);

        assertEquals(1, tickets.waiting().peek(), "a value signal takes the write");
    }

    @Test
    void theListWriteLandsAndItsOperationNeverCompletes() throws Exception {
        navigate(CounterView.class);
        int before = tickets.queue().peek().size();

        var operation = tickets.queue().insertLast("a ticket");

        assertEquals(before + 1, tickets.queue().peek().size(), "the list really grew");
        assertFalse(operation.result().isDone(), "and the operation has not reported anything");
        try {
            operation.result().get(2, TimeUnit.SECONDS);
            throw new AssertionError("the operation completed after all, which would be the fix");
        } catch (java.util.concurrent.TimeoutException expected) {
            // This is the finding: the write is visible and the future is not.
        }
    }
}
