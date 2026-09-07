package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.ordering.DaySlotLoad;
import com.vaadin.bakery.ordering.PickupLocation;
import com.vaadin.bakery.ordering.SlotService;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.datepicker.DateMetadata;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DateMetadataProvider;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.signals.local.ValueSignal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Choosing when to pick the order up.
 *
 * This is where the 25.3 calendar work earns its place: closed weekdays and
 * closure dates are disabled outright, days that are full are disabled with a
 * reason, and every remaining day carries its remaining capacity as metadata
 * with a part name, so a nearly full Saturday looks different from an empty
 * Tuesday. Changing the location or the basket refreshes the metadata instead
 * of rebuilding the picker.
 */
public class SlotPicker extends Composite<Div> {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int HORIZON_DAYS = 60;

    private final SlotService slots;
    private final SerializableSupplier<Integer> leadTimeDays;
    private final Select<PickupLocation> location = new Select<>();
    private final DatePicker date = new DatePicker();
    private final Select<LocalTime> time = new Select<>();
    private final ValueSignal<PickupLocation> selectedLocation = new ValueSignal<>(null);
    private final Map<LocalDate, DaySlotLoad> loadByDate = new HashMap<>();

    /**
     * @param leadTimeDays
     *            how many days' notice the order in progress needs, read fresh
     *            each time the calendar reloads. The customer checkout passes
     *            the session cart's {@code maxLeadTimeDays}; a staff screen
     *            passes one computed from whatever is actually in its editor,
     *            never the customer's cart, which is the wrong state entirely
     *            for a counter or telephone order.
     */
    public SlotPicker(SlotService slots, SerializableSupplier<Integer> leadTimeDays, List<PickupLocation> locations) {
        this.slots = slots;
        this.leadTimeDays = leadTimeDays;
        getContent().addClassName("slot-picker");

        Translations.bind(location, location::setLabel, "ordering.slot.location");
        location.setItems(locations);
        location.setItemLabelGenerator(PickupLocation::getName);
        location.addValueChangeListener(event -> {
            selectedLocation.set(event.getValue());
            reloadCalendar();
        });

        Translations.bind(date, date::setLabel, "ordering.slot.date");
        date.setDateMetadataProvider(metadataProvider());
        date.addValueChangeListener(event -> reloadTimes(event.getValue()));

        Translations.bind(time, time::setLabel, "ordering.slot.time");
        time.setItemLabelGenerator(value -> value.format(TIME));

        getContent().add(location, date, time);
        if (!locations.isEmpty()) {
            location.setValue(locations.getFirst());
        }
    }

    private DateMetadataProvider metadataProvider() {
        return DateMetadataProvider.perDate(day -> {
            var load = loadByDate.get(day);
            if (load == null) {
                return new DateMetadata(day, false);
            }
            // The part name is what the theme hangs on: free, busy, full, closed.
            return new DateMetadata(day, !load.isSelectable(), load.partName());
        });
    }

    /** Everything the calendar knows comes from one query over the horizon. */
    private void reloadCalendar() {
        var current = selectedLocation.peek();
        loadByDate.clear();
        if (current == null) {
            return;
        }
        var from = slots.today();
        var to = from.plusDays(HORIZON_DAYS);
        var leadTime = leadTimeDays.get();

        var load = slots.load(current, from, to, leadTime);
        load.forEach(day -> loadByDate.put(day.date(), day));

        date.setMin(from.plusDays(leadTime));
        date.setMax(to);
        date.setDisabledWeekdays(current.getClosedWeekdays());
        date.setDisabledDates(load.stream()
                .filter(day -> !day.isSelectable())
                .map(DaySlotLoad::date)
                .toList());
        date.refreshDateMetadata();

        if (date.getValue() != null && !isSelectable(date.getValue())) {
            date.clear();
        }
        reloadTimes(date.getValue());
    }

    private boolean isSelectable(LocalDate day) {
        var load = loadByDate.get(day);
        return load != null && load.isSelectable();
    }

    private void reloadTimes(LocalDate day) {
        var current = selectedLocation.peek();
        if (current == null || day == null) {
            time.setItems(List.of());
            time.clear();
            return;
        }
        var options = slots.options(current, day).stream()
                .filter(option -> option.isAvailable())
                .map(option -> option.time())
                .toList();
        time.setItems(options);
        // The next free slot is the answer most people want, so it is prefilled.
        slots.nextFreeTime(current, day).filter(options::contains).ifPresent(time::setValue);
    }

    /** Called when the basket changes, because lead time may have moved. */
    public void refresh() {
        reloadCalendar();
    }

    public PickupLocation getLocation() {
        return location.getValue();
    }

    public LocalDate getDate() {
        return date.getValue();
    }

    public LocalTime getTime() {
        return time.getValue();
    }

    public DatePicker datePicker() {
        return date;
    }

    public Select<LocalTime> timeSelect() {
        return time;
    }

    public Select<PickupLocation> locationSelect() {
        return location;
    }

    public String reasonFor(LocalDate day) {
        var load = loadByDate.get(day);
        return load == null ? null : load.closedReason();
    }

    public DaySlotLoad loadFor(LocalDate day) {
        return loadByDate.get(day);
    }
}
