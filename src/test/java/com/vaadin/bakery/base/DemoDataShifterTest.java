package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

/**
 * DOM-09. The whole week rule is the reason the dataset survives being booted
 * on an arbitrary day, so it is tested on its own rather than through the
 * application.
 */
class DemoDataShifterTest {

    private static final LocalDate ANCHOR = LocalDate.of(2026, 1, 5); // a Monday

    private long weekShiftFor(LocalDate today) {
        long days = ChronoUnit.DAYS.between(ANCHOR, today);
        return Math.floorDiv(days, 7) * 7;
    }

    @Test
    void theAnchorIsAMonday() {
        assertEquals(DayOfWeek.MONDAY, ANCHOR.getDayOfWeek());
    }

    @Test
    void everyShiftPreservesTheWeekday() {
        for (int offset = 0; offset < 400; offset++) {
            var today = ANCHOR.plusDays(offset);
            long shift = weekShiftFor(today);
            assertEquals(0, shift % 7, "the shift is always whole weeks");
            assertEquals(ANCHOR.getDayOfWeek(), ANCHOR.plusDays(shift).getDayOfWeek(),
                    "a seeded Monday stays a Monday after shifting for " + today);
        }
    }

    @Test
    void theDataIsNeverMoreThanSixDaysStale() {
        for (int offset = 0; offset < 400; offset++) {
            var today = ANCHOR.plusDays(offset);
            var shiftedAnchor = ANCHOR.plusDays(weekShiftFor(today));
            long staleness = ChronoUnit.DAYS.between(shiftedAnchor, today);
            assertTrue(staleness >= 0 && staleness <= 6,
                    "staleness was " + staleness + " days for " + today);
        }
    }

    @Test
    void aClockCanFreezeTheDate() {
        var clock = Clock.fixed(LocalDate.of(2026, 1, 7).atStartOfDay(ZoneOffset.UTC).toInstant(),
                ZoneId.of("UTC"));
        assertEquals(LocalDate.of(2026, 1, 7), LocalDate.now(clock));
    }
}
