package com.vaadin.bakery.ordering;

import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.catalogue.Product;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.bakery.people.Customer;
import com.vaadin.bakery.people.CustomerService;
import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.people.User;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orders;
    private final ProductRepository products;
    private final SlotService slots;
    private final Clock clock;
    private final CustomerService customers;
    private final OrderActivity activity;

    public OrderService(OrderRepository orders, ProductRepository products, SlotService slots, Clock clock,
            CustomerService customers, OrderActivity activity) {
        this.orders = orders;
        this.products = products;
        this.slots = slots;
        this.clock = clock;
        this.customers = customers;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public Optional<Order> byReference(String reference) {
        return orders.findByReference(reference);
    }

    @Transactional(readOnly = true)
    public Optional<Order> byReferenceAndToken(String reference, String token) {
        return orders.findByReference(reference)
                .filter(order -> token != null && order.getTrackingToken().equals(token));
    }

    /** Row details, resolved while a session is still open. */
    @Transactional(readOnly = true)
    public List<OrderDetailLine> detailLines(String reference) {
        return orders.findByReference(reference)
                .map(order -> order.getItems().stream()
                        .map(item -> new OrderDetailLine(item.getProduct().getId(), item.getQuantity(),
                                item.getProduct().getName(), item.getComment(),
                                item.getProduct().getAllergens().stream()
                                        .map(allergen -> allergen.translationKey())
                                        .sorted()
                                        .toList(),
                                item.gross()))
                        .toList())
                .orElse(List.of());
    }

    /** The conversation, as data. */
    @Transactional(readOnly = true)
    public List<OrderMessageLine> messages(String reference) {
        return orders.findByReference(reference)
                .map(order -> order.getMessages().stream()
                        .map(message -> new OrderMessageLine(message.getId(), message.getAuthorName(),
                                message.isFromStaff(), message.getText(), message.getSentAt(),
                                message.isReadByStaff(),
                                message.getAttachments().stream()
                                        .map(attachment -> new OrderMessageLine.Attachment(attachment.getId(),
                                                attachment.getFilename(), attachment.getContentType(),
                                                attachment.isImage()))
                                        .toList()))
                        .toList())
                .orElse(List.of());
    }

    /** Posting is what makes the badge move, so it also marks staff messages read. */
    @Transactional
    public OrderMessageLine post(String reference, String authorName, boolean fromStaff, User author, String text,
            List<OrderMessageAttachment> attachments) {
        var order = orders.findByReference(reference)
                .orElseThrow(() -> new com.vaadin.bakery.base.error.DomainException.NotFound(
                        "ordering.order.notFound"));
        if (order.getState() == OrderState.PICKED_UP || order.getState() == OrderState.CANCELLED) {
            throw new com.vaadin.bakery.base.error.DomainException.RuleViolation("ordering.message.closed");
        }
        var message = new OrderMessage();
        message.setAuthorName(authorName);
        message.setFromStaff(fromStaff);
        message.setAuthor(author);
        message.setText(com.vaadin.bakery.base.SafeHtml.text(text));
        message.setReadByStaff(fromStaff);
        attachments.forEach(attachment -> message.getAttachments().add(attachment));
        order.getMessages().add(message);
        orders.save(order);
        // Everybody with this order open hears about it, which is the
        // difference between a message thread and a page you have to reload.
        activity.changed(reference);
        return new OrderMessageLine(message.getId(), message.getAuthorName(), message.isFromStaff(),
                message.getText(), message.getSentAt(), message.isReadByStaff(),
                message.getAttachments().stream()
                        .map(attachment -> new OrderMessageLine.Attachment(attachment.getId(),
                                attachment.getFilename(), attachment.getContentType(), attachment.isImage()))
                        .toList());
    }

    @Transactional
    public void markMessagesRead(String reference) {
        orders.findByReference(reference).ifPresent(order -> {
            order.getMessages().forEach(message -> message.setReadByStaff(true));
            orders.save(order);
        });
    }

    @Transactional(readOnly = true)
    public long unreadMessages(String reference) {
        return orders.findByReference(reference)
                .map(order -> order.getMessages().stream()
                        .filter(message -> !message.isReadByStaff())
                        .count())
                .orElse(0L);
    }

    /** The whole history, with the author name read while the session is open. */
    @Transactional(readOnly = true)
    public List<OrderHistoryLine> historyLines(String reference) {
        return orders.findByReference(reference)
                .map(order -> order.getHistory().stream()
                        .map(entry -> new OrderHistoryLine(entry.getTimestamp(), entry.getMessage(),
                                entry.getNewState(),
                                entry.getCreatedBy() == null ? null : entry.getCreatedBy().getFullName(),
                                entry.getDetail()))
                        .toList())
                .orElse(List.of());
    }

    /** The last few history entries, already translated to keys and text. */
    @Transactional(readOnly = true)
    public List<String> recentHistoryKeys(String reference, int count) {
        return orders.findByReference(reference)
                .map(order -> order.getHistory().stream()
                        .skip(Math.max(0, order.getHistory().size() - count))
                        .map(entry -> entry.getMessage())
                        .toList())
                .orElse(List.of());
    }

    /** A one line summary of what is in the order, for the expensive column. */
    @Transactional(readOnly = true)
    public String itemsSummary(String reference) {
        return orders.findByReference(reference)
                .map(order -> order.getItems().stream()
                        .map(item -> item.getQuantity() + " x " + item.getProduct().getName())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(""))
                .orElse("");
    }

    @Transactional(readOnly = true)
    public Optional<Order> byId(Long id) {
        return orders.findById(id);
    }

    /** The kitchen board, as data. Read inside a transaction, used outside one. */
    @Transactional(readOnly = true)
    public List<KitchenTicket> kitchenTickets(LocalDate from, LocalDate to) {
        return orders.findByPickupDateBetweenAndStateInOrderByPickupDateAscPickupTimeAsc(from, to,
                        OrderState.kitchenColumns())
                .stream()
                .map(order -> {
                    var full = orders.findByReference(order.getReference()).orElse(order);
                    return new KitchenTicket(full.getId(), full.getReference(), full.getPickupDate(),
                            full.getPickupTime(), full.getCustomer().getFirstName(), full.getState(),
                            full.getItems().stream()
                                    .map(item -> item.getQuantity() + " x " + item.getProduct().getName()
                                            + (item.getComment() == null || item.getComment().isBlank()
                                                    ? "" : " (" + item.getComment() + ")"))
                                    .toList(),
                            full.getItems().stream()
                                    .flatMap(item -> item.getProduct().getAllergens().stream())
                                    .map(allergen -> allergen.translationKey())
                                    .distinct()
                                    .sorted()
                                    .toList(),
                            full.getAssignedBaker() == null ? null : full.getAssignedBaker().getFullName(),
                            full.getAssignedBaker() == null ? null : full.getAssignedBaker().getId());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Order> kitchenBoard(LocalDate from, LocalDate to) {
        return orders.findByPickupDateBetweenAndStateInOrderByPickupDateAscPickupTimeAsc(from, to,
                OrderState.kitchenColumns());
    }

    /**
     * The whole of placing an order, in one transaction. Everything is
     * revalidated here because a cart may have been open for an hour: the
     * product may have gone, the price may have changed, and the slot may have
     * filled up while the customer was choosing.
     */
    @Transactional
    public Order place(List<CartLine> cart, Customer customer, PickupLocation location, LocalDate date,
            LocalTime time, Channel channel, String customerNote, User createdBy) {
        if (cart.isEmpty()) {
            throw new DomainException.RuleViolation("ordering.cart.empty");
        }
        var order = new Order();
        order.setCustomer(customer);
        order.setPickupLocation(location);
        order.setPickupDate(date);
        order.setPickupTime(time);
        order.setChannel(channel);
        order.setCustomerNote(customerNote);
        order.setCreatedBy(createdBy);
        order.setState(OrderState.NEW);

        int maxLeadTime = 0;
        for (CartLine line : cart) {
            Product product = products.findById(line.productId())
                    .orElseThrow(() -> new DomainException.NotFound("catalogue.product.gone"));
            if (!product.isAvailable()) {
                throw new DomainException.RuleViolation("ordering.product.unavailable", product.getName());
            }
            if (!product.isAvailableOn(date.getDayOfWeek())) {
                throw new DomainException.RuleViolation("ordering.product.notOnThatDay", product.getName());
            }
            maxLeadTime = Math.max(maxLeadTime, product.getLeadTimeDays());

            var item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(line.quantity());
            item.setUnitPriceCents(product.getPriceCents());
            item.setVatRate(product.getVatRate());
            item.setComment(line.comment());
            order.getItems().add(item);
        }

        if (date.isBefore(LocalDate.now(clock).plusDays(maxLeadTime))) {
            throw new DomainException.RuleViolation("ordering.slot.leadTimeViolated", maxLeadTime);
        }
        if (!slots.hasCapacity(location, date, time)) {
            throw new DomainException.Conflict("ordering.slot.full", time.toString());
        }

        order.recalculateTotals();
        order.setReference(nextReference());
        order.setTrackingToken(newToken());
        order.addHistory(new OrderHistoryItem(OrderState.NEW, "ordering.history.placed", createdBy));
        return orders.save(order);
    }

    /**
     * The same placement, but for a screen that has not resolved a
     * {@code Customer} yet: the counter and the telephone, neither of which
     * has a session cart or a binder-validated contact step behind it.
     *
     * Resolving and placing happen in this one transaction rather than the
     * caller first calling {@code CustomerService.findOrCreate} and then this
     * class's other {@code place}: that call is {@code @Transactional} on its
     * own and commits immediately, so a refusal here (a product not baked on
     * that day, a lead time violation the client-side calendar missed, a slot
     * that filled up) would otherwise leave a created or renamed
     * {@code Customer} behind even though no order exists. Everything in this
     * method, the customer write included, rolls back together.
     */
    @Transactional
    public Order place(List<CartLine> cart, String firstName, String lastName, String email, String phone,
            PickupLocation location, LocalDate date, LocalTime time, Channel channel, String customerNote,
            User createdBy) {
        var customer = customers.findOrCreate(firstName, lastName, email, phone);
        return place(cart, customer, location, date, time, channel, customerNote, createdBy);
    }

    /**
     * Replaces an order's lines. The prices are taken again from the catalogue
     * rather than kept from the old items, because an order that is being
     * changed is being priced now.
     */
    @Transactional
    public Order updateLines(Order order, List<CartLine> lines, User actor) {
        if (lines.isEmpty()) {
            throw new DomainException.RuleViolation("ordering.cart.empty");
        }
        var current = orders.findById(order.getId())
                .orElseThrow(() -> new DomainException.NotFound("ordering.order.notFound"));
        // The caller handed us the order as their screen had it. Loading it
        // again by id throws that away, and with it the version column, so two
        // baristas editing one order both saved and the second silently won.
        // Comparing the versions is what makes the second one lose out loud.
        if (order.getVersion() != current.getVersion()) {
            throw new OptimisticLockingFailureException(
                    "Order " + current.getReference() + " changed since it was opened: the screen had version "
                            + order.getVersion() + " and the database has " + current.getVersion());
        }

        var items = new java.util.ArrayList<OrderItem>();
        for (CartLine line : lines) {
            Product product = products.findById(line.productId())
                    .orElseThrow(() -> new DomainException.NotFound("catalogue.product.gone"));
            if (!product.isAvailable()) {
                throw new DomainException.RuleViolation("ordering.product.unavailable", product.getName());
            }
            var item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(line.quantity());
            item.setUnitPriceCents(product.getPriceCents());
            item.setVatRate(product.getVatRate());
            item.setComment(line.comment());
            items.add(item);
        }

        // Mutated in place rather than replaced: the collection has orphan
        // removal, and Hibernate only tracks removals against the very
        // instance it manages, not a new list handed to the setter.
        // What the change cost, before it is applied, so the history can say
        // what moved rather than only that something did.
        var wasCents = current.getTotalGrossCents();

        current.getItems().clear();
        current.getItems().addAll(items);
        current.recalculateTotals();
        var detail = wasCents == current.getTotalGrossCents() ? null
                : wasCents + ">" + current.getTotalGrossCents();
        current.addHistory(new OrderHistoryItem(current.getState(), "ordering.history.linesChanged",
                detail, actor));
        return orders.save(current);
    }

    /**
     * The internal note, by reference rather than by the caller's copy.
     *
     * It is a scratchpad, not a figure anybody reconciles, so it does not need
     * the version check the lines get. Loading the row here is what stops it
     * needing one: saving the caller's detached copy bumped the version under
     * the screen that was holding it, and the next line save then failed
     * against work nobody else had touched.
     */
    @Transactional
    public void updateInternalNote(String reference, String note) {
        orders.findByReference(reference).ifPresent(order -> {
            order.setInternalNote(note == null || note.isBlank() ? null : note);
            orders.save(order);
        });
    }

    @Transactional
    public Order changeState(Order order, OrderState target, String message, User actor) {
        var current = orders.findById(order.getId())
                .orElseThrow(() -> new DomainException.NotFound("ordering.order.notFound"));
        if (current.getState() == target) {
            return current;
        }
        if (!current.getState().canMoveTo(target)) {
            throw new DomainException.RuleViolation("ordering.state.illegalTransition",
                    current.getState().name(), target.name());
        }
        // Enforced here rather than in the buttons, so no other path can route
        // around it. See the transition table in specs/04-security.md.
        if (actor != null && !target.settableBy(actor.getRole())) {
            throw new DomainException.RuleViolation("ordering.state.notYourRole", target.name());
        }
        // Reopening a cancelled order is the same commercial decision as
        // cancelling it, so the role rule that keeps a baker out of one keeps
        // them out of the other.
        if (current.getState() == OrderState.CANCELLED && actor != null && actor.getRole() == Role.BAKER) {
            throw new DomainException.RuleViolation("ordering.state.notYourRole", target.name());
        }
        // Read before the write, or the detail says "READY>READY".
        var was = current.getState();
        current.setState(target);
        // The transition itself, as data: the view translates both names, so a
        // change made in Spanish reads correctly in English.
        current.addHistory(new OrderHistoryItem(target, message, was + ">" + target, actor));
        var saved = orders.save(current);
        // The customer watching their tracking page is on the other end of
        // this, and so is every board showing the order.
        activity.changed(saved.getReference());
        return saved;
    }

    /** A customer may withdraw only while nothing has been baked. */
    @Transactional
    public Order cancelAsCustomer(Order order) {
        var current = orders.findById(order.getId())
                .orElseThrow(() -> new DomainException.NotFound("ordering.order.notFound"));
        if (current.getState() != OrderState.NEW) {
            throw new DomainException.RuleViolation("ordering.cancel.tooLate");
        }
        return changeState(current, OrderState.CANCELLED, "ordering.history.cancelledByCustomer", null);
    }

    @Transactional
    public Order assign(Order order, User baker) {
        var current = orders.findById(order.getId())
                .orElseThrow(() -> new DomainException.NotFound("ordering.order.notFound"));
        current.setAssignedBaker(baker);
        return orders.save(current);
    }

    @Transactional
    public Order save(Order order) {
        order.recalculateTotals();
        return orders.save(order);
    }

    String nextReference() {
        String year = String.valueOf(Year.now(clock).getValue());
        for (int attempt = 0; attempt < 50; attempt++) {
            long sequence = orders.count() + 1 + attempt;
            String candidate = "ORD-%s-%06d".formatted(year, sequence);
            if (!orders.existsByReference(candidate)) {
                return candidate;
            }
        }
        throw new DomainException.Conflict("ordering.reference.exhausted");
    }

    private static String newToken() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
