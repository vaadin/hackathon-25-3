package com.example;

import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.router.PageTitleGenerator;
import org.springframework.stereotype.Component;

/** A generator the application registers as a bean. */
@Component
public class Title implements PageTitleGenerator {

    /** Anything a test can recognise. */
    public static final String TITLE = "computed by the generator";

    @Override
    public String generatePageTitle(PageTitleContext context) {
        return TITLE;
    }
}
