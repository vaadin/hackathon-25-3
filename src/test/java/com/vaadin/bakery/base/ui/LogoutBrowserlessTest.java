package com.vaadin.bakery.base.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.base.security.CurrentUser;
import com.vaadin.bakery.ordering.CartSignals;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.spring.security.AuthenticationContext;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Log out has to log somebody out.
 *
 * It did not. The menu item sent the browser to {@code /logout} with a
 * navigation, and Spring Security maps that path as a POST: the request came
 * back 403, the session stayed open, and the only visible sign was that nothing
 * happened. Nothing in the suite covered it, which is how a dead control sits in
 * the corner of every screen for a while.
 *
 * The shell is built here rather than navigated to, because it builds its user
 * menu once, when the UI comes up, and in a browserless test that is before any
 * sign in this class can arrange. In the application the same thing happens the
 * other way round: logging in reloads the page, so the shell is always built
 * knowing who is looking at it.
 *
 * What is asserted is the wiring, that the item reaches
 * {@link AuthenticationContext#logout()}, which clears the context and
 * invalidates the session. Where the browser lands afterwards is Spring
 * Security's business and is configured, not written, here.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class LogoutBrowserlessTest extends SpringBrowserlessTest {

    @MockitoBean
    private AuthenticationContext authentication;

    @Autowired
    private ApplicationContext context;

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    /** The shell a signed in barista gets, attached so its labels translate. */
    private MainLayout shell() {
        navigate(HomeView.class);
        var layout = new MainLayout(context.getBean(CurrentUser.class), context.getBean(CartSignals.class),
                context.getBean(AppearanceSettings.class), authentication);
        UI.getCurrent().add(layout);
        return layout;
    }

    private static Stream<Component> descendants(Component component) {
        return Stream.concat(Stream.of(component), component.getChildren().flatMap(LogoutBrowserlessTest::descendants));
    }

    private static List<MenuItem> menuItems(MainLayout shell) {
        return descendants(shell)
                .filter(MenuBar.class::isInstance)
                .map(MenuBar.class::cast)
                .flatMap(menu -> menu.getItems().stream())
                .flatMap(item -> Stream.concat(Stream.of(item), item.getSubMenu().getItems().stream()))
                .toList();
    }

    private static MenuItem logoutItem(MainLayout shell) {
        return menuItems(shell).stream()
                .filter(item -> "Log out".equals(item.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no Log out in the shell, only "
                        + menuItems(shell).stream().map(MenuItem::getText).toList()));
    }

    @Test
    void theUserMenuOffersLoggingOut() {
        var shell = shell();

        assertEquals(1, menuItems(shell).stream().filter(item -> "Log out".equals(item.getText())).count(),
                "one way out, and it is named");
    }

    @Test
    void choosingItActuallyLogsOut() {
        var logout = logoutItem(shell());

        ComponentUtil.fireEvent(logout, new ClickEvent<>(logout));

        // The old code navigated to "/logout" instead, so this call never
        // happened and the session survived the click.
        verify(authentication).logout();
    }
}
