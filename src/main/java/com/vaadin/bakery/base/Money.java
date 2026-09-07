package com.vaadin.bakery.base;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Money is integer cents everywhere. Nothing in this application multiplies a
 * price by a double.
 */
public record Money(int cents) implements Serializable, Comparable<Money> {

    public static final Currency CURRENCY = Currency.getInstance("EUR");
    public static final Money ZERO = new Money(0);

    public static Money ofCents(int cents) {
        return new Money(cents);
    }

    public Money plus(Money other) {
        return new Money(cents + other.cents);
    }

    public Money times(int quantity) {
        return new Money(cents * quantity);
    }

    /** Half up at the line, which is where VAT is legally rounded. */
    public Money percentage(int percent) {
        return new Money(Math.toIntExact(Math.round(cents * percent / 100.0)));
    }

    public String format(Locale locale) {
        var format = NumberFormat.getCurrencyInstance(locale == null ? Locale.of("es", "ES") : locale);
        format.setCurrency(CURRENCY);
        return format.format(cents / 100.0);
    }

    @Override
    public int compareTo(Money other) {
        return Integer.compare(cents, other.cents);
    }
}
