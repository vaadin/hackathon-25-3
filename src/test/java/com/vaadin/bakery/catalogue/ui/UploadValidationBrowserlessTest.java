package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.catalogue.ProductImageRepository;
import com.vaadin.bakery.catalogue.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * ADM-06. The upload components enforce their limits in the browser, and the
 * service enforces them again, because a browser is not a trust boundary.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UploadValidationBrowserlessTest {

    @Autowired
    private CatalogueService catalogue;

    @Autowired
    private ProductRepository products;

    @Autowired
    private ProductImageRepository images;

    private static byte[] png(int size) {
        var data = new byte[Math.max(size, 12)];
        data[0] = (byte) 0x89;
        data[1] = 'P';
        data[2] = 'N';
        data[3] = 'G';
        return data;
    }

    @Test
    void aValidImageIsStoredAndServedFromTheDatabase() {
        var product = products.findBySlug("focaccia").orElseThrow();

        var stored = catalogue.storeImage(product, "focaccia.png", "image/png", png(2048));

        assertEquals("focaccia.png", stored.getFilename());
        assertEquals(2048, stored.getSizeBytes());
        assertTrue(images.findByProduct(product).isPresent());
    }

    @Test
    void anOversizedFileIsRefusedWithItsSize() {
        var product = products.findBySlug("baguette").orElseThrow();

        var failure = assertThrows(DomainException.RuleViolation.class,
                () -> catalogue.storeImage(product, "huge.png", "image/png", png(3 * 1024 * 1024)));

        assertEquals("catalogue.image.tooLarge", failure.translationKey());
        assertTrue(failure.arguments().length > 0, "the message names the size");
        assertFalse(images.findByProduct(product).isPresent(), "and nothing was written");
    }

    @Test
    void aFileThatOnlyClaimsToBeAnImageIsRefused() {
        var product = products.findBySlug("ciabatta").orElseThrow();
        var notAnImage = "MZ this is an executable".getBytes();

        var failure = assertThrows(DomainException.RuleViolation.class,
                () -> catalogue.storeImage(product, "evil.png", "image/png", notAnImage));

        assertEquals("catalogue.image.unsupported", failure.translationKey());
    }

    @Test
    void anUnsupportedTypeIsRefusedEvenWithValidBytes() {
        var product = products.findBySlug("empanada").orElseThrow();

        var failure = assertThrows(DomainException.RuleViolation.class,
                () -> catalogue.storeImage(product, "photo.gif", "image/gif", png(1024)));

        assertEquals("catalogue.image.unsupported", failure.translationKey());
    }
}
