package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.bakery.ordering.SlotService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * BOARD-02 and BOARD-03. What the board shows before anybody asks, and what it
 * finds when they do.
 *
 * The default matters more than it looks. A counter opens this screen forty
 * times a day and never touches the filters, so what it shows on arrival is
 * what the board is: today and later, because yesterday's orders are somebody
 * else's problem and there are far more of them.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class BoardFilteringBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private OrderRepository orders;

    @Autowired
    private SlotService slots;

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    @SuppressWarnings("unchecked")
    private Grid<Order> board() {
        return (Grid<Order>) find(Grid.class).single();
    }

    /**
     * What the grid actually fetched. A lazy grid does not keep its items, so
     * this asks the data provider the same question the grid asked it.
     */
    private List<Order> shown() {
        // Sort orders must be a list and not null: the Spring Data helper the
        // board is populated through streams them without checking, and a null
        // there comes back as an NPE from inside the framework rather than as
        // anything about this test. See specs/FEEDBACK-25.3.md.
        return board().getDataProvider()
                .fetch(new Query<>(0, 500, List.of(), null, null))
                .map(Order.class::cast)
                .toList();
    }

    private TextField searchField() {
        return find(TextField.class).first();
    }

    private Checkbox pastToggle() {
        return find(Checkbox.class).withLabel("Show past orders").single();
    }

    @Test
    void theBoardOpensOnTodayAndLaterAndTheToggleRevealsThePast() {
        navigate(OrderBoardView.class);
        var yesterday = slots.today().minusDays(1);

        assertTrue(shown().stream().noneMatch(order -> order.getPickupDate().isBefore(yesterday)),
                "nothing older than yesterday is on the board to begin with");
        long byDefault = shown().size();

        test(pastToggle()).click();

        long withPast = shown().size();
        assertTrue(withPast > byDefault,
                "the toggle brought the past back: " + byDefault + " became " + withPast);
        assertTrue(shown().stream().anyMatch(order -> order.getPickupDate().isBefore(yesterday)),
                "and what it brought back is older than yesterday");
    }

    /**
     * A counter searches by whatever the customer says on the telephone, which
     * is as often a phone number as a name.
     */
    @Test
    void theSearchMatchesPhoneAndEmailAsWellAsNameAndReference() {
        navigate(OrderBoardView.class);
        var target = orders.findAll().stream()
                .filter(order -> order.getCustomer().getPhone() != null
                        && !order.getCustomer().getPhone().isBlank())
                .findFirst()
                .orElseThrow();

        test(searchField()).setValue(target.getCustomer().getPhone());
        assertTrue(shown().stream().anyMatch(order -> order.getId().equals(target.getId())),
                "found by telephone number");

        test(searchField()).setValue(target.getCustomer().getEmail());
        assertTrue(shown().stream().anyMatch(order -> order.getId().equals(target.getId())),
                "found by email address");

        test(searchField()).setValue(target.getReference());
        assertEquals(1, shown().size(), "a reference finds exactly one order");
    }

    /**
     * A search is a search. Somebody typing a reference means any order, not
     * only the ones still to come, so searching lifts the date filter.
     */
    @Test
    void searchingLooksPastTheDefaultDateFilter() {
        navigate(OrderBoardView.class);
        var old = orders.findAll().stream()
                .filter(order -> order.getPickupDate().isBefore(slots.today().minusDays(2)))
                .findFirst()
                .orElseThrow();

        assertFalse(shown().stream().anyMatch(order -> order.getId().equals(old.getId())),
                "the old order is not on the board by default");

        test(searchField()).setValue(old.getReference());

        assertTrue(shown().stream().anyMatch(order -> order.getId().equals(old.getId())),
                "and searching for it finds it anyway");
    }
}
