package com.example;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A public landing page and one page behind the login.
 *
 * The landing page is what makes the bug hard to see: after a scripted login
 * that carried no token, Spring's rejection redirects here, and here is a page
 * that looks perfectly normal because it is open to everybody. It prints who is
 * signed in, which is the only way to tell.
 */
public final class Views {

    private Views() {
    }

    @Route("")
    @AnonymousAllowed
    public static class LandingView extends VerticalLayout {
        public LandingView() {
            var who = SecurityContextHolder.getContext().getAuthentication();
            var name = who == null ? "nobody" : who.getName();

            add(new H2("The landing page"));
            add(new Paragraph("Signed in as: " + name));
            add(new Anchor("secret", "The page behind the login"));
            add(new Paragraph("Paste this into the console on /login, with the login form open:"));

            var script = new Pre("""
                    const f = document.querySelector('form');
                    f.querySelector('input[name=username]').value = 'user';
                    f.querySelector('input[name=password]').value = 'password';
                    f.submit();
                    """);
            add(script);
            add(new Paragraph("""
                    It lands back here, on a page that looks like a successful login, \
                    and this line still says nobody. The hidden input the form carries \
                    has no name and no value: the token is in the _csrf meta tags and \
                    the component copies it in from its own submit handler, which a \
                    scripted submit never runs.
                    """));
            add(new Paragraph("""
                    Fill it in first and the same script signs in:
                    """));
            add(new Pre("""
                    const f = document.querySelector('form');
                    const hidden = f.querySelector('input[type=hidden]');
                    hidden.name = document.querySelector('meta[name=_csrf_parameter]').content;
                    hidden.value = document.querySelector('meta[name=_csrf]').content;
                    f.querySelector('input[name=username]').value = 'user';
                    f.querySelector('input[name=password]').value = 'password';
                    f.submit();
                    """));
        }
    }

    @Route("secret")
    @PermitAll
    public static class SecretView extends VerticalLayout {
        public SecretView() {
            add(new H2("Behind the login"));
            add(new Paragraph("If you can read this, the login worked."));
            add(new Anchor("", "Back"));
        }
    }
}
