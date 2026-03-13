package com.vaadin.example.sightseeing.views.tags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.vaadin.example.sightseeing.data.entity.Tag;
import com.vaadin.example.sightseeing.data.service.TagService;
import com.vaadin.example.sightseeing.ui.AdminNav;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.shared.Tooltip.TooltipPosition;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.RolesAllowed;

@PageTitle("Tags")
@Route(value = "tags/:tagID?/:action?(edit)")
@RolesAllowed("ADMIN")
@StyleSheet("tags-view.css")
@Uses(Icon.class)
public class TagsView extends Div implements BeforeEnterObserver {

    private final String TAG_ID = "tagID";
    private final String TAG_EDIT_ROUTE_TEMPLATE = "tags/%s/edit";

    private Grid<Tag> grid = new Grid<>(Tag.class, false);

    private TextField placeName;
    private TextField name;
    private TextField val;
    private Checkbox enabled;

    private Div editorDiv;
    private FormLayout editorSingleSelectionContent;
    private Div editorMultiSelectionContent;

    RadioButtonGroup<MultiSelectionOption> multiSelectionOptions = new RadioButtonGroup<MultiSelectionOption>(
            null, MultiSelectionOption.ENABLE_ALL,
            MultiSelectionOption.DISABLE_ALL);

    private Button cancel = new Button("Cancel");
    private Button save = new Button("Save");

    private BeanValidationBinder<Tag> binder;

    private Tag tag;
    private final List<Tag> tags = new ArrayList<Tag>();

    private final TagService tagService;

    private enum MultiSelectionOption {
        ENABLE_ALL("Endable all"), DISABLE_ALL("Disable all");

        private String caption;

        MultiSelectionOption(String caption) {
            this.caption = caption;
        }

        public String getCaption() {
            return caption;
        }
    }

