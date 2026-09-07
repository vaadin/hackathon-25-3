package com.vaadin.bakery.ordering;

import com.vaadin.bakery.people.User;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * One board, shared by every baker in the kitchen and by every customer looking
 * at a tracking page.
 *
 * The state lives in a shared signal rather than in each session, so a ticket
 * moved on the tablet by the oven moves on the screen by the counter without a
 * single line of push plumbing. The signal is typed with a Jackson
 * TypeReference, which is what makes a record round trip properly.
 */
@Component
public class KitchenBoard {

    private final SharedListSignal<KitchenTicket> tickets =
            new SharedListSignal<>(new TypeReference<KitchenTicket>() {
            });

    private final OrderService orders;
    private final Clock clock;

    public KitchenBoard(OrderService orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    public SharedListSignal<KitchenTicket> tickets() {
        return tickets;
    }

    /** Fills the board from the database. Called once, and after a hard change. */
    public synchronized void reload() {
        var today = LocalDate.now(clock);
        var loaded = orders.kitchenTickets(today, today.plusDays(1));
        tickets.peek().forEach(tickets::remove);
        loaded.forEach(tickets::insertLast);
    }

    public void ensureLoaded() {
        if (tickets.peek().isEmpty()) {
            reload();
        }
    }

    private Optional<SharedValueSignal<KitchenTicket>> find(Long orderId) {
        return tickets.peek().stream()
                .filter(signal -> signal.peek().orderId().equals(orderId))
                .findFirst();
    }

    /**
     * Moves a ticket and tells everybody. The database is the record, the signal
     * is what the room sees, and they change together.
     */
    public void advance(KitchenTicket ticket, OrderState target, User actor) {
        var order = orders.byId(ticket.orderId()).orElseThrow();
        orders.changeState(order, target, "ordering.history." + target.name().toLowerCase(), actor);
        find(ticket.orderId()).ifPresent(signal -> signal.update(current -> current.withState(target)));
        if (!target.isActiveInKitchen()) {
            find(ticket.orderId()).ifPresent(tickets::remove);
        }
    }

    /** Claiming is a race, and the loser is told who won rather than shown an error. */
    public synchronized Optional<String> claim(KitchenTicket ticket, User baker) {
        var signal = find(ticket.orderId()).orElse(null);
        if (signal == null) {
            return Optional.empty();
        }
        var current = signal.peek();
        if (current.isClaimed() && !current.assignedBakerId().equals(baker.getId())) {
            return Optional.of(current.assignedBakerName());
        }
        var order = orders.byId(ticket.orderId()).orElseThrow();
        orders.assign(order, baker);
        signal.update(value -> value.withBaker(baker.getId(), baker.getFullName()));
        return Optional.empty();
    }

    public List<KitchenTicket> snapshot() {
        return tickets.peek().stream().map(SharedValueSignal::peek).toList();
    }
}
