package com.vaadin.bakery.base.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.html.Div;
import org.junit.jupiter.api.Test;

/**
 * Row actions are a named pattern, so nothing renders as "EditDelete".
 *
 * The spacing itself is one rule in {@code base/layout.css}. What this holds is
 * the hook it hangs on: a view that builds its actions any other way loses the
 * spacing silently, which is how three views ended up with none.
 */
class RowActionsTest {

    @Test
    void theRowCarriesTheClassTheOneSpacingRuleNeeds() {
        var actions = RowActions.of(new Div(), new Div());

        assertTrue(actions.getClassNames().contains("row-actions"));
        assertEquals(2, actions.getComponentCount());
    }
}
