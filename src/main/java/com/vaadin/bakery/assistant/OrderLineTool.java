package com.vaadin.bakery.assistant;

import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.catalogue.Product;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.bakery.ordering.ui.OrderLineEditor;
import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.ai.provider.ToolException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import tools.jackson.databind.JsonNode;

/**
 * Adding a line to the order under edit.
 *
 * This is a tool rather than a form field, because the lines are a list that
 * grows: {@code FormAIController} fills fields that already exist, and the
 * second line of an order has no field until the first one is filled. A tool
 * has no such problem, and it is also the only place the two rules of AC2 that
 * concern a line can be enforced in one piece of code that the model learns
 * from.
 *
 * Both refusals throw {@link ToolException}, which is what the model is shown.
 * It names what was wrong and what would be right, because a refusal the model
 * cannot act on just produces the same call again.
 */
public final class OrderLineTool implements LLMProvider.ToolSpec {

    private static final int MIN = 1;
    private static final int MAX = 99;

    private final CatalogueService catalogue;
    private final OrderLineEditor editor;
    private final com.vaadin.flow.component.UI ui;

    public OrderLineTool(CatalogueService catalogue, OrderLineEditor editor, com.vaadin.flow.component.UI ui) {
        this.catalogue = catalogue;
        this.editor = editor;
        this.ui = ui;
    }

    @Override
    public String getName() {
        return "add_order_line";
    }

    @Override
    public String getDescription() {
        return "Add one line to the order under edit. The product must be one the bakery sells, "
                + "named as the catalogue spells it, and the quantity must be between " + MIN + " and " + MAX
                + ". Call it once per line. It refuses anything it cannot serve, and says why.";
    }

    @Override
    public String getParametersSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "product": { "type": "string", "description": "Product name as the catalogue spells it." },
                    "quantity": { "type": "integer", "description": "How many, between 1 and 99." }
                  },
                  "required": ["product", "quantity"]
                }
                """;
    }

    @Override
    public String execute(JsonNode arguments) {
        if (arguments == null || !arguments.isObject()) {
            throw new ToolException("Arguments must be a JSON object with 'product' and 'quantity'.");
        }
        var wanted = arguments.path("product").asString("").trim();
        if (wanted.isEmpty()) {
            throw new ToolException("Name the product. " + whatIsSold());
        }
        int quantity = arguments.path("quantity").asInt(0);

        var product = match(wanted).orElseThrow(() -> new ToolException(
                "The bakery does not sell anything called \"" + wanted + "\". " + whatIsSold()));
        if (quantity < MIN || quantity > MAX) {
            throw new ToolException("A line has to be between " + MIN + " and " + MAX + " of something, and "
                    + quantity + " is not. Ask the barista what they meant.");
        }

        // The editor is a component, so the write belongs on the UI thread.
        return UiWork.on(ui, () -> {
            var lines = new ArrayList<>(editor.getLines());
            lines.add(new CartLine(product.getId(), quantity, null));
            editor.setLines(List.copyOf(lines));
            return "Added " + quantity + " x " + product.getName() + ".";
        });
    }

    /**
     * Exact name first, then a forgiving match, because a model that has been
     * told the catalogue still writes "croissants" for "Butter croissant".
     */
    private java.util.Optional<Product> match(String wanted) {
        var normalised = wanted.toLowerCase(Locale.ROOT);
        var available = catalogue.availableProducts();
        return available.stream()
                .filter(product -> product.getName().equalsIgnoreCase(wanted))
                .findFirst()
                .or(() -> available.stream()
                        .filter(product -> product.getName().toLowerCase(Locale.ROOT).contains(normalised)
                                || normalised.contains(product.getName().toLowerCase(Locale.ROOT)))
                        .findFirst());
    }

    private String whatIsSold() {
        return "The catalogue holds: " + catalogue.availableProducts().stream()
                .map(Product::getName)
                .reduce((left, right) -> left + ", " + right)
                .orElse("nothing today") + ".";
    }
}
