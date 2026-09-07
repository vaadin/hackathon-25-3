package com.vaadin.bakery.base.ui;

import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Locale;

@Route(value = "login", autoLayout = false)
@AnonymousAllowed
public class LoginView extends LoginOverlay implements BeforeEnterObserver {

    public LoginView() {
        setForgotPasswordButtonVisible(false);
        setAction("login");
        setOpened(true);

        // The overlay reads one i18n object rather than exposing setters per
        // string, so following the locale means handing it a new one.
        Translations.onLocale(this, locale -> setI18n(i18n(locale)));
    }

    private LoginI18n i18n(Locale locale) {
        var i18n = new LoginI18n();

        var header = new LoginI18n.Header();
        header.setTitle(getTranslation(locale, "app.name"));
        header.setDescription(getTranslation(locale, "login.demo.accounts"));
        i18n.setHeader(header);

        var form = new LoginI18n.Form();
        form.setTitle(getTranslation(locale, "login.title"));
        form.setUsername(getTranslation(locale, "login.email"));
        form.setPassword(getTranslation(locale, "login.password"));
        form.setSubmit(getTranslation(locale, "login.submit"));
        form.setForgotPassword("");
        i18n.setForm(form);

        var error = new LoginI18n.ErrorMessage();
        error.setTitle(getTranslation(locale, "login.error.title"));
        error.setMessage(getTranslation(locale, "login.error.message"));
        i18n.setErrorMessage(error);
        return i18n;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
    }
}
