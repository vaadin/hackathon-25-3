package com.vaadin.bakery.catalogue.ui;

import com.vaadin.bakery.catalogue.Allergen;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.catalogue.Category;
import com.vaadin.bakery.catalogue.Product;
import com.vaadin.bakery.catalogue.ProductImages;
import com.vaadin.bakery.catalogue.VatRate;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.PartialMatchMode;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadDropZone;
import com.vaadin.flow.component.upload.UploadFileList;
import com.vaadin.flow.component.upload.UploadManager;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.signals.local.ValueSignal;
import java.util.List;

/**
 * The product editor.
 *
 * Three 25.3 pieces meet here: the category picker matches on any fragment
 * rather than a prefix, the description preview is the same Markdown component
 * the public page uses bound to the same signal, and the photo can arrive by
 * drop, by button or from the clipboard.
 */
public class ProductEditor extends Dialog {

    private final BeanValidationBinder<Product> binder = new BeanValidationBinder<>(Product.class);
    private final ValueSignal<String> description = new ValueSignal<>("");
    private final CatalogueService catalogue;
    private Product product;

    public ProductEditor(CatalogueService catalogue, List<Category> categories, List<Allergen> allergens,
            SerializableConsumer<Product> onSaved) {
        this.catalogue = catalogue;
        Translations.bind(this, this::setHeaderTitle, "admin.product.edit");
        setWidth("52rem");

        var name = new TextField();
        Translations.bind(name, name::setLabel, "admin.product.name");
        var price = new IntegerField();
        Translations.bind(price, price::setLabel, "admin.product.price");
        Translations.bind(price, price::setHelperText, "admin.product.price.helper");
        var stock = new IntegerField();
        Translations.bind(stock, stock::setLabel, "admin.product.stock");
        var leadTime = new IntegerField();
        Translations.bind(leadTime, leadTime::setLabel, "admin.product.leadTime");

        var category = new ComboBox<Category>();
        Translations.bind(category, category::setLabel, "admin.product.category");
        category.setItems(categories);
        category.setItemLabelGenerator(Category::getName);
        // Any fragment, not just the beginning: "cake" should find "Celebration cake".
        category.setPartialMatchMode(PartialMatchMode.FIRST_MATCH);

        var vat = new ComboBox<VatRate>();
        Translations.bind(vat, vat::setLabel, "admin.product.vat");
        vat.setItems(VatRate.values());
        Translations.onLocale(vat, locale ->
                vat.setItemLabelGenerator(rate -> getTranslation(locale, rate.translationKey())));

        var allergenPicker = new MultiSelectComboBox<Allergen>();
        Translations.bind(allergenPicker, allergenPicker::setLabel, "admin.product.allergens");
        allergenPicker.setItems(allergens);
        Translations.onLocale(allergenPicker, locale -> allergenPicker.setItemLabelGenerator(
                allergen -> getTranslation(locale, allergen.translationKey())));

        var available = new Switch();
        Translations.bind(available, available::setLabel, "admin.product.available");
        var featured = new Switch();
        Translations.bind(featured, featured::setLabel, "admin.product.featured");

        var markdown = new TextArea();
        Translations.bind(markdown, markdown::setLabel, "admin.product.description");
        markdown.setValueChangeMode(ValueChangeMode.LAZY);
        markdown.setHeight("12rem");
        var preview = new Markdown(description);
        preview.addClassName("product-editor__preview");
        markdown.addValueChangeListener(event -> description.set(event.getValue()));

        binder.bind(name, "name");
        binder.bind(price, "priceCents");
        binder.bind(stock, "stockToday");
        binder.bind(leadTime, "leadTimeDays");
        binder.bind(category, "category");
        binder.bind(vat, "vatRate");
        binder.bind(available, "available");
        binder.bind(featured, "featured");
        binder.bind(markdown, "descriptionMarkdown");
        binder.forField(allergenPicker).bind(
                item -> item.getAllergens(),
                (item, value) -> item.setAllergens(new java.util.LinkedHashSet<>(value)));

        var form = new FormLayout(name, category, price, vat, stock, leadTime, available, featured);
        var editorSide = new Div(form, markdown);
        editorSide.addClassName("product-editor__form");
        var previewSide = new Div(imageArea(), preview);
        previewSide.addClassName("product-editor__preview-side");

        var content = new Div(editorSide, previewSide);
        content.addClassName("product-editor__layout");
        add(content);

        var save = Translations.bindText(new Button("", event -> {
            if (binder.writeBeanIfValid(product)) {
                onSaved.accept(catalogue.save(product));
                close();
            }
        }), "admin.save");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(Translations.bindText(new Button("", event -> close()), "admin.cancel"), save);
    }

    /** Drop zone, button and file list, all driven by one upload manager. */
    private Div imageArea() {
        var image = new Image();
        image.addClassName("product-editor__image");

        var handler = UploadHandler.inMemory((metadata, data) ->
                store(metadata.fileName(), metadata.contentType(), data, image));

        var manager = new UploadManager(this, handler);
        manager.setMaxFiles(1);
        manager.setMaxFileSize(2L * 1024 * 1024);
        manager.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp");

        var button = Translations.bindText(new UploadButton("", manager), "admin.product.image.choose");
        var fileList = new UploadFileList(manager);
        var hint = Translations.bindText(new com.vaadin.flow.component.html.Span(),
                "admin.product.image.hint");
        var dropContent = new Div(hint, button, fileList);
        var dropZone = new UploadDropZone(dropContent, manager);
        dropZone.addClassName("product-editor__dropzone");

        // The same photo, pasted. Somebody with a phone photo in the clipboard
        // should not have to save it to disk first.
        com.vaadin.flow.component.clipboard.Clipboard.onFilePaste(dropZone, handler);

        var area = new Div(image, dropZone);
        area.addClassName("product-editor__image-area");
        return area;
    }

    private void store(String filename, String contentType, byte[] data, Image image) {
        try {
            catalogue.storeImage(product, filename, contentType, data);
            image.setSrc(ProductImages.url(product, true) + "?v=" + System.nanoTime());
        } catch (com.vaadin.bakery.base.error.DomainException failure) {
            Notification.show(getTranslation(failure.translationKey(), failure.arguments()));
        }
    }

    public void editProduct(Product toEdit) {
        this.product = toEdit;
        Translations.bind(this, this::setHeaderTitle, "admin.product.edit");
        binder.readBean(toEdit);
        description.set(toEdit.getDescriptionMarkdown() == null ? "" : toEdit.getDescriptionMarkdown());
        open();
    }

    /**
     * A blank product, in the same editor. The slug is left empty on purpose:
     * the catalogue derives it from the name when it saves, so nobody is asked
     * to invent a URL.
     */
    public void newProduct() {
        var blank = new Product();
        blank.setCategory(catalogue.categories().stream().findFirst().orElse(null));
        this.product = blank;
        Translations.bind(this, this::setHeaderTitle, "admin.product.new");
        binder.readBean(blank);
        description.set("");
        open();
    }
}
