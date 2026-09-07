package com.vaadin.bakery.catalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.error.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** DOM-08 plus the upload guard that keeps a renamed executable out of the database. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Transactional
class CatalogueServiceTest {

    @Autowired
    private CatalogueService catalogue;

    @Autowired
    private ProductRepository products;

    @Test
    void aProductWithOrdersCannotBeDeleted() {
        var product = products.findBySlug("butter-croissant").orElseThrow();

        var failure = assertThrows(DomainException.Conflict.class, () -> catalogue.delete(product));

        assertEquals("catalogue.product.inUse", failure.translationKey());
        assertTrue(products.findBySlug("butter-croissant").isPresent(), "and it is still there");
    }

    @Test
    void aCategoryWithProductsCannotBeDeleted() {
        var category = catalogue.categories().getFirst();
        var failure = assertThrows(DomainException.Conflict.class, () -> catalogue.delete(category));
        assertEquals("catalogue.category.inUse", failure.translationKey());
    }

    @Test
    void anImageIsCheckedOnItsBytesNotOnWhatItClaims() {
        byte[] png = new byte[] { (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13 };
        byte[] notAnImage = "MZ this is an executable".getBytes();

        assertTrue(CatalogueService.isSupportedImage("image/png", png));
        assertFalse(CatalogueService.isSupportedImage("image/png", notAnImage),
                "claiming to be a png is not enough");
        assertFalse(CatalogueService.isSupportedImage("application/x-msdownload", png));
    }

    @Test
    void anOversizedImageIsRefused() {
        var product = products.findAll().getFirst();
        byte[] big = new byte[3 * 1024 * 1024];
        big[0] = (byte) 0x89;
        big[1] = 'P';
        big[2] = 'N';
        big[3] = 'G';

        var failure = assertThrows(DomainException.RuleViolation.class,
                () -> catalogue.storeImage(product, "huge.png", "image/png", big));
        assertEquals("catalogue.image.tooLarge", failure.translationKey());
    }
}