    @Autowired
    public TagsView(TagService tagService) {
        this.tagService = tagService;
        addClassNames("tags-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addColumn(i -> i.getPlace().getName()).setHeader("place")
                .setAutoWidth(true);
        grid.addColumn("name").setAutoWidth(true);
        grid.addColumn("val").setAutoWidth(true);
        LitRenderer<Tag> enabledRenderer = LitRenderer.<Tag> of(
                "<vaadin-icon icon='vaadin:${item.icon}' style='width: var(--lumo-icon-size-s); height: var(--lumo-icon-size-s); color: ${item.color};'></vaadin-icon>")
                .withProperty("icon",
                        enabled -> enabled.isEnabled() ? "check" : "minus")
                .withProperty("color",
                        enabled -> enabled.isEnabled()
                                ? "var(--lumo-primary-text-color)"
                                : "var(--lumo-disabled-text-color)");

        grid.addColumn(enabledRenderer).setHeader("Enabled").setAutoWidth(true);

        grid.setItems(
                query -> tagService
                        .list(PageRequest
                                .of(query.getPage(), query.getPageSize(),
                                        VaadinSpringDataHelpers
                                                .toSpringDataSort(query)))
                        .stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when selection changes, populate form depending on the amount of
        // selected rows
        grid.setSelectionMode(SelectionMode.MULTI);
        grid.asMultiSelect().addValueChangeListener(event -> {
            if (event.getValue() != null && event.getValue().size() > 0) {
                if (event.getValue().size() == 1) {
                    UI.getCurrent().navigate(String.format(TAG_EDIT_ROUTE_TEMPLATE,
                            event.getValue().iterator().next().getId()));
                } else {
                    StringBuilder sb = new StringBuilder();
                    event.getValue().stream().forEach(tag -> {
                        sb.append(";");
                        sb.append(tag.getId());
                    });
                    UI.getCurrent().navigate(String.format(
                            TAG_EDIT_ROUTE_TEMPLATE,
                            "multi:" + sb.substring(1)));
                }
            } else {
                clearForm();
                UI.getCurrent().navigate(TagsView.class);
            }
        });
        ((GridMultiSelectionModel<Tag>) grid.getSelectionModel())
                .setDragSelect(true);

        grid.setTooltipGenerator(item -> {
            return "X: " + item.getPlace().getX() + ", Y: "
                    + item.getPlace().getY();
        });
        grid.setTooltipPosition(TooltipPosition.BOTTOM);

        // Configure Form
        binder = new BeanValidationBinder<>(Tag.class);

        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (tags.isEmpty()) {
                    if (tag == null) {
                        tag = new Tag();
                    }
                    binder.writeBean(tag);
                    tagService.update(tag);
                } else {
                    switch (multiSelectionOptions.getValue()) {
                    case ENABLE_ALL:
                        tags.stream().forEach(tag -> {
                            tag.setEnabled(true);
                        });
                        break;
                    case DISABLE_ALL:
                        tags.stream().forEach(tag -> {
                            tag.setEnabled(false);
                        });
                        break;
                    }
                    tagService.update(tags);
                    multiSelectionOptions.clear();
                }
                clearForm();
                refreshGrid();
                Notification.show("Tag details stored.");
                UI.getCurrent().navigate(TagsView.class);
            } catch (ValidationException validationException) {
                Notification.show(
                        "An exception happened while trying to store the tag details.");
            }
        });

    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<String> tagIdParameter = event.getRouteParameters().get(TAG_ID);
        if (tagIdParameter.isPresent()) {
            String tagIdString = tagIdParameter.get();
            if (tagIdString.startsWith("multi:")) {
                tagIdString = tagIdString.substring(6);
                List<Long> tagIds = Arrays.asList(tagIdString.split(";"))
                        .stream()
                        .map(id -> Long.valueOf(id))
                        .collect(Collectors.toList());
                List<Tag> found = new ArrayList<Tag>();
                List<Tag> missing = new ArrayList<Tag>();
                for (Long tagId : tagIds) {
                    Optional<Tag> tagFromBackend = tagService.get(tagId);
                    if (tagFromBackend.isPresent()) {
                        found.add(tagFromBackend.get());
                    } else {
                        missing.add(tagFromBackend.get());
                    }
                }
                if (missing.isEmpty()) {
                    populateMultiForm(found);
                } else {
                    Notification.show(String.format(
                            "One or more requested tag was not found, IDs = %s",
                            tagIdString.replace(";", ", ")), 3000,
                            Notification.Position.BOTTOM_START);
                    refreshGrid(found, missing);
                    event.forwardTo(TagsView.class);
                }
            } else {
                Long tagId = Long.valueOf(tagIdString);
                Optional<Tag> tagFromBackend = tagService.get(tagId);
                if (tagFromBackend.isPresent()) {
                    populateForm(tagFromBackend.get());
                } else {
                    Notification.show(
                            String.format(
                                    "The requested tag was not found, ID = %s",
                                    tagId),
                            3000, Notification.Position.BOTTOM_START);
                    // when a row is selected but the data is no longer
                    // available, refresh grid
                    refreshGrid();
                    event.forwardTo(TagsView.class);
                }
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        editorSingleSelectionContent = new FormLayout();
        placeName = new TextField("Place");
        name = new TextField("Name");
        val = new TextField("Val");
        enabled = new Checkbox("Enabled");
        Component[] fields = new Component[] { placeName, name, val, enabled };
        editorSingleSelectionContent.add(fields);

        editorMultiSelectionContent = new Div();
        Text multiSelectionText = new Text("Multiple tags selected.");
        editorMultiSelectionContent.add(multiSelectionText,
                multiSelectionOptions);

        updateEditorContents();
        createButtonLayout(editorLayoutDiv);
        editorLayoutDiv.add(new AdminNav("tags"));

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void updateEditorContents() {
        if (tags.isEmpty()) {
            editorDiv.add(editorSingleSelectionContent);
            editorDiv.remove(editorMultiSelectionContent);
        } else {
            editorDiv.remove(editorSingleSelectionContent);
            editorDiv.add(editorMultiSelectionContent);
        }
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
        grid.asMultiSelect().clear();
        grid.getLazyDataView().refreshAll();
    }

    private void refreshGrid(Collection<Tag> found, Collection<Tag> missing) {
        grid.asMultiSelect().deselect(missing);
        grid.asMultiSelect().select(found);
        grid.getLazyDataView().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(Tag value) {
        tags.clear();
        tag = value;
        binder.readBean(tag);
        updateEditorContents();
    }

    private void populateMultiForm(Collection<Tag> values) {
        tags.clear();
        tags.addAll(values);
        tag = null;
        binder.removeBean();
        updateEditorContents();
    }
}
