package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.catalogue.Allergen;
import com.vaadin.bakery.catalogue.AllergenRepository;
import com.vaadin.bakery.catalogue.ProductRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** SHOP-01, SHOP-02, SHOP-03, SHOP-08 and SHOP-09. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class CatalogueBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private AllergenRepository allergens;

    @Autowired
    private ProductRepository products;

    private List<String> visibleProductNames() {
        // The titles, not every link to a product: a card has two of those, the
        // title and the photograph, and filtering by href alone counted each
        // card twice and returned a blank name for every photo.
        return find(Anchor.class).withClassName("product-card__title").all().stream()
                .map(Anchor::getText)
                .toList();
    }

    @Test
    void theWholeCatalogueIsVisibleByDefault() {
        navigate(StorefrontView.class);
        assertEquals(products.findByAvailableTrueOrderBySortOrderAsc().size(), visibleProductNames().size());
    }

    @Test
    void searchingNarrowsTheGridAsYouType() {
        navigate(StorefrontView.class);
        var search = find(TextField.class).single();

        test(search).setValue("crois");

        var names = visibleProductNames();
        assertFalse(names.isEmpty(), "something should match");
        assertTrue(names.stream().allMatch(name -> name.toLowerCase().contains("crois")), names.toString());
    }

    @Test
    void excludingAnAllergenRemovesEveryProductThatCarriesIt() {
        navigate(StorefrontView.class);
        @SuppressWarnings("unchecked")
        var picker = (MultiSelectComboBox<Allergen>) find(MultiSelectComboBox.class).single();
        var nuts = allergens.findByCode("NUTS").orElseThrow();

        picker.setValue(Set.of(nuts));

        var withNuts = products.findByAvailableTrueOrderBySortOrderAsc().stream()
                .filter(product -> product.getAllergens().contains(nuts))
                .map(product -> product.getName())
                .toList();
        assertFalse(withNuts.isEmpty(), "the dataset should have products with nuts");
        assertTrue(visibleProductNames().stream().noneMatch(withNuts::contains),
                "no product carrying nuts should remain");
    }

    @Test
    void oneUserActionCausesOneRefilter() {
        var view = navigate(StorefrontView.class);
        @SuppressWarnings("unchecked")
        var picker = (MultiSelectComboBox<Allergen>) find(MultiSelectComboBox.class).single();
        int before = view.refreshCount;

        // Two allergens, one action. In 25.3 the value arrives on change rather
        // than per selected item, so the catalogue refilters once.
        picker.setValue(Set.of(allergens.findByCode("NUTS").orElseThrow(),
                allergens.findByCode("SOY").orElseThrow()));

        assertEquals(before + 1, view.refreshCount, "one action, one refilter");
    }

    @Test
    void anUnavailableProductDisappears() {
        var product = products.findBySlug("baguette").orElseThrow();
        product.setAvailable(false);
        products.saveAndFlush(product);
        try {
            navigate(StorefrontView.class);
            assertFalse(visibleProductNames().contains("Baguette"));
        } finally {
            // Reload before restoring: the save above bumped the version, and a
            // stale instance would fail the optimistic lock.
            var restored = products.findBySlug("baguette").orElseThrow();
            restored.setAvailable(true);
            products.saveAndFlush(restored);
        }
    }

    @Test
    void filtersThatMatchNothingExplainThemselves() {
        navigate(StorefrontView.class);
        test(find(TextField.class).single()).setValue("zzzzz nothing zzzzz");

        assertTrue(visibleProductNames().isEmpty());
        assertTrue(find(com.vaadin.flow.component.html.Paragraph.class).all().stream()
                .anyMatch(paragraph -> paragraph.getText().contains("Nothing matches")),
                "the empty state should say why");
    }

    @Test
    void choosingACategoryKeepsOnlyThatCategory() {
        navigate(StorefrontView.class);
        @SuppressWarnings("unchecked")
        var category = (Select<String>) find(Select.class).first();

        category.setValue("Drinks");

        var drinks = products.findByAvailableTrueOrderBySortOrderAsc().stream()
                .filter(product -> product.getCategory().getName().equals("Drinks"))
                .map(product -> product.getName())
                .sorted()
                .toList();
        assertEquals(drinks, visibleProductNames().stream().sorted().toList());
    }
}
