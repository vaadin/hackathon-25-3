package com.example;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * A Markdown bound to a signal, and a button that reads it back.
 *
 * Open http://localhost:8095 and press "Read the content". It throws:
 *
 *   com.vaadin.flow.signals.BindingActiveException: Operation could not be
 *   performed because a binding is active.
 *       at com.vaadin.flow.component.SignalPropertySupport.bind(...)
 *
 * `getContent()` is a getter. Binding the property is the documented way to
 * keep a preview in step with an editor, and after doing it there is no way to
 * ask the component what it is showing.
 */
@Route("")
public class MarkdownView extends VerticalLayout {

    public MarkdownView() {
        var content = new ValueSignal<>("**Type below.**");
        var markdown = new Markdown(content);

        var editor = new TextField("Markdown");
        editor.setValue("**Type below.**");
        editor.addValueChangeListener(event -> content.set(event.getValue()));

        var result = new Paragraph();
        var read = new Button("Read the content", event -> {
            // Throws. Reading the signal instead works.
            result.setText("getContent() returned: " + markdown.getContent());
        });

        add(editor, markdown, read, result);
    }
}
