package com.vaadin.bakery.base.ui;

import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import java.util.List;

/**
 * Turns columns on and off. The menu is the full inventory: a column that is
 * not listed here is one nobody can discover, and a table that can be emptied
 * of every column is one somebody will empty by accident.
 */
public final class ColumnChooser {

    /** A column and the key naming it, so the menu reads the same as the header. */
    public record Entry<T>(Grid.Column<T> column, String labelKey) {
    }

    private ColumnChooser() {
    }

    /**
     * The menu lives in the table's own header, where a column chooser is
     * looked for, so it is an icon rather than a word: a button reading
     * "Columns" above the table was one more thing competing with the search
     * field for the top of the page. The word is still there, as the accessible
     * name and as the tooltip.
     */
    public static <T> void of(MenuBar menu, String labelKey, List<Entry<T>> entries) {
        var item = menu.addItem(new Icon(VaadinIcon.COG));
        Translations.bind(menu, text -> {
            item.getElement().setAttribute("aria-label", text);
            // A menu item is not a component with a tooltip of its own: the bar
            // owns it, and it takes the item back as an argument.
            menu.setTooltipText(item, text);
        }, labelKey);
        var submenu = item.getSubMenu();

        for (Entry<T> entry : entries) {
            var toggle = new Checkbox();
            Translations.bind(toggle, toggle::setLabel, entry.labelKey());
            toggle.setValue(entry.column().isVisible());
            toggle.addValueChangeListener(event -> {
                if (Boolean.FALSE.equals(event.getValue()) && isLastVisible(entries, entry)) {
                    // Springs back rather than leaving a table with no columns.
                    toggle.setValue(true);
                    return;
                }
                entry.column().setVisible(Boolean.TRUE.equals(event.getValue()));
            });
            submenu.addItem(toggle);
        }
    }

    private static <T> boolean isLastVisible(List<Entry<T>> entries, Entry<T> candidate) {
        return entries.stream().filter(entry -> entry.column().isVisible()).count() <= 1
                && candidate.column().isVisible();
    }
}
