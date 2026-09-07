package com.vaadin.bakery.ordering;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Slots are computed, never stored. A slot is a location plus a date plus a
 * time; its capacity comes from the location unless a closure removes it, and
 * its load is a count of orders. One query covers a whole range, which is what
 * makes the calendar metadata affordable.
 */
@Service
public class SlotService {

    private static final List<OrderState> COUNTING_STATES = List.of(OrderState.NEW, OrderState.CONFIRMED,
            OrderState.IN_PREPARATION, OrderState.READY, OrderState.PICKED_UP, OrderState.PROBLEM);

    private final OrderRepository orders;
    private final PickupClosureRepository closures;
    private final Clock clock;

    public SlotService(OrderRepository orders, PickupClosureRepository closures, Clock clock) {
        this.orders = orders;
        this.closures = closures;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Every time the location serves on that day, ignoring bookings. */
    public List<LocalTime> slotTimes(PickupLocation location) {
        List<LocalTime> times = new ArrayList<>();
        for (LocalTime time = location.getOpensAt(); time.isBefore(location.getClosesAt());
                time = time.plusMinutes(location.getSlotMinutes())) {
            times.add(time);
        }
        return times;
    }

    @Transactional(readOnly = true)
    public List<DaySlotLoad> load(PickupLocation location, LocalDate from, LocalDate to, int earliestLeadDays) {
        var closuresByDate = new HashMap<LocalDate, PickupClosure>();
        for (PickupClosure closure : closures.findForLocation(location, from, to)) {
            closuresByDate.putIfAbsent(closure.getDate(), closure);
        }

        var bookedByDate = new HashMap<LocalDate, Long>();
        for (OrderRepository.SlotLoadRow row : orders.loadPerSlot(location, from, to)) {
            bookedByDate.merge(row.getDate(), row.getBooked(), Long::sum);
        }

        int slotsPerDay = slotTimes(location).size();
        int dayCapacity = slotsPerDay * location.getSlotCapacity();
        LocalDate earliest = today().plusDays(earliestLeadDays);

        List<DaySlotLoad> result = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            var closure = closuresByDate.get(date);
            boolean closedWeekday = !location.isOpenOn(date.getDayOfWeek());
            boolean tooSoon = date.isBefore(earliest);
            String reason = null;
            if (closure != null && closure.isWholeDay()) {
                reason = closure.getReason();
            } else if (closedWeekday) {
                reason = "ordering.slot.closedWeekday";
            } else if (tooSoon) {
                reason = "ordering.slot.leadTime";
            }
            int booked = Math.toIntExact(bookedByDate.getOrDefault(date, 0L));
            result.add(new DaySlotLoad(date, dayCapacity, booked, reason != null, reason));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<SlotOption> options(PickupLocation location, LocalDate date) {
        var closed = closures.findForLocation(location, date, date);
        var booked = new HashMap<LocalTime, Long>();
        for (OrderRepository.SlotLoadRow row : orders.loadPerSlot(location, date, date)) {
            booked.merge(row.getTime(), row.getBooked(), Long::sum);
        }
        List<SlotOption> options = new ArrayList<>();
        for (LocalTime time : slotTimes(location)) {
            boolean blocked = closed.stream().anyMatch(closure -> closure.covers(time));
            if (blocked) {
                continue;
            }
            options.add(new SlotOption(time, location.getSlotCapacity(),
                    Math.toIntExact(booked.getOrDefault(time, 0L))));
        }
        return options;
    }

    /** The default the date time picker offers once a date is chosen. */
    @Transactional(readOnly = true)
    public Optional<LocalTime> nextFreeTime(PickupLocation location, LocalDate date) {
        LocalTime notBefore = date.equals(today()) ? LocalTime.now(clock) : LocalTime.MIN;
        return options(location, date).stream()
                .filter(SlotOption::isAvailable)
                .map(SlotOption::time)
                .filter(time -> !time.isBefore(notBefore))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public boolean hasCapacity(PickupLocation location, LocalDate date, LocalTime time) {
        long booked = orders.countByPickupLocationAndPickupDateAndPickupTimeAndStateIn(location, date, time,
                COUNTING_STATES);
        return booked < location.getSlotCapacity();
    }

    static List<OrderState> countingStates() {
        return COUNTING_STATES;
    }
}
