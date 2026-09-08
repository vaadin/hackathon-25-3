package com.vaadin.bakery.base.ui;

import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
     * The chooser lives in the table's own header, where a column chooser is
     * looked for, so it is an icon rather than a word: a button reading
     * "Columns" above the table was one more thing competing with the search
     * field for the top of the page. The word is still there, as the accessible
     * name and as the tooltip.
     *
     * A button with a context menu rather than a `MenuBar`. A menu bar of one
     * item decides for itself whether that item fits, and inside a column two
     * and a half rem wide it decides it does not: it hides the item and renders
     * its own overflow indicator, which is the four pixel dot that appeared
     * where the icon should have been. A button has no such opinion and stays
     * the size of its icon at any column width.
     */
    public static <T> Component of(String labelKey, List<Entry<T>> entries) {
        // Vertical dots rather than a cog: this opens a list of columns, it does
        // not configure anything, and a cog on a table header reads as settings
        // for the whole view. Coloured the way the grid documentation styles an
        // icon in a header, so it sits behind the column names rather than on
        // top of them.
        var icon = new Icon(VaadinIcon.ELLIPSIS_DOTS_V);
        icon.getStyle().set("color", "var(--vaadin-text-color-secondary)");
        var button = new Button(icon);
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        button.addClassName("column-chooser");
        Translations.bind(button, text -> {
            button.setAriaLabel(text);
            button.setTooltipText(text);
        }, labelKey);

        var menu = new ContextMenu(button);
        menu.setOpenOnClick(true);
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
            menu.addItem(toggle);
        }
        return button;
    }

    /**
     * The same list of columns, opened from the table's own header rather than
     * from a control of ours.
     *
     * This is what the grid offers instead of a header cell above the selection
     * column, which does not exist: {@code GridSelectionColumn} extends
     * {@code Component} and not {@code AbstractColumn}, so it has no header API
     * at all, and neither the default header row nor a prepended one has a cell
     * for it. A {@code GridContextMenu} reaches the whole header, the select all
     * cell included, and its dynamic content handler is given {@code null} when
     * the menu is opened there rather than on a row, which is what makes "the
     * header only" expressible.
     */
    public static <T> GridContextMenu<T> onHeaderOf(Grid<T> grid, List<Entry<T>> entries) {
        var menu = grid.addContextMenu();
        // Rows have their own actions; this menu is about the table's shape.
        menu.setDynamicContentHandler(item -> item == null);
        for (Entry<T> entry : entries) {
            menu.addItem(toggleFor(entries, entry));
        }
        return menu;
    }

    private static <T> Checkbox toggleFor(List<Entry<T>> entries, Entry<T> entry) {
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
        return toggle;
    }

    private static <T> boolean isLastVisible(List<Entry<T>> entries, Entry<T> candidate) {
        return entries.stream().filter(entry -> entry.column().isVisible()).count() <= 1
                && candidate.column().isVisible();
    }
}
