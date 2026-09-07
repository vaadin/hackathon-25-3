package com.vaadin.bakery.base;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A clock bean, so tests pin the date instead of depending on the day the suite
 * happens to run. Set demo.today to freeze it.
 */
@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock(@Value("${demo.today:}") String frozenDate) {
        if (frozenDate == null || frozenDate.isBlank()) {
            return Clock.systemDefaultZone();
        }
        var zone = ZoneId.systemDefault();
        var instant = ZonedDateTime.of(LocalDate.parse(frozenDate), LocalTime.of(9, 0), zone).toInstant();
        return Clock.fixed(instant, zone);
    }
}
