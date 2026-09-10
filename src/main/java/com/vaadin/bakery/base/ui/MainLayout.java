package com.vaadin.bakery.base.ui;

import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.base.security.CurrentUser;
import com.vaadin.bakery.base.ui.AppearanceSettings.Theme;
import com.vaadin.bakery.ordering.CartSignals;
import com.vaadin.bakery.people.Role;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.theme.aura.Aura;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
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
    /** The route the Shop group is, rather than one of the things inside it. */
    private static final String STOREFRONT = "shop";

    private final CurrentUser currentUser;
    private final CartSignals cart;
    private final AppearanceSettings appearance;
    private final AuthenticationContext authentication;
    private final Span viewTitle = new Span();


    public MainLayout(CurrentUser currentUser, CartSignals cart, AppearanceSettings appearance,
            AuthenticationContext authentication) {
        this.currentUser = currentUser;
        this.cart = cart;
        this.appearance = appearance;
        this.authentication = authentication;
        setPrimarySection(Section.DRAWER);
        viewTitle.addClassName("view-title");
        addToNavbar(true, new DrawerToggle(), viewTitle, header());
        addToDrawer(brand(), navigation());

        // Whatever this person chose last time they were here. The session bean
        // was built before they signed in, so it cannot have asked itself.
        appearance.loadForCurrentUser();

        // Attach scoped setup, the 25.3 way: no onAttach override, and whatever
        // the function returns is released when the layout detaches.
        // The theme and the colour scheme, applied the same way on every route
        // rather than only on the ones inside this shell.
        ThemeBinding.apply(this, appearance);

        // The title in the navbar answers to two things, not one. Binding it to
        // the router state alone leaves it in the language it was first drawn
        // in: the signal does not change when somebody switches language, so
        // the mapped text never runs again and the header stays English over a
        // Spanish page. Reading the locale signal inside the same effect is
        // what makes it depend on both.
        whenAttached(ui -> Signal.effect(this, () -> {
            ui.localeSignal().get();
            var title = titleFor(ui);
            viewTitle.setText(title);
            // And the browser tab. The router sets it once from `@PageTitle`,
            // which is a literal, so nothing moves it when the language
            // changes: the tab keeps saying "Opening hours" over a page that
            // now says "Horario". Setting it here is what makes the two agree.
            if (!title.isBlank()) {
                ui.getPage().setTitle(title);
            }
        }));
    }

    /**
     * The route's own name, in the reader's language.
     *
     * {@code MenuConfiguration.getPageHeader} answers from `@Menu` and
     * `@PageTitle`, which are English literals: it cannot be translated, and a
     * navbar bound to it stays English over a Spanish page however the binding
     * is written. So the shell resolves the same bundle key the side navigation
     * uses for that route, and falls back to the header when a route has no key
     * of its own, such as a panel opened over the board.
     */
    private String titleFor(UI ui) {
        var location = ui.getInternals().getActiveViewLocation();
        var key = location == null ? null : navigationKey(location.getPath());
        if (key != null) {
            var translated = getTranslation(key);
            if (!translated.isBlank() && !translated.contains(key)) {
                return translated;
            }
        }
        return ui.routerStateSignal().get().currentView()
                .filter(view -> view instanceof Component)
                .flatMap(view -> MenuConfiguration.getPageHeader((Component) view))
                .orElse("");
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
                appearance.chooseTheme(theme);
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

        var header = new HorizontalLayout(cartBadge(), themeSelector(), languageSelector(), themeToggle,
                userMenu());
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName("app-header");
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        header.setWidthFull();
        return header;
    }

    /** The badge reads the same signal the cart page writes, so they agree by construction. */
    private Component cartBadge() {
        var count = new Span();
        count.getElement().getThemeList().add("badge primary small");
        count.bindText(cart.itemCount().map(String::valueOf));
        count.bindVisible(com.vaadin.flow.signals.Signal.computed(() -> cart.itemCount().get() > 0));

        var link = new com.vaadin.flow.component.html.Anchor("cart", "");
        link.addClassName("cart-badge");
        Translations.bind(link, link::setAriaLabel, "cart.title");
        link.add(new Icon(VaadinIcon.CART), count);
        return link;
    }

    private Component languageSelector() {
        var menu = new MenuBar();
        menu.addThemeVariants(com.vaadin.flow.component.menubar.MenuBarVariant.LUMO_TERTIARY_INLINE);
        var item = menu.addItem(new Icon(VaadinIcon.GLOBE));
        Translations.bind(menu, item::setAriaLabel, "app.language");
        var submenu = item.getSubMenu();
        // Checkable, like the theme menu next to it: a menu that offers four
        // choices and says nothing about which one is in force makes the reader
        // work out the answer from the page behind it.
        var entries = new java.util.LinkedHashMap<Locale, com.vaadin.flow.component.contextmenu.MenuItem>();
        entries.put(Locale.ENGLISH, submenu.addItem("English", event -> setLocale(Locale.ENGLISH)));
        entries.put(Locale.of("es"), submenu.addItem("Espanol", event -> setLocale(Locale.of("es"))));
        entries.values().forEach(entry -> entry.setCheckable(true));
        // One tick at a time, and it follows the locale rather than the click:
        // the language can also change from somewhere else, and a menu that
        // ticked itself on click would then be lying.
        Translations.onLocale(menu, locale -> entries.forEach((candidate, entry) ->
                entry.setChecked(candidate.getLanguage().equals(locale.getLanguage()))));
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
        // The person, not just their name. Avatar draws initials when there is
        // no picture, so a user without one is never a broken image.
        var avatar = new Avatar(currentUser.get().map(user -> user.getFullName()).orElse(name));
        currentUser.get()
                .map(user -> user.getAvatarPath())
                .filter(path -> path != null && !path.isBlank())
                .ifPresent(avatar::setImage);
        avatar.setThemeName("xsmall");

        var who = new com.vaadin.flow.component.html.Span(avatar,
                new com.vaadin.flow.component.html.Span(name));
        who.addClassName("app-header__user");
        var item = menu.addItem(who);
        // Not a navigation to "/logout": Spring Security maps that as a POST, so
        // sending the browser there with a GET answered 403 and left the session
        // open, which is a Log out that logs nobody out. This clears the
        // context, invalidates the session and redirects, which is the whole
        // job.
        var logout = item.getSubMenu().addItem("", event -> authentication.logout());
        Translations.bind(menu, logout::setText, "app.logout");
        return menu;
    }

    /**
     * One navigation with three parents, each holding the entries that belong to
     * it. The groups used to be three separate {@code SideNav}s with a label
     * apiece, which reads as three headings rather than as a structure that can
     * be collapsed.
     *
     * The Shop parent carries the storefront's own path: the group and the
     * storefront are the same thing, and a child repeating its parent's name is
     * the only thing that arrangement adds. The other two parents are labels,
     * because neither operations nor administration has a landing page.
     */
    private Component navigation() {
        var nav = new SideNav();

        var shop = new SideNavItem("");
        Translations.bind(shop, shop::setLabel, "nav.group.shop");
        var operations = new SideNavItem("");
        Translations.bind(operations, operations::setLabel, "nav.group.operations");
        // Every parent carries an icon, because the Shop parent takes its own
        // from the storefront entry and a group without one starts its label in
        // the icon column, leaving the three parents out of line with each other.
        operations.setPrefixComponent(new Icon(VaadinIcon.TASKS));
        var administration = new SideNavItem("");
        Translations.bind(administration, administration::setLabel, "nav.group.administration");
        administration.setPrefixComponent(new Icon(VaadinIcon.COG));

        List<MenuEntry> entries = MenuConfiguration.getMenuEntries();
        for (MenuEntry entry : entries) {
            var target = groupFor(entry.path(), shop, operations, administration);
            if (target == shop && STOREFRONT.equals(head(entry.path()))) {
                shop.setPath(entry.path());
                // A product page is a route under the storefront, so the parent
                // stays current while one is open.
                shop.setMatchNested(true);
                if (entry.icon() != null) {
                    shop.setPrefixComponent(new Icon(entry.icon()));
                }
                continue;
            }
            var item = new SideNavItem(entry.title(), entry.path());
            Translations.bind(item, item::setLabel, navigationKey(entry.path()));
            if (entry.icon() != null) {
                item.setPrefixComponent(new Icon(entry.icon()));
            }
            target.addItem(item);
        }

        for (SideNavItem group : List.of(shop, operations, administration)) {
            // A group nobody has access to is not an empty group, it is absent.
            if (group.getItems().isEmpty() && group.getPath() == null) {
                continue;
            }
            nav.addItem(group);
        }

        // One group opens on a cold load: the one this person works in. A baker
        // and a barista live in Operations, an administrator in Administration,
        // and somebody who has not signed in gets a closed drawer, because
        // every group they can see is one item deep anyway. Whatever route they
        // are on opens its own group as well, which the side nav does by itself
        // without being told.
        currentUser.get()
                .map(user -> user.getRole() == Role.ADMIN ? administration : operations)
                .filter(group -> group.getParent().isPresent())
                .ifPresent(group -> group.setExpanded(true));
        return nav;
    }

    /**
     * The route decides the key, so a navigation entry is translated like every
     * other visible string instead of carrying the English written in its
     * annotation.
     */
    private static String navigationKey(String path) {
        var route = path.startsWith("/") ? path.substring(1) : path;
        return "nav.item." + route.replace('/', '.');
    }

    /** The first segment of a route, which is what decides its group. */
    private static String head(String path) {
        var route = path.startsWith("/") ? path.substring(1) : path;
        return route.contains("/") ? route.substring(0, route.indexOf('/')) : route;
    }

    private SideNavItem groupFor(String path, SideNavItem shop, SideNavItem operations,
            SideNavItem administration) {
        var first = head(path);
        if (ADMINISTRATION.contains(first)) {
            return administration;
        }
        if (OPERATIONS.contains(first)) {
            return operations;
        }
        return shop;
    }

}
