package com.vaadin.bakery.ordering.ui;

/**
 * A view the board hosts in its panel slot.
 *
 * Escape, a click on the backdrop and the panel's own close control are three
 * gestures for one intention, so they end in one method. A guard against losing
 * unsaved work then has to be written once, and cannot be walked around by
 * choosing a different gesture.
 */
public interface BoardPanel {

    /** Dismisses the panel, asking first when there is something to lose. */
    void close();
}
