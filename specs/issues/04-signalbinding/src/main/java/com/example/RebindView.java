package com.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * A dialog whose button text is bound to a signal on attach.
 *
 * Open http://localhost:8094, press "Open the dialog", close it, press it
 * again. The second open throws:
 *
 *   com.vaadin.flow.signals.BindingActiveException: Operation could not be
 *   performed because a binding is active.
 *       at com.vaadin.flow.component.SignalPropertySupport.bind(...)
 *       at com.vaadin.flow.component.button.Button.bindText(...)
 *
 * Binding on attach is the natural place, because that is where the UI and its
 * locale signal are available. A dialog attaches every time it opens, and
 * `HasText.bindText` returns a `SignalBinding` with nothing on it that releases
 * the binding, so there is no way to write this correctly.
 */
@Route("")
public class RebindView extends VerticalLayout {

    public RebindView() {
        var dialog = new Dialog();
        dialog.add(new Paragraph("Close me and open me again."));
        dialog.add(bindText(new Button(), "Inside the dialog"));

        add(new Button("Open the dialog", event -> dialog.open()), dialog);
        add(new Paragraph("Open it, close it, open it again. The second open throws."));
    }

    /** Exactly what an application does to translate a caption. */
    private static <C extends Component & HasText> C bindText(C component, String text) {
        component.whenAttached(ui -> {
            component.bindText(ui.localeSignal().map(locale -> text));
            return () -> {
            };
        });
        return component;
    }
}
