package com.vaadin.bakery.assistant;

import com.vaadin.bakery.ordering.SlotService;
import com.vaadin.bakery.ordering.ui.SlotPicker;
import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.ai.provider.ToolException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import tools.jackson.databind.JsonNode;

/**
 * Proposing a pickup slot, which the bakery then accepts or refuses.
 *
 * The specification says the model proposes and the slot service validates, and
 * that a full slot comes back with the reason. That is a conversation, not a
 * value: a field either holds a date or it does not, while this has to be able
 * to answer "not that one, and here is the next".
 *
 * It is also a tool for a duller reason. The slot picker is a
 * {@code Composite}, and form field discovery does not walk into one, so its
 * date and time are invisible to {@code FormAIController} whatever the
 * container holds. Recorded in {@code specs/FEEDBACK-25.3.md}.
 */
public final class PickupSlotTool implements LLMProvider.ToolSpec {

    private static final String SOONEST = "soonest";
    private static final int HORIZON_DAYS = 60;

    private final SlotService slots;
    private final SlotPicker picker;
    private final com.vaadin.flow.component.UI ui;

    public PickupSlotTool(SlotService slots, SlotPicker picker, com.vaadin.flow.component.UI ui) {
        this.slots = slots;
        this.picker = picker;
        this.ui = ui;
    }

    @Override
    public String getName() {
        return "propose_pickup_slot";
    }

    /**
     * The description carries today's date, because the model does not know it.
     *
     * Without it a customer asking for "el viernes" produces an empty date,
     * which this tool refuses, which the model cannot act on, which it retries.
     * Seven times, on the run that found this.
     */
    @Override
    public String getDescription() {
        return "Propose the day and time the customer wants to collect the order. Today is "
                + slots.today() + ", so work out any weekday or \"tomorrow\" the customer said from that. "
                + "If they gave no day, or you cannot work theirs out, send \"soonest\" for both and the "
                + "bakery will pick the earliest it can serve. The bakery checks the day against its "
                + "closures, its lead times and how full the slot already is, and refuses with a reason "
                + "if it cannot be served. A refusal names the next time that is free, so take it.";
    }

    @Override
    public String getParametersSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "date": { "type": "string", "description": "Pickup day as YYYY-MM-DD. Omit it, or send \"soonest\", and the bakery picks the earliest day it can serve." },
                    "time": { "type": "string", "description": "Pickup time as HH:mm on the hour or the half hour. Omit it, or send \"soonest\", and the bakery picks." }
                  },
                  "required": []
                }
                """;
    }

    @Override
    public String execute(JsonNode arguments) {
        if (arguments == null || !arguments.isObject()) {
            throw new ToolException("Arguments must be a JSON object with 'date' and 'time'.");
        }
        // The picker is a component, and this both reads it and writes it, so
        // the whole check belongs on the UI thread rather than half of it.
        return UiWork.on(ui, () -> propose(arguments));
    }

    private String propose(JsonNode arguments) {
        var location = picker.getLocation();
        if (location == null) {
            throw new ToolException("No pickup location is selected yet, so no slot can be checked. "
                    + "Ask the barista which shop the customer is collecting from.");
        }

        // A model that has never seen this bakery's calendar cannot name a day
        // it can serve, and one that cannot work out which Friday the customer
        // meant sends no day at all: watching a real turn, it omitted the
        // argument and then repeated the same call seven times against a
        // refusal it could not act on. So an absent day means the bakery
        // chooses, which is an answer, and only a day that was given and is
        // wrong is refused.
        var wantedDay = arguments.path("date").asString("").trim();
        var wantedTime = arguments.path("time").asString("").trim();

        var date = wantedDay.isEmpty() || SOONEST.equalsIgnoreCase(wantedDay)
                ? soonestDay(location)
                : parseDate(wantedDay);
        var time = wantedTime.isEmpty() || SOONEST.equalsIgnoreCase(wantedTime)
                ? slots.nextFreeTime(location, date).orElseThrow(() -> new ToolException(
                        "Nothing is free at " + location.getName() + " on " + date + ". Propose another day."))
                : parseTime(wantedTime);

        var load = picker.loadFor(date);
        if (load != null && !load.isSelectable()) {
            // closedReason is a translation key, and a key is not a sentence a
            // model can pass on. Resolve it the way the screen would.
            throw new ToolException("The bakery cannot take a pickup on " + date + ": "
                    + (load.closedReason() == null ? "that day is full"
                            : picker.getTranslation(load.closedReason()))
                    + ". Propose another day.");
        }
        if (!slots.hasCapacity(location, date, time)) {
            var next = slots.nextFreeTime(location, date)
                    .map(LocalTime::toString)
                    .orElse(null);
            throw new ToolException(date + " at " + time + " is fully booked at " + location.getName() + ". "
                    + (next == null ? "Nothing is free that day, propose another one."
                            : "The next free time that day is " + next + "."));
        }

        picker.datePicker().setValue(date);
        picker.timeSelect().setValue(time);
        return "Pickup set for " + date + " at " + time + " at " + location.getName() + ".";
    }

    /**
     * The earliest day the picker itself would let the barista choose.
     *
     * Asking the slot service directly is not the same question: the picker's
     * calendar already carries the lead time of what is in the order, so a day
     * that is free in general can still be too soon for a cake that needs two
     * days. The screen is the authority, and disagreeing with it produces a
     * proposal the same tool then refuses.
     */
    private LocalDate soonestDay(com.vaadin.bakery.ordering.PickupLocation location) {
        var today = slots.today();
        for (int offset = 0; offset <= HORIZON_DAYS; offset++) {
            var day = today.plusDays(offset);
            var load = picker.loadFor(day);
            if (load != null && load.isSelectable() && slots.nextFreeTime(location, day).isPresent()) {
                return day;
            }
        }
        throw new ToolException("The bakery has nothing free in the next " + HORIZON_DAYS
                + " days for what is on this order. Tell the barista.");
    }

    /**
     * A refusal a model can act on. Saying only that the value was wrong is
     * what makes it send the same wrong value again, so this says what today
     * is and what it could have sent instead.
     */
    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException | NullPointerException malformed) {
            throw new ToolException("\"" + raw + "\" is not a date. Today is " + slots.today()
                    + ". Send a day as YYYY-MM-DD, or send \"soonest\" and the bakery will choose.");
        }
    }

    private LocalTime parseTime(String raw) {
        try {
            return LocalTime.parse(raw.trim());
        } catch (DateTimeParseException | NullPointerException malformed) {
            throw new ToolException("\"" + raw + "\" is not a time. Send HH:mm on the hour or the half "
                    + "hour, or send \"soonest\" and the bakery will choose.");
        }
    }
}
