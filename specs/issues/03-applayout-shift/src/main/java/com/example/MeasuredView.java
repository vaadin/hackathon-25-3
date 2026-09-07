package com.example;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * A view inside an AppLayout that measures itself.
 *
 * Open http://localhost:8093 with a **cold load**, on a window wide enough for
 * an inline drawer, and read the table it prints. It samples its own width
 * every twenty milliseconds and reports only the moments where something
 * changed.
 *
 * What it shows: the content is painted at the full window width while the
 * drawer is already occupying its space, and reflows by the drawer's width
 * twenty to fifty milliseconds later. `drawer-opened` and `overlay` carry their
 * final values in both samples, so nothing observable says the layout has
 * settled.
 *
 * An in application navigation to the same route shows no shift, so use the
 * browser's reload rather than the navigation link.
 */
@Route("")
@AnonymousAllowed
public class MeasuredView extends VerticalLayout {

    public MeasuredView() {
        setSizeFull();
        addClassName("measured");

        var output = new Pre();
        output.setId("output");
        add(new H3("What this view's own width does after a cold load"),
                new Paragraph("Reload the page. Each row is a change, not a sample."),
                output, filler());

        getElement().executeJs("""
                var out = document.getElementById('output');
                var rows = [];
                var previous = null;
                var t0 = performance.now();
                var samples = 0;
                var timer = setInterval(function () {
                  var layout = document.querySelector('vaadin-app-layout');
                  var view = document.querySelector('.measured');
                  var drawer = layout ? layout.shadowRoot.querySelector('[part="drawer"]') : null;
                  var line = JSON.stringify({
                    drawerOpened: layout ? layout.hasAttribute('drawer-opened') : null,
                    overlay: layout ? layout.hasAttribute('overlay') : null,
                    drawerWidth: drawer ? Math.round(drawer.getBoundingClientRect().width) : null,
                    contentWidth: view ? Math.round(view.getBoundingClientRect().width) : null
                  });
                  if (line !== previous) {
                    previous = line;
                    rows.push(Math.round(performance.now() - t0) + ' ms  ' + line);
                    out.textContent = rows.join('\\n');
                  }
                  if (++samples > 100) { clearInterval(timer); }
                }, 20);
                """);
    }

    /** Something wide enough that a 256 pixel shift is obvious to the eye. */
    private Div filler() {
        var filler = new Div();
        filler.getStyle().set("width", "100%").set("height", "40vh")
                .set("background", "repeating-linear-gradient(90deg, #ddd 0 40px, #bbb 40px 80px)");
        return filler;
    }
}
