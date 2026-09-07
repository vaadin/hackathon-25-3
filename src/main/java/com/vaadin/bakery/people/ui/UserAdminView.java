package com.vaadin.bakery.people.ui;

import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.base.security.CurrentUser;
import com.vaadin.bakery.base.ui.Fields;
import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.people.User;
import com.vaadin.bakery.people.UserRepository;
import com.vaadin.bakery.people.UserService;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudI18n;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * Staff accounts.
 *
 * This is a list and an editor and nothing else, which is what {@code Crud} is
 * for: it brings the new item button, the editor panel, the delete
 * confirmation and the keyboard handling that were all written by hand here
 * before. The catalogue deliberately does not use it, because its grid is a
 * {@code GridPro} with inline editing, and that is one of the stories this
 * application exists to show. See {@code specs/features/07-admin.md}.
 *
 * Two rules matter more than the rest and neither is Crud's to keep: an empty
 * password field means "leave the password alone", and a locked account is
 * refused for everybody, including the administrator looking at it.
 */
@Route("admin/users")
@PageTitle("People")
@Menu(order = 21, title = "People", icon = "vaadin:users")
@RolesAllowed(Role.ADMIN_NAME)
public class UserAdminView extends VerticalLayout {

    private final UserService users;
    private final UserRepository repository;
    private final CurrentUser currentUser;
    private final Grid<User> grid = new Grid<>(User.class, false);
    private final Crud<User> crud;

    /** Empty on open, so opening a user and saving cannot wipe their password. */
    private final PasswordField password = new PasswordField();

    public UserAdminView(UserService users, UserRepository repository, CurrentUser currentUser) {
        this.users = users;
        this.repository = repository;
        this.currentUser = currentUser;
        addClassName("user-admin");
        setSizeFull();

        // A column is not in the component tree, so the grid owns the binding.
        var email = grid.addColumn(User::getEmail).setSortable(true).setFlexGrow(2);
        Translations.bind(grid, email::setHeader, "admin.user.email");
        var name = grid.addColumn(User::getFullName).setSortable(true).setFlexGrow(2);
        Translations.bind(grid, name::setHeader, "admin.user.name");
        var role = grid.addColumn(user -> getTranslation(user.getRole().translationKey())).setSortable(true);
        Translations.bind(grid, role::setHeader, "admin.user.role");
        var locked = grid.addComponentColumn(this::lockToggle);
        Translations.bind(grid, locked::setHeader, "admin.user.locked");

        // A Crud given its own grid does not add the edit column: with a grid
        // handed in, that is the application's to place.
        Crud.addEditColumn(grid);

        crud = new Crud<>(User.class, grid, editor());
        crud.setSizeFull();
        Translations.onLocale(this, locale -> crud.setI18n(texts(locale)));

        crud.addSaveListener(event -> save(event.getItem()));
        crud.addDeleteListener(event -> delete(event.getItem()));

        add(crud);
        // The role cell is translated by its value provider, and only a reload
        // makes the grid ask for it again.
        Translations.onLocale(this, locale -> refresh());
    }

    private BinderCrudEditor<User> editor() {
        var binder = new BeanValidationBinder<>(User.class);

        var email = Fields.email("admin.user.email");
        var firstName = new TextField();
        Translations.bind(firstName, firstName::setLabel, "checkout.firstName");
        var lastName = new TextField();
        Translations.bind(lastName, lastName::setLabel, "checkout.lastName");
        var role = new ComboBox<Role>();
        Translations.bind(role, role::setLabel, "admin.user.role");
        role.setItems(Role.values());
        Translations.onLocale(role, locale ->
                role.setItemLabelGenerator(value -> getTranslation(locale, value.translationKey())));

        Translations.bind(password, password::setLabel, "admin.user.password");
        Translations.bind(password, password::setHelperText, "admin.user.password.helper");

        binder.bind(email, "email");
        binder.bind(firstName, "firstName");
        binder.bind(lastName, "lastName");
        binder.bind(role, "role");

        return new BinderCrudEditor<>(binder, new FormLayout(email, firstName, lastName, role, password));
    }

    /** Crud's own buttons and its delete confirmation, in the reader's language. */
    private CrudI18n texts(java.util.Locale locale) {
        var i18n = CrudI18n.createDefault();
        i18n.setNewItem(getTranslation(locale, "admin.user.new"));
        i18n.setEditItem(getTranslation(locale, "admin.user.edit"));
        i18n.setSaveItem(getTranslation(locale, "admin.save"));
        i18n.setDeleteItem(getTranslation(locale, "admin.delete"));
        i18n.setCancel(getTranslation(locale, "admin.cancel"));
        i18n.setEditLabel(getTranslation(locale, "admin.edit"));
        i18n.getConfirm().getDelete().setTitle(getTranslation(locale, "admin.user.delete.title"));
        i18n.getConfirm().getDelete().setContent(getTranslation(locale, "admin.user.delete.body"));
        i18n.getConfirm().getDelete().getButton().setConfirm(getTranslation(locale, "admin.delete"));
        i18n.getConfirm().getDelete().getButton().setDismiss(getTranslation(locale, "admin.cancel"));
        return i18n;
    }

    private Switch lockToggle(User user) {
        var toggle = new Switch(user.isLocked());
        toggle.addValueChangeListener(event -> {
            users.setLocked(user, event.getValue());
            refresh();
        });
        return toggle;
    }

    private void save(User user) {
        try {
            users.save(user, password.getValue(), currentUser.get().orElse(null));
            password.clear();
            refresh();
        } catch (DomainException failure) {
            error(getTranslation(failure.translationKey(), failure.arguments()));
        }
    }

    private void delete(User user) {
        try {
            users.delete(user, currentUser.get().orElse(null));
            refresh();
        } catch (DomainException failure) {
            error(getTranslation(failure.translationKey(), failure.arguments()));
        }
    }

    private void error(String message) {
        var notification = Notification.show(message);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void refresh() {
        grid.setItems(repository.findAll().stream()
                .sorted((left, right) -> left.getEmail().compareToIgnoreCase(right.getEmail()))
                .toList());
    }

    Grid<User> grid() {
        return grid;
    }

    Crud<User> crud() {
        return crud;
    }
}
