package com.vaadin.bakery.ordering.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

/**
 * A panel's memory of work nobody has saved, and the question it asks before
 * that work is thrown away.
 *
 * Both panel views own one. The line editor is the only thing in either of them
 * that can lose anything, and it already says when it changed, so following it
 * is all this needs to know.
 */
final class UnsavedChanges {

    private boolean pending;

    /** Every change the editor reports counts, a removed row included. */
    void follow(OrderLineEditor editor) {
        editor.addLinesChangeListener(event -> pending = true);
    }

    /** Nothing is at stake any more: the panel was saved, or freshly rendered. */
    void settled() {
        pending = false;
    }

    /** Leaves, or asks first when leaving would lose something. */
    void leave(Component owner, Runnable leave) {
        if (!pending) {
            leave.run();
            return;
        }
        // Composed the moment it is shown, so the locale in force then is
        // already the right one and there is nothing to keep bound.
        var dialog = new ConfirmDialog();
        dialog.setHeader(owner.getTranslation("board.panel.discard.title"));
        dialog.setText(owner.getTranslation("board.panel.discard.body"));
        dialog.setCancelable(true);
        dialog.setCancelText(owner.getTranslation("board.panel.discard.keep"));
        dialog.setConfirmText(owner.getTranslation("board.panel.discard.confirm"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            pending = false;
            leave.run();
        });
        dialog.open();
    }
}
