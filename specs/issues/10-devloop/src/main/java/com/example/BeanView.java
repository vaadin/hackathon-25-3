package com.example;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * For the new bean experiment. Add a class next to this one:
 *
 * <pre>
 * &#64;org.springframework.stereotype.Component
 * public class Extra {
 *     public String greeting() {
 *         return "from a bean that did not exist";
 *     }
 * }
 * </pre>
 *
 * then take the constructor parameter below, press apply, and open /bean.
 */
@Route("bean")
@AnonymousAllowed
public class BeanView extends VerticalLayout {

    public BeanView(/* Extra extra */) {
        add(new H2("A view with no dependencies yet"));
    }
}
