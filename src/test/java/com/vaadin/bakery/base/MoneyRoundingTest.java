package com.vaadin.bakery.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * DOM-06. VAT rounds half up, at the line, against values chosen to break it.
 *
 * The existing coverage exercises rounding through real invoices, which proves
 * the totals of the orders the dataset happens to contain. What it cannot do is
 * choose the awkward cases: an amount whose VAT lands exactly on half a cent is
 * the only one where the rule is visible at all, and the dataset has no reason
 * to contain one.
 *
 * Half up rather than half even is deliberate. Bankers' rounding is the better
 * default in most arithmetic and the wrong one here: an invoice is a legal
 * document and the tax authority's rule is half up.
 */
class MoneyRoundingTest {

    static Stream<Arguments> awkwardValues() {
        return Stream.of(
                // net cents, VAT percent, expected VAT cents, why this one
                Arguments.of(10, 10, 1, "exactly one cent, no rounding at all"),
                Arguments.of(5, 10, 1, "half a cent goes up, not to even"),
                Arguments.of(15, 10, 2, "one and a half cents goes up, and two is even anyway"),
                Arguments.of(25, 10, 3, "two and a half goes up to three, where half even would say two"),
                Arguments.of(35, 10, 4, "three and a half goes up to four"),
                Arguments.of(45, 10, 5, "four and a half goes up to five, not down to four"),
                Arguments.of(1, 21, 0, "a fifth of a cent disappears"),
                Arguments.of(3, 21, 1, "and two thirds of one becomes a whole"),
                Arguments.of(238, 21, 50, "49.98 rounds to fifty"),
                Arguments.of(1190, 21, 250, "a round two fifty"),
                Arguments.of(999, 21, 210, "209.79 rounds to two hundred and ten"),
                Arguments.of(100000, 10, 10000, "the largest price this catalogue allows"),
                Arguments.of(0, 21, 0, "nothing is taxed at nothing"));
    }

    @ParameterizedTest(name = "{0} cents at {1} percent is {2} cents of VAT, because {3}")
    @MethodSource("awkwardValues")
    void vatRoundsHalfUp(int netCents, int percent, int expectedVatCents, String why) {
        var vat = Money.ofCents(netCents).percentage(percent);

        assertEquals(expectedVatCents, vat.cents(), why);
    }

    /**
     * And the line is where it rounds, not the invoice. Three lines of five
     * cents at ten percent are three one cent roundings and not one, which is
     * the difference between three cents of VAT and two.
     */
    @org.junit.jupiter.api.Test
    void roundingHappensPerLineAndNotOnTheTotal() {
        var perLine = Money.ofCents(5).percentage(10).cents() * 3;
        var onTheTotal = Money.ofCents(15).percentage(10).cents();

        assertEquals(3, perLine, "three lines, three roundings up");
        assertEquals(2, onTheTotal, "the same money in one line rounds once");
    }
}
