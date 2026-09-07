package com.vaadin.bakery.base.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.BrowserIT;
import com.vaadin.testbench.BrowserTest;

/**
 * FIX-10. The signed in person has a face, not just a name.
 *
 * This is a browser test because it has to be. The avatar sits inside a
 * {@code MenuBar} item, and browserless {@code find} cannot see components
 * there, exactly as it cannot see them inside a Grid component column. A
 * browserless test asserting the avatar exists fails while the header renders
 * it correctly, which is worse than no test.
 */
class UserAvatarIT extends BrowserIT {

    @BrowserTest
    void theHeaderShowsTheSignedInPerson() {
        signIn("admin@bakery.test", "admin");

        assertTrue(count(".app-header__user vaadin-avatar") == 1L,
                "an avatar beside the name, at " + whereAmI());
    }

    /**
     * The seeded users have no picture, so every one of them exercises the
     * fallback: initials rather than a broken image.
     */
    @BrowserTest
    void withNoPictureItDrawsInitials() {
        signIn("admin@bakery.test", "admin");

        var initials = String.valueOf(script(
                "return document.querySelector('.app-header__user vaadin-avatar').getAttribute('abbr');"));

        assertTrue("GR".equals(initials),
                "Goran Rich, drawn from the name because there is no image, got " + initials);
    }

    @BrowserTest
    void aSignedOutVisitorHasNoAvatar() {
        getDriver().manage().deleteAllCookies();
        open("/shop");

        assertTrue(count("vaadin-avatar") == 0L, "there is nobody to show, at " + whereAmI());
    }
}
