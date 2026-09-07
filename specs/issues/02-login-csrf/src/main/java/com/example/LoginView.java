package com.example;

import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Nothing unusual: an overlay, opened, posting to the login endpoint. */
@Route(value = "login", autoLayout = false)
@AnonymousAllowed
public class LoginView extends LoginOverlay {

    public LoginView() {
        setAction("login");
        setForgotPasswordButtonVisible(false);
        setOpened(true);
    }
}
