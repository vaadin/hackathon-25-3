package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * POL2-01. The price column shows money and is edited in the same words.
 *
 * The cell used to be the raw {@code priceCents}, so 290 meant two euros
 * ninety and nobody could tell. Now it reads "€2.90" and is typed back in
 * whichever way the person in front of it writes an amount: with the currency
 * symbol or without, with a comma or with a full stop, with thousands grouped
 * or not. The storage did not change, which is what these numbers are.
 */
class PriceFormatTest {

    @Test
    void anAmountIsReadHoweverItIsWritten() {
        assertEquals(245, ProductAdminView.parseCents("2,45 €"), "a comma and a symbol");
        assertEquals(245, ProductAdminView.parseCents("€2.45"), "a full stop and a symbol in front");
        assertEquals(245, ProductAdminView.parseCents("2.45"), "no symbol at all");
        assertEquals(200, ProductAdminView.parseCents("2"), "a whole number of euros");
    }

    /**
     * The rule a reader uses: the last separator with one or two digits behind
     * it is the decimal point, and any other separator groups thousands. So the
     * Spanish and the English way of writing the same amount are the same
     * amount.
     */
    @Test
    void groupingIsNotADecimalPoint() {
        assertEquals(123456, ProductAdminView.parseCents("1.234,56"));
        assertEquals(123456, ProductAdminView.parseCents("1,234.56"));
        assertEquals(123400, ProductAdminView.parseCents("1.234"), "three digits behind is a thousand");
    }

    /**
     * Three digits behind the separator is grouping, whichever separator it is,
     * and that is a decision rather than an accident: "1.234" is a thousand two
     * hundred and thirty four in one convention and one point two three four in
     * another, and no rule can be right about both. Grouping is the reading
     * that matches what the cell prints, which never has more than two decimals
     * in it.
     */
    @Test
    void threeDigitsBehindTheSeparatorAreAThousand() {
        assertEquals(245500, ProductAdminView.parseCents("2,455"));
        assertEquals(245500, ProductAdminView.parseCents("2.455"));
    }

    @Test
    void nothingUsableIsNothing() {
        assertNull(ProductAdminView.parseCents(null));
        assertNull(ProductAdminView.parseCents(""));
        assertNull(ProductAdminView.parseCents("   "));
        assertNull(ProductAdminView.parseCents("free"));
    }
}
