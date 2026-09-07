package com.example;

import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.router.PageTitleGenerator;
import org.springframework.stereotype.Component;

/**
 * A generator that knows how to name one route, annotated the way any Spring
 * developer annotates a class without thinking about it.
 *
 * The `@Component` is the whole bug. Remove it and every page is named
 * correctly; leave it and this generator names all of them.
 */
@Component
public class ProductTitle implements PageTitleGenerator {

    @Override
    public String generatePageTitle(PageTitleContext context) {
        return context.routeParameters().get("slug")
                .map(slug -> "Product " + slug)
                .orElse("We cannot find that product");
    }
}
