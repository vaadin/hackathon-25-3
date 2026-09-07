package com.vaadin.bakery.base.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/**
 * The two or three controls that belong to one row of a grid.
 *
 * They were a bare {@code Div}, so they rendered as "EditDelete" with nothing
 * between them, in every view that had them. Giving the pattern a name means
 * the spacing is decided once, in one stylesheet rule, rather than remembered
 * in each view that grows a second action.
 */
public final class RowActions {

    private RowActions() {
    }

    public static Div of(Component... actions) {
        var row = new Div(actions);
        row.addClassName("row-actions");
        return row;
    }
}
