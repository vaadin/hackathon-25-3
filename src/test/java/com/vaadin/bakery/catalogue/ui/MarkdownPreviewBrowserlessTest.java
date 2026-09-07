package com.vaadin.bakery.catalogue.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.SafeHtml;
import com.vaadin.bakery.TestLogin;
import com.vaadin.browserless.SpringBrowserlessTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * ADM-02. What the administrator sees while typing is what a visitor gets.
 *
 * The two are the same component bound to a signal, which is the whole point of
 * the arrangement, and that is not enough on its own: the public page cleans
 * the markdown before it renders it and the editor did not, so anything the
 * safelist strips looked fine to the person writing it and vanished for
 * everybody else. A preview that flatters the author is worse than no preview.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class MarkdownPreviewBrowserlessTest extends SpringBrowserlessTest {

    @BeforeEach
    void signIn() {
        TestLogin.asAdmin();
    }

    /**
     * What the preview holds, from the editor rather than from the component.
     *
     * The dialog's fields are not in the tree the finder walks, and
     * {@code Markdown.getContent()} throws {@code BindingActiveException} when
     * the content is bound to a signal, which it is here and which is the whole
     * point of the arrangement. Both rows are in
     * {@code specs/FEEDBACK-25.3.md}.
     */
    private ProductEditor editor() {
        return find(ProductAdminView.class).single().editor();
    }

    @Test
    void thePreviewRendersWhatThePublicPageWouldRender() {
        navigate(ProductAdminView.class);
        var view = find(ProductAdminView.class).single();
        view.editor().newProduct();

        var typed = "**Butter** and _flour_. <script>alert('no')</script> Nothing else.";
        test(editor().descriptionField()).setValue(typed);

        assertEquals(SafeHtml.clean(typed), editor().previewContent(),
                "the preview shows the cleaned markdown, which is what a visitor gets");
        assertFalse(editor().previewContent().contains("<script>"),
                "and not the script the safelist removes: " + editor().previewContent());
    }

    /** The ordinary case still works: markdown the safelist keeps is untouched. */
    @Test
    void markdownThatSurvivesTheSafelistIsShownAsTyped() {
        navigate(ProductAdminView.class);
        find(ProductAdminView.class).single().editor().newProduct();

        var typed = "**Butter** and _flour_, nothing else.";
        test(editor().descriptionField()).setValue(typed);

        assertEquals(typed, editor().previewContent(), "nothing was taken away");
    }
}
