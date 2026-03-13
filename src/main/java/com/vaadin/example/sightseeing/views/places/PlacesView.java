package com.vaadin.example.sightseeing.views.places;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.vaadin.example.sightseeing.data.entity.Place;
import com.vaadin.example.sightseeing.data.service.PlaceService;
import com.vaadin.example.sightseeing.ui.AdminNav;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.shared.Tooltip.TooltipPosition;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.StringToDoubleConverter;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.RolesAllowed;

@PageTitle("Places")
@Route(value = "places/:placeID?/:action?(edit)")
@RolesAllowed("ADMIN")
@StyleSheet("places-view.css")
@Uses(Icon.class)
public class PlacesView extends Div implements BeforeEnterObserver {

    private final String PLACE_ID = "placeID";
    private final String PLACE_EDIT_ROUTE_TEMPLATE = "places/%s/edit";

    private Grid<Place> grid = new Grid<>(Place.class, false);

    private TextField name;
    private TextField x;
    private TextField y;
    private Checkbox enabled;

    private Button cancel = new Button("Cancel");
    private Button save = new Button("Save");

    private BeanValidationBinder<Place> binder;

    private Place place;

    private final PlaceService placeService;

    @Autowired
    public PlacesView(PlaceService placeService) {
        this.placeService = placeService;
        addClassNames("places-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addColumn("name").setAutoWidth(true);
        grid.addColumn("x").setAutoWidth(true);
        grid.addColumn("y").setAutoWidth(true);
        LitRenderer<Place> enabledRenderer = LitRenderer.<Place> of(
                "<vaadin-icon icon='vaadin:${item.icon}' style='width: var(--lumo-icon-size-s); height: var(--lumo-icon-size-s); color: ${item.color};'></vaadin-icon>")
                .withProperty("icon",
                        enabled -> enabled.isEnabled() ? "check" : "minus")
                .withProperty("color",
                        enabled -> enabled.isEnabled()
                                ? "var(--lumo-primary-text-color)"
                                : "var(--lumo-disabled-text-color)");

        grid.addColumn(enabledRenderer).setHeader("Enabled").setAutoWidth(true);

        grid.setItems(
                query -> placeService
                        .list(PageRequest
                                .of(query.getPage(), query.getPageSize(),
                                        VaadinSpringDataHelpers
                                                .toSpringDataSort(query)))
                        .stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(
                        PLACE_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(PlacesView.class);
            }
        });

        List<String> included = Arrays.asList("tourism", "historic", "amenity",
                "turism");
        grid.setTooltipGenerator(item -> {
            StringBuilder sb = new StringBuilder();
            item.getTags().stream()
                    .filter(tag -> included.contains(tag.getName()))
                    .forEach(tag -> {
                        sb.append(" - ");
                        sb.append(tag.getName() + ": " + tag.getVal());
            });
            if (sb.length() > 3) {
                return sb.toString().substring(3).replace("_", " ");
            }
            return "something went wrong, try again later";
        });
        grid.setTooltipPosition(TooltipPosition.END);

        // Configure Form
        binder = new BeanValidationBinder<>(Place.class);

        // Bind fields. This is where you'd define e.g. validation rules
        binder.forField(x)
                .withConverter(
                        new StringToDoubleConverter("Only numbers are allowed"))
                .bind("x");
        binder.forField(y)
                .withConverter(
                        new StringToDoubleConverter("Only numbers are allowed"))
                .bind("y");

        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (place == null) {
                    place = new Place();
                }
                binder.writeBean(place);
                placeService.update(place);
                clearForm();
                refreshGrid();
                Notification.show("Place details stored.");
                UI.getCurrent().navigate(PlacesView.class);
            } catch (ValidationException validationException) {
                Notification.show(
                        "An exception happened while trying to store the place details.");
            }
        });

    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> placeId = event.getRouteParameters().get(PLACE_ID)
                .map(Long::parseLong);
        if (placeId.isPresent()) {
            Optional<Place> placeFromBackend = placeService.get(placeId.get());
            if (placeFromBackend.isPresent()) {
                populateForm(placeFromBackend.get());
            } else {
                Notification.show(
                        String.format(
                                "The requested place was not found, ID = %s",
                                placeId.get()),
                        3000, Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(PlacesView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        name = new TextField("Name");
        x = new TextField("X");
        y = new TextField("Y");
        enabled = new Checkbox("Enabled");
        Component[] fields = new Component[] { name, x, y, enabled };

        formLayout.add(fields);
        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);
        editorLayoutDiv.add(new AdminNav("places"));

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getLazyDataView().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(Place value) {
        place = value;
        binder.readBean(place);

    }
}
