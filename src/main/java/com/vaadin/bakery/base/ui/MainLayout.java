package com.vaadin.bakery.base.ui;

import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.base.security.CurrentUser;
import com.vaadin.bakery.base.ui.AppearanceSettings.Theme;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.theme.aura.Aura;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The shell. Views never name it: it is annotated {@link Layout} and picked up
 * automatically. Cross view state here is held in signals, never in fields read
 * from several places.
 */
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

    private static final Set<String> OPERATIONS = Set.of("orders", "kitchen");
    private static final Set<String> ADMINISTRATION = Set.of("admin");

    private final CurrentUser currentUser;
    private final AppearanceSettings appearance;
    private final AuthenticationContext authentication;
    private final Span viewTitle = new Span();
    private transient com.vaadin.flow.shared.Registration themeStylesheet;
    private transient com.vaadin.flow.shared.Registration paletteStylesheet;
    private transient String loadedStylesheet;

    public MainLayout(CurrentUser currentUser, AppearanceSettings appearance,
            AuthenticationContext authentication) {
        this.currentUser = currentUser;
        this.appearance = appearance;
        this.authentication = authentication;
        setPrimarySection(Section.DRAWER);
        addToNavbar(true, new DrawerToggle(), viewTitle, header());
        addToDrawer(brand(), navigation());

        // Attach scoped setup, the 25.3 way: no onAttach override, and whatever
        // the function returns is released when the layout detaches.
        whenAttached(ui -> {
            // Page.setColorScheme, not a theme attribute: Aura follows the CSS
            // color-scheme property, and so does every light-dark() value in our
            // own stylesheets. Setting theme="dark" on the body, which is what
            // the Lumo 24 examples do, changes nothing at all here.
            Signal.effect(this, () -> ui.getPage().setColorScheme(
                    Boolean.TRUE.equals(appearance.dark().get())
                            ? ColorScheme.Value.DARK
                            : ColorScheme.Value.LIGHT));

            // Exactly one theme stylesheet at a time, plus the bakery palette
            // when the chosen variant asks for it. Loading both themes at once
            // would leave one of them half applied, and a plain theme has to
            // look exactly like itself for the comparison to mean anything.
            Signal.effect(this, () -> {
                Theme wanted = appearance.theme().get();

                // Only touch what actually changes. Removing a stylesheet and
                // adding the same URL back in one round trip loses it: the two
                // operations reach the browser together and the add is treated
                // as a duplicate of a sheet that is still there. Switching
                // between Lumo and "Lumo with the bakery colours" left the page
                // with no theme at all until this was split in two.
                if (!wanted.stylesheet().equals(loadedStylesheet)) {
                    if (themeStylesheet != null) {
                        themeStylesheet.remove();
                    }
                    themeStylesheet = ui.getPage().addStyleSheet(wanted.stylesheet());
                    loadedStylesheet = wanted.stylesheet();
                }

                if (wanted.hasBakeryPalette() && paletteStylesheet == null) {
                    paletteStylesheet = ui.getPage().addStyleSheet(AppearanceSettings.BAKERY_PALETTE);
                } else if (!wanted.hasBakeryPalette() && paletteStylesheet != null) {
                    paletteStylesheet.remove();
                    paletteStylesheet = null;
                }
            });
            viewTitle.bindText(ui.routerStateSignal().map(state -> state.currentView()
                    .filter(view -> view instanceof Component)
                    .flatMap(view -> MenuConfiguration.getPageHeader((Component) view))
                    .orElse("")));
            return () -> {
            };
        });
    }

    private Component brand() {
        var brand = Translations.bindText(new Span(), "app.name");
        brand.addClassName("brand");
        return brand;
    }

    /** Read by the tests, and by anything that needs to know which scheme is on. */
    boolean isDark() {
        return appearance.isDark();
    }

    /**
     * Lumo or Aura, chosen at runtime. Both are supported on purpose: Lumo is
     * what the old Bakery looked like and what most people recognise, Aura is
     * the new one, and the fastest way to have an opinion about them is to
     * switch between them on the same screen.
     */
    private Component themeSelector() {
        var menu = new MenuBar();
        menu.addThemeVariants(com.vaadin.flow.component.menubar.MenuBarVariant.LUMO_TERTIARY_INLINE);
        var item = menu.addItem(new Icon(VaadinIcon.PALETTE));
        Translations.bind(menu, item::setAriaLabel, "app.theme");
        var submenu = item.getSubMenu();
        var entries = new java.util.EnumMap<Theme, com.vaadin.flow.component.contextmenu.MenuItem>(Theme.class);
        for (Theme theme : Theme.values()) {
            var entry = submenu.addItem("", event -> {
                appearance.theme().set(theme);
                // One theme at a time: checking one clears the rest. A checkable
                // menu item does not do this for you, so a plain loop is the
                // difference between a radio group and a set of checkboxes.
                entries.forEach((candidate, menuItem) -> menuItem.setChecked(candidate == theme));
            });
            Translations.bind(menu, entry::setText, theme.translationKey());
            entry.setCheckable(true);
            entry.setChecked(appearance.currentTheme() == theme);
            entries.put(theme, entry);
        }
        return menu;
    }

    private Component header() {
        var themeToggle = new Button(new Icon(VaadinIcon.MOON));
        themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        Translations.bind(themeToggle, themeToggle::setAriaLabel, "app.theme.toggle");
        themeToggle.addClickListener(event -> appearance.toggleDark());

        var header = new HorizontalLayout(themeSelector(), languageSelector(), themeToggle,
                userMenu());
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName("app-header");
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        header.setWidthFull();
        return header;
    }


    private Component languageSelector() {
        var menu = new MenuBar();
        menu.addThemeVariants(com.vaadin.flow.component.menubar.MenuBarVariant.LUMO_TERTIARY_INLINE);
        var item = menu.addItem(new Icon(VaadinIcon.GLOBE));
        Translations.bind(menu, item::setAriaLabel, "app.language");
        var submenu = item.getSubMenu();
        submenu.addItem("English", event -> setLocale(Locale.ENGLISH));
        submenu.addItem("Espanol", event -> setLocale(Locale.of("es")));
        return menu;
    }

    private void setLocale(Locale locale) {
        getUI().ifPresent(ui -> ui.setLocale(locale));
    }

    private Component userMenu() {
        var menu = new MenuBar();
        menu.addThemeVariants(com.vaadin.flow.component.menubar.MenuBarVariant.LUMO_TERTIARY_INLINE);
        var name = currentUser.get().map(user -> user.getFirstName()).orElse(null);
        if (name == null) {
            var login = menu.addItem("");
            Translations.bind(menu, login::setText, "app.login");
            login.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate("login")));
            return menu;
        }
        var item = menu.addItem(name);
        // Not a navigation to "/logout": Spring Security maps that as a POST, so
        // sending the browser there with a GET answered 403 and left the session
        // open, which is a Log out that logs nobody out. This clears the
        // context, invalidates the session and redirects, which is the whole
        // job.
        var logout = item.getSubMenu().addItem("", event -> authentication.logout());
        Translations.bind(menu, logout::setText, "app.logout");
        return menu;
    }

    private Component navigation() {
        var shop = new SideNav();
        Translations.bind(shop, shop::setLabel, "nav.group.shop");
        var operations = new SideNav();
        Translations.bind(operations, operations::setLabel, "nav.group.operations");
        var administration = new SideNav();
        Translations.bind(administration, administration::setLabel, "nav.group.administration");

        List<MenuEntry> entries = MenuConfiguration.getMenuEntries();
        for (MenuEntry entry : entries) {
            var target = groupFor(entry.path(), shop, operations, administration);
            var item = new SideNavItem(entry.title(), entry.path());
            Translations.bind(item, item::setLabel, navigationKey(entry.path()));
            if (entry.icon() != null) {
                item.setPrefixComponent(new Icon(entry.icon()));
            }
            target.addItem(item);
        }

        var container = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        for (SideNav nav : List.of(shop, operations, administration)) {
            if (!nav.getItems().isEmpty()) {
                container.add(nav);
            }
        }
        return container;
    }

    /**
     * The route decides the key, so a navigation entry is translated like every
     * other visible string instead of carrying the English written in its
     * annotation.
     */
    private static String navigationKey(String path) {
        var head = path.startsWith("/") ? path.substring(1) : path;
        return "nav.item." + head.replace('/', '.');
    }

    private SideNav groupFor(String path, SideNav shop, SideNav operations, SideNav administration) {
        var head = path.startsWith("/") ? path.substring(1) : path;
        var first = head.contains("/") ? head.substring(0, head.indexOf('/')) : head;
        if (ADMINISTRATION.contains(first)) {
            return administration;
        }
        if (OPERATIONS.contains(first)) {
            return operations;
        }
        return shop;
    }

}
