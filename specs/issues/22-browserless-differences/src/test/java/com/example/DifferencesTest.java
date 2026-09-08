package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Two of the four differences, as tests. Both pass, and both passing is the
 * finding: the first one documents an exception nobody expects, and the second
 * one documents a title that never arrives.
 */
@SpringBootTest(classes = Application.class)
class DifferencesTest extends SpringBrowserlessTest {

    @Test
    void navigationCannotCarryAQueryString() {
        // The tier wraps what the router throws, so the cause is where the
        // message is. Either way the string form is refused.
        var thrown = assertThrows(Exception.class,
                () -> navigate("plain?token=abc", Views.PlainView.class));
        var message = thrown.getCause() == null ? thrown.getMessage() : thrown.getCause().getMessage();

        assertTrue(message != null && message.contains("query separator"), String.valueOf(message));

        // What works instead, and it needs the view looked up afterwards.
        UI.getCurrent().navigate("plain", QueryParameters.simple(Map.of("token", "abc")));
        assertEquals(1, find(Views.PlainView.class).all().size(), "the view is there after the other form");
    }

    /**
     * A `PageTitleGenerator` registered only as a bean, with no annotation on
     * any view. In a browser it becomes the whole application's title, which is
     * its own issue. The question here is whether the browserless tier applies
     * it at all.
     */
    @Test
    void aPageTitleGeneratorBeanReachesTheTitle() {
        navigate(Views.TitledView.class);

        assertEquals(Title.TITLE, UI.getCurrent().getInternals().getTitle(),
                "the tier does apply a generator registered as a bean");
    }
}
