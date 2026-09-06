# Staff order board Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the staff order board the one place an order is found, read, changed and taken, without ever losing the list.

**Architecture:** The board becomes the master area of a master-detail pair and acts as the router layout for `orders/:reference`, so opening an order slides a panel in beside the list instead of replacing the page and the address stays shareable. Reading at a glance stays in the row details, redesigned as item tiles that reflow to the width of the table. One line editor component serves the panel, the counter and the telephone, so the three ways an order arrives share a single surface.

**Tech Stack:** Vaadin 25.3.0-beta1 Flow (Java views, no Lit), Spring Boot 4, signals, `browserless-test-junit6`, CSS container queries.

**Spec:** `specs/features/06-order-board.md`

## Global Constraints

- Vaadin `25.3.0-beta1`. Query the Vaadin MCP before writing against any 25.3 API rather than recalling it.
- Views are Java. No Lit or Polymer templates.
- No user visible string literal in Java, annotation values included. Everything goes through the bundles, and a key must be added to **all three** files: `translations.properties`, `translations_en.properties`, `translations_es.properties`. `TranslationCompletenessTest` fails otherwise.
- Any text that stays on screen is bound to the locale through `com.vaadin.bakery.base.i18n.Translations`. Use `bindText` for a component's own text, `bind` for a setter someone else owns, `onLocale` for content that has to be rebuilt. Text composed at the instant it is shown, a notification or a confirmation raised by a click, is exempt.
- Subscriptions register in `Component.whenAttached` and release through the returned registration. Never override `onAttach` or `onDetach`.
- Views never touch repositories. `OrderService` and its neighbours are the only transaction boundary.
- CSS lives in `src/main/resources/META-INF/resources/styles/views/`, imported from the one `styles.css`. No `themes/` folder, no `@CssImport`.
- The application is driven by the dev loop, never by Maven: `.vaadin/vaadin-dev status`, `start`, `apply`. Open the page in the browser **before** the first `apply`, and read `apply`'s exit code as the verdict.
- The gate is `./mvnw verify`. It must be green at every commit.
- `specs/features/06-order-board.md` names four test classes that are currently registered as gaps in `SpecConsistencyTest.NOT_WRITTEN_YET`. **Each task that writes one of them must delete its line from that set in the same commit**, or `theGapListIsStillHonest` fails.
- `OrderDetailsBandIT` (BOARD-13) is in `BROWSER_TIER` and is deliberately not written: this project has no IT files at all. Leave it alone.

---

### Task 1: A column menu that lists every column

**Files:**
- Create: `src/main/java/com/vaadin/bakery/base/ui/ColumnChooser.java`
- Modify: `src/main/java/com/vaadin/bakery/ordering/ui/OrderBoardView.java:135-152` (replace `columnChooser()` and `addColumnToggle(...)`)
- Modify: `src/main/resources/vaadin-i18n/translations.properties`, `translations_en.properties`, `translations_es.properties`
- Modify: `src/test/java/com/vaadin/bakery/base/SpecConsistencyTest.java` (drop the `ColumnVisibilityBrowserlessTest` gap line)
- Test: `src/test/java/com/vaadin/bakery/ordering/ui/ColumnVisibilityBrowserlessTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `ColumnChooser.of(MenuBar menu, String labelKey, List<ColumnChooser.Entry<T>> entries)`, where `Entry` is `record Entry<T>(Grid.Column<T> column, String labelKey)`. Task 3 keeps calling it after the board changes shape.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/vaadin/bakery/ordering/ui/ColumnVisibilityBrowserlessTest.java`:

```java
package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.Order;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/** BOARD-10 and BOARD-11. The menu is the full inventory of columns, and it cannot empty the table. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "barista@bakery.test", roles = { "BARISTA" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ColumnVisibilityBrowserlessTest extends SpringBrowserlessTest {

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    @SuppressWarnings("unchecked")
    private Grid<Order> grid() {
        return (Grid<Order>) find(Grid.class).single();
    }

    private List<Checkbox> toggles() {
        return find(Checkbox.class).all().stream()
                .filter(box -> box.getLabel() != null && !box.getLabel().isBlank())
                .filter(box -> !"Show past orders".equals(box.getLabel()))
                .toList();
    }

    @Test
    void everyColumnIsListedWithItsCurrentState() {
        navigate(OrderBoardView.class);

        var labels = toggles().stream().map(Checkbox::getLabel).toList();
        assertEquals(List.of("Reference", "Customer", "Pickup", "State", "Items", "Channel", "Total"), labels,
                "the menu lists every column, in table order");

        var byLabel = toggles().stream()
                .collect(java.util.stream.Collectors.toMap(Checkbox::getLabel, box -> box));
        assertTrue(byLabel.get("Reference").getValue(), "reference is on to begin with");
        assertFalse(byLabel.get("Items").getValue(), "the expensive column is off to begin with");
    }

    @Test
    void turningAColumnOnChangesTheTable() {
        navigate(OrderBoardView.class);
        var items = toggles().stream().filter(box -> "Items".equals(box.getLabel())).findFirst().orElseThrow();
        var column = grid().getColumns().stream().filter(c -> "items".equals(c.getKey())).findFirst().orElseThrow();

        assertFalse(column.isVisible(), "off before");
        test(items).setValue(true);
        assertTrue(column.isVisible(), "and on after");
    }

    @Test
    void theLastVisibleColumnCannotBeTurnedOff() {
        navigate(OrderBoardView.class);
        var on = toggles().stream().filter(Checkbox::getValue).toList();
        // Turn them all off but the last one, which must refuse.
        on.subList(0, on.size() - 1).forEach(box -> test(box).setValue(false));
        var last = on.getLast();

        test(last).setValue(false);

        assertTrue(last.getValue(), "the toggle springs back");
        assertEquals(1, grid().getColumns().stream().filter(Grid.Column::isVisible).count(),
                "and the table still has a column");
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `./mvnw -o test -Dtest=ColumnVisibilityBrowserlessTest -DfailIfNoSpecifiedTests=false`
Expected: FAIL. The first test fails on the label list, because today only the items column is in the menu.

- [ ] **Step 3: Write the chooser**

Create `src/main/java/com/vaadin/bakery/base/ui/ColumnChooser.java`:

```java
package com.vaadin.bakery.base.ui;

import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.menubar.MenuBar;
import java.util.List;

/**
 * Turns columns on and off. The menu is the full inventory: a column that is
 * not listed here is one nobody can discover, and a table that can be emptied
 * of every column is one somebody will empty by accident.
 */
public final class ColumnChooser {

    /** A column and the key naming it, so the menu reads the same as the header. */
    public record Entry<T>(Grid.Column<T> column, String labelKey) {
    }

    private ColumnChooser() {
    }

    public static <T> void of(MenuBar menu, String labelKey, List<Entry<T>> entries) {
        var item = menu.addItem("");
        Translations.bind(menu, item::setText, labelKey);
        var submenu = item.getSubMenu();

        for (Entry<T> entry : entries) {
            var toggle = new Checkbox();
            Translations.bind(toggle, toggle::setLabel, entry.labelKey());
            toggle.setValue(entry.column().isVisible());
            toggle.addValueChangeListener(event -> {
                if (Boolean.FALSE.equals(event.getValue()) && isLastVisible(entries, entry)) {
                    // Springs back rather than leaving a table with no columns.
                    toggle.setValue(true);
                    return;
                }
                entry.column().setVisible(Boolean.TRUE.equals(event.getValue()));
            });
            submenu.addItem(toggle);
        }
    }

    private static <T> boolean isLastVisible(List<Entry<T>> entries, Entry<T> candidate) {
        return entries.stream().filter(entry -> entry.column().isVisible()).count() <= 1
                && candidate.column().isVisible();
    }
}
```

- [ ] **Step 4: Give every column a key and wire the chooser**

In `OrderBoardView`, give each column a key when it is created, so the test and the chooser can name them. Add `.setKey("reference")`, `"customer"`, `"slot"`, `"state"`, `"items"`, `"channel"`, `"total"` to the seven columns, keep `summaryColumn` as the field it already is, and hold the other six in local variables.

Replace `columnChooser()` and delete `addColumnToggle(...)`:

```java
    private Div columnChooser() {
        var menu = new MenuBar();
        ColumnChooser.of(menu, "board.columns", List.of(
                new ColumnChooser.Entry<>(referenceColumn, "board.column.reference"),
                new ColumnChooser.Entry<>(customerColumn, "board.column.customer"),
                new ColumnChooser.Entry<>(slotColumn, "board.column.slot"),
                new ColumnChooser.Entry<>(stateColumn, "board.column.state"),
                new ColumnChooser.Entry<>(summaryColumn, "board.column.items"),
                new ColumnChooser.Entry<>(channelColumn, "board.column.channel"),
                new ColumnChooser.Entry<>(totalColumn, "board.column.total")));
        var chooser = new Div(menu);
        chooser.addClassName("order-board__columns");
        return chooser;
    }
```

Promote `referenceColumn`, `customerColumn`, `slotColumn`, `stateColumn`, `channelColumn` and `totalColumn` to fields beside the existing `summaryColumn`, all `private final Grid.Column<Order>`, assigned in the constructor where the columns are built today.

- [ ] **Step 5: Run the test and watch it pass**

Run: `./mvnw -o test -Dtest=ColumnVisibilityBrowserlessTest -DfailIfNoSpecifiedTests=false`
Expected: PASS, 3 tests.

- [ ] **Step 6: Close the gap and run the gate**

Delete the line `"ColumnVisibilityBrowserlessTest",` from `NOT_WRITTEN_YET` in `src/test/java/com/vaadin/bakery/base/SpecConsistencyTest.java`.

Run: `./mvnw -o verify`
Expected: BUILD SUCCESS, 178 tests.

- [ ] **Step 7: Make it live and look at it**

```bash
.vaadin/vaadin-dev status
.vaadin/vaadin-dev apply
```

Open `http://localhost:8080/orders` **before** the apply. Confirm the menu lists seven columns and that toggling one changes the table.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/vaadin/bakery/base/ui/ColumnChooser.java \
        src/main/java/com/vaadin/bakery/ordering/ui/OrderBoardView.java \
        src/main/resources/vaadin-i18n \
        src/test/java/com/vaadin/bakery/ordering/ui/ColumnVisibilityBrowserlessTest.java \
        src/test/java/com/vaadin/bakery/base/SpecConsistencyTest.java
git commit -m "List every column in the board's column menu

Only the expensive column was reachable, so the other two hidden ones
could be found only by reading the source."
```

---

### Task 2: Row details as item tiles

**Files:**
- Create: `src/main/java/com/vaadin/bakery/ordering/ui/OrderDetailsBand.java`
- Modify: `src/main/java/com/vaadin/bakery/ordering/ui/OrderBoardView.java` (replace `details(Order)` with the new component)
- Modify: `src/main/resources/META-INF/resources/styles/views/board.css`
- Test: `src/test/java/com/vaadin/bakery/ordering/ui/OrderBoardBrowserlessTest.java` (add BOARD-12)

**Interfaces:**
- Consumes: `OrderService.detailLines(String reference)` returning `List<OrderDetailLine>`, where `OrderDetailLine` is `record OrderDetailLine(int quantity, String productName, String comment, List<String> allergenKeys, Money gross)`. `OrderService.recentHistoryKeys(String reference, int count)` returning `List<String>`.
- Produces: `new OrderDetailsBand(OrderService orders, String reference)`, a `Div` carrying class `order-board__band`. Task 3 keeps using it unchanged.

- [ ] **Step 1: Write the failing test**

Add to `OrderBoardBrowserlessTest`:

```java
    /** BOARD-12. The band leads with quantities and still carries allergens and history. */
    @Test
    void theExpandedRowShowsATilePerLine() {
        navigate(OrderBoardView.class);
        var order = orders.findAll().stream()
                .filter(candidate -> !candidate.getItems().isEmpty())
                .findFirst()
                .orElseThrow();

        grid().setDetailsVisible(order, true);

        var band = find(com.vaadin.flow.component.html.Div.class).all().stream()
                .filter(div -> div.getClassNames().contains("order-board__band"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the expanded row has no band"));
        var tiles = band.getChildren()
                .flatMap(child -> child.getChildren())
                .filter(child -> child.getElement().getClassList().contains("order-board__tile"))
                .toList();

        assertEquals(orderService.detailLines(order.getReference()).size(), tiles.size(),
                "one tile per line");
        var quantities = tiles.stream()
                .map(tile -> tile.getChildren().findFirst().orElseThrow().getElement().getText())
                .toList();
        assertTrue(quantities.stream().allMatch(text -> text.matches("\\d+")),
                "each tile leads with its quantity, got " + quantities);
    }
```

- [ ] **Step 2: Run it and watch it fail**

Run: `./mvnw -o test -Dtest=OrderBoardBrowserlessTest -DfailIfNoSpecifiedTests=false`
Expected: FAIL with "the expanded row has no band".

- [ ] **Step 3: Write the band**

Create `src/main/java/com/vaadin/bakery/ordering/ui/OrderDetailsBand.java`:

```java
package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.SafeHtml;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * The quick answer to "what is in this one", asked without losing the queue.
 *
 * The quantities are what somebody is actually reading, so they are the largest
 * thing here and everything else arranges itself around them.
 */
public class OrderDetailsBand extends Div {

    public OrderDetailsBand(OrderService orders, String reference) {
        addClassName("order-board__band");

        var tiles = new Div();
        tiles.addClassName("order-board__tiles");
        orders.detailLines(reference).forEach(line -> {
            var tile = new Div();
            tile.addClassName("order-board__tile");

            var quantity = new Span(String.valueOf(line.quantity()));
            quantity.addClassName("order-board__tile-quantity");
            var name = new Span(line.productName());
            name.addClassName("order-board__tile-name");
            tile.add(quantity, name);

            if (line.comment() != null && !line.comment().isBlank()) {
                var comment = new Span(SafeHtml.text(line.comment()));
                comment.addClassName("order-board__tile-comment");
                tile.add(comment);
            }
            tiles.add(tile);
        });

        var allergens = new Div();
        allergens.addClassName("order-board__band-allergens");
        orders.detailLines(reference).stream()
                .flatMap(line -> line.allergenKeys().stream())
                .distinct()
                .forEach(key -> {
                    var chip = Translations.bindText(new Span(), key);
                    chip.getElement().getThemeList().add("badge small");
                    allergens.add(chip);
                });

        var history = new Div();
        history.addClassName("order-board__band-history");
        orders.recentHistoryKeys(reference, 2)
                .forEach(key -> history.add(new Div(Translations.bindText(new Span(), key))));

        add(tiles, allergens, history);
    }
}
```

- [ ] **Step 4: Use it from the board**

In `OrderBoardView`, replace the body of `details(Order order)` with:

```java
    private Div details(Order order) {
        return new OrderDetailsBand(orderService, order.getReference());
    }
```

Delete the now unused imports the old body needed (`SafeHtml` stays only if used elsewhere in the file; the compiler will say).

- [ ] **Step 5: Style the band**

Append to `src/main/resources/META-INF/resources/styles/views/board.css`:

```css
/*
 * The band answers to the width of the table, not the width of the window. A
 * container query is the only thing that can: open the order panel and the
 * table narrows while the window never moves, and the tiles have to notice.
 */
.order-board__band {
  container-type: inline-size;
  padding-block: var(--vaadin-gap-m, 1rem);
}

.order-board__band > * {
  width: 100%;
  max-width: 52rem;
  margin-inline: auto;
}

.order-board__tiles {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--vaadin-gap-s, .5rem);
}

@container (min-width: 30rem) {
  .order-board__tiles {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@container (min-width: 46rem) {
  .order-board__tiles {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}

.order-board__tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: .1rem;
  padding: var(--vaadin-gap-s, .5rem);
  background: var(--vaadin-background-container, rgba(0, 0, 0, .04));
  border-radius: var(--vaadin-radius-m, 8px);
}

/* The number is the thing being read, so it is read first. */
.order-board__tile-quantity {
  font-size: 1.6rem;
  font-weight: 700;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.order-board__tile-name {
  font-size: .85em;
}

.order-board__tile-comment {
  font-size: .8em;
  opacity: .7;
  font-style: italic;
}

.order-board__band-allergens,
.order-board__band-history {
  display: flex;
  flex-wrap: wrap;
  gap: var(--vaadin-gap-s, .5rem);
  margin-block-start: var(--vaadin-gap-m, 1rem);
}

.order-board__band-history {
  flex-direction: column;
  gap: .1rem;
  opacity: .75;
  font-size: .9em;
}
```

- [ ] **Step 6: Run the test and the gate**

Run: `./mvnw -o test -Dtest=OrderBoardBrowserlessTest -DfailIfNoSpecifiedTests=false`
Expected: PASS.

Run: `./mvnw -o verify`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Make it live and look at it**

Open `http://localhost:8080/orders`, then `.vaadin/vaadin-dev apply`. Expand a row. Confirm the tiles are centred rather than flush left, that the quantity is the biggest thing in each tile, and that narrowing the browser reflows them. The true test of BOARD-13, narrowing the **table** with the window still, comes in Task 3.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/vaadin/bakery/ordering/ui/OrderDetailsBand.java \
        src/main/java/com/vaadin/bakery/ordering/ui/OrderBoardView.java \
        src/main/resources/META-INF/resources/styles/views/board.css \
        src/test/java/com/vaadin/bakery/ordering/ui/OrderBoardBrowserlessTest.java
git commit -m "Read an expanded order as tiles rather than a left hand list

The quantities are what a counter reads first, and they were the smallest
thing in the row."
```

---

### Task 3: The order opens beside the list

**Files:**
- Modify: `src/main/java/com/vaadin/bakery/ordering/ui/OrderBoardView.java` (extend `MasterDetailLayout`, implement `RouterLayout`)
- Modify: `src/main/java/com/vaadin/bakery/ordering/ui/OrderDetailView.java` (route layout, close control)
- Modify: `src/main/resources/META-INF/resources/styles/views/board.css`
- Modify: `src/main/resources/vaadin-i18n/*.properties` (add `board.panel.close`)
- Modify: `src/test/java/com/vaadin/bakery/base/SpecConsistencyTest.java` (drop the `OrderPanelBrowserlessTest` gap line)
- Test: `src/test/java/com/vaadin/bakery/ordering/ui/OrderPanelBrowserlessTest.java`

**Interfaces:**
- Consumes: `OrderDetailsBand` from Task 2, `ColumnChooser` from Task 1.
- Produces: `OrderBoardView` is a `MasterDetailLayout` and the route layout of `orders/:reference`. Task 4 adds the editor inside `OrderDetailView`, and Task 5 adds a "new order" action to `OrderBoardView`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/vaadin/bakery/ordering/ui/OrderPanelBrowserlessTest.java`:

```java
package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.TestLogin;
import com.vaadin.bakery.ordering.OrderRepository;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/** BOARD-14, BOARD-15 and BOARD-16. Opening an order must not cost the list. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@WithMockUser(username = "barista@bakery.test", roles = { "BARISTA" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderPanelBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private OrderRepository orders;

    @BeforeEach
    void signIn() {
        TestLogin.asBarista();
    }

    private String anyReference() {
        return orders.findAll().getFirst().getReference();
    }

    @Test
    void followingAnOrderAddressArrivesWithTheListStillThere() {
        UI.getCurrent().navigate("orders/" + anyReference());

        assertFalse(find(Grid.class).all().isEmpty(), "the list is still on screen");
        assertFalse(find(OrderDetailView.class).all().isEmpty(), "and the order is open beside it");
    }

    @Test
    void theBoardIsTheLayoutOfTheOrderAddress() {
        UI.getCurrent().navigate("orders/" + anyReference());

        assertEquals(1, find(OrderBoardView.class).all().size(),
                "one board, hosting the order rather than being replaced by it");
    }

    @Test
    void closingTheOrderReturnsToThePlainList() {
        UI.getCurrent().navigate("orders/" + anyReference());
        assertFalse(find(OrderDetailView.class).all().isEmpty());

        find(OrderDetailView.class).single().close();

        assertTrue(find(OrderDetailView.class).all().isEmpty(), "the panel is gone");
        assertFalse(find(Grid.class).all().isEmpty(), "the list remains");
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `./mvnw -o test -Dtest=OrderPanelBrowserlessTest -DfailIfNoSpecifiedTests=false`
Expected: FAIL. Today `OrderDetailView` replaces the board, so `find(Grid.class)` is empty.

- [ ] **Step 3: Turn the board into a master-detail**

Consult the Vaadin MCP for `MasterDetailLayout` before writing, then in `OrderBoardView`:

- Change `extends VerticalLayout` to `extends MasterDetailLayout`.
- Add `@ParentLayout(MainLayout.class)` beside `@Route("orders")` so the shell still wraps it when it is acting as a layout.
- Replace the constructor's final `add(toolbar(), columnChooser(), grid)` with:

```java
        setSizeFull();
        var master = new Div(toolbar(), columnChooser(), grid);
        master.addClassName("order-board__master");
        setMaster(master);
        setDetailSize("32rem");
        setOverlayContainment(MasterDetailLayout.OverlayContainment.LAYOUT);

        // Escape and a click outside are the two ways everybody already knows.
        addBackdropClickListener(event -> closePanel());
        addDetailEscapePressListener(event -> closePanel());
```

- Add:

```java
    private void closePanel() {
        getUI().ifPresent(ui -> ui.navigate("orders"));
    }
```

- Change the double click listener to open the panel rather than a page: it already calls `ui.navigate("orders/" + reference)`, which is now the panel. Leave it.

- [ ] **Step 4: Make the order view the child route**

In `OrderDetailView`:

- Replace `@RouteParent(OrderBoardView.class)` with `layout = OrderBoardView.class` on the route: `@Route(value = "orders/:reference", layout = OrderBoardView.class)`.
- Add a close control at the top of `render(Order)`, before the breadcrumbs:

```java
        var close = new Button(new Icon(VaadinIcon.CLOSE_SMALL), event -> close());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        Translations.bind(close, close::setAriaLabel, "board.panel.close");
        add(close);
```

- Add the method the test calls:

```java
    /** Also the way the board closes the panel, so both routes end in one place. */
    public void close() {
        getUI().ifPresent(ui -> ui.navigate("orders"));
    }
```

- [ ] **Step 5: Add the key to all three bundles**

```properties
board.panel.close=Close the order
```

Spanish: `board.panel.close=Cerrar el pedido`

- [ ] **Step 6: Style the panel**

Append to `board.css`:

```css
.order-board__master {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: var(--vaadin-gap-m, 1rem);
  box-sizing: border-box;
}

.order-detail {
  height: 100%;
  overflow-y: auto;
  padding: var(--vaadin-gap-m, 1rem);
  box-sizing: border-box;
}
```

Remove the `max-width: 46rem` from the existing `.order-detail` rule: the panel now has a width of its own and the cap fights it.

- [ ] **Step 7: Run the test and the gate**

Run: `./mvnw -o test -Dtest=OrderPanelBrowserlessTest -DfailIfNoSpecifiedTests=false`
Expected: PASS, 3 tests.

Run: `./mvnw -o verify`
Expected: BUILD SUCCESS. `OrderDetailBrowserlessTest` must still pass: it navigates to `orders/{reference}` and that address still resolves.

- [ ] **Step 8: Make it live and check the thing no test can**

Open `http://localhost:8080/orders`, `.vaadin/vaadin-dev apply`, then:

- Expand a row and note how many tiles sit on a line.
- Open an order **without touching the browser window**. The table narrows because the panel takes its space.
- Confirm the tiles reflowed. That is BOARD-13 by hand, and the reason `OrderDetailsBandIT` exists in the spec.
- Confirm Escape and a click outside both close the panel and the address returns to `/orders`.

- [ ] **Step 9: Close the gap and commit**

Delete `"OrderPanelBrowserlessTest",` from `NOT_WRITTEN_YET`.

```bash
git add src/main/java/com/vaadin/bakery/ordering/ui/OrderBoardView.java \
        src/main/java/com/vaadin/bakery/ordering/ui/OrderDetailView.java \
        src/main/resources/META-INF/resources/styles/views/board.css \
        src/main/resources/vaadin-i18n \
        src/test/java/com/vaadin/bakery/ordering/ui/OrderPanelBrowserlessTest.java \
        src/test/java/com/vaadin/bakery/base/SpecConsistencyTest.java
git commit -m "Open an order beside the list instead of replacing it

Losing the list is losing the queue, and the address still names the order
so the link can be shared."
```

---

### Task 4: Composing the lines of an order

**Files:**
- Create: `src/main/java/com/vaadin/bakery/ordering/ui/OrderLineEditor.java`
- Modify: `src/main/java/com/vaadin/bakery/ordering/OrderService.java` (add `updateLines`)
- Modify: `src/main/java/com/vaadin/bakery/ordering/ui/OrderDetailView.java` (host the editor, save)
- Modify: `src/main/resources/META-INF/resources/styles/views/board.css`
- Modify: `src/main/resources/vaadin-i18n/*.properties`
- Modify: `src/test/java/com/vaadin/bakery/base/SpecConsistencyTest.java` (drop the `OrderEditorBrowserlessTest` gap line)
- Test: `src/test/java/com/vaadin/bakery/ordering/ui/OrderEditorBrowserlessTest.java`

**Interfaces:**
- Consumes: `CartLine`, the record `CartLine(Long productId, int quantity, String comment)` with `withQuantity(int)` and `withComment(String)`. `CatalogueService.availableProducts()` returning `List<Product>`. `Money.times(int)`, `Money.plus(Money)`, `Money.ZERO`, `Money.format(Locale)`.
- Produces:
  - `new OrderLineEditor(CatalogueService catalogue)`
  - `void setLines(List<CartLine> lines)`
  - `List<CartLine> getLines()` — only rows that have a product and a quantity above zero
  - `Registration addLinesChangeListener(ComponentEventListener<OrderLineEditor.LinesChangeEvent> listener)`
  - `OrderService.updateLines(Order order, List<CartLine> lines, User actor)` returning `Order`

  Task 5 uses all of these.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/vaadin/bakery/ordering/ui/OrderEditorBrowserlessTest.java`:

```java
package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.browserless.SpringBrowserlessTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** BOARD-17 and BOARD-18. Any product, any quantity, and zero means remove. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class OrderEditorBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private CatalogueService catalogue;

    private OrderLineEditor editor() {
        var editor = new OrderLineEditor(catalogue);
        add(editor);
        return editor;
    }

    @Test
    void anEmptyRowIsAlwaysWaiting() {
        var editor = editor();
        assertEquals(0, editor.getLines().size(), "an empty row is not a line");
        assertTrue(editor.rowCount() >= 1, "but there is a row to type into");
    }

    @Test
    void aProductCanBeAddedInAnyQuantity() {
        var editor = editor();
        var product = catalogue.availableProducts().getFirst();

        editor.setLines(List.of(new CartLine(product.getId(), 4, null)));

        assertEquals(List.of(new CartLine(product.getId(), 4, null)), editor.getLines());
        assertEquals(product.price().times(4), editor.total(), "the total follows the lines");
        assertEquals(2, editor.rowCount(), "the filled row plus a fresh empty one");
    }

    @Test
    void aQuantityOfZeroRemovesTheLine() {
        var editor = editor();
        var product = catalogue.availableProducts().getFirst();
        editor.setLines(List.of(new CartLine(product.getId(), 2, null)));

        editor.setQuantity(0, 0);

        assertEquals(List.of(), editor.getLines(), "the line is gone");
        assertEquals(com.vaadin.bakery.base.Money.ZERO, editor.total());
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `./mvnw -o test -Dtest=OrderEditorBrowserlessTest -DfailIfNoSpecifiedTests=false`
Expected: FAIL to compile, `OrderLineEditor` does not exist.

- [ ] **Step 3: Write the editor**

Create `src/main/java/com/vaadin/bakery/ordering/ui/OrderLineEditor.java`:

```java
package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.Money;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.catalogue.Product;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.List;

/**
 * The lines of an order, wherever the order came from.
 *
 * There is always an empty row at the end, so adding a product is typing rather
 * than first asking for somewhere to type. A quantity of zero removes the line,
 * because that is what everybody tries first.
 */
public class OrderLineEditor extends Composite<Div> {

    /** Fired whenever the lines or the total change, so a host can follow them. */
    public static class LinesChangeEvent extends ComponentEvent<OrderLineEditor> {
        public LinesChangeEvent(OrderLineEditor source) {
            super(source, false);
        }
    }

    private final CatalogueService catalogue;
    private final Div rows = new Div();
    private final List<Row> live = new ArrayList<>();
    private final Span total = new Span();

    public OrderLineEditor(CatalogueService catalogue) {
        this.catalogue = catalogue;
        getContent().addClassName("order-editor");
        rows.addClassName("order-editor__rows");

        total.addClassName("order-editor__total");
        Translations.bindText(total, locale -> getTranslation(locale, "board.editor.total", total().format(locale)));

        getContent().add(rows, total);
        appendEmptyRow();
    }

    public void setLines(List<CartLine> lines) {
        live.clear();
        rows.removeAll();
        lines.forEach(line -> appendRow(line));
        appendEmptyRow();
        changed();
    }

    /** Only rows somebody actually filled in. */
    public List<CartLine> getLines() {
        return live.stream()
                .filter(row -> row.product.getValue() != null)
                .filter(row -> row.quantity.getValue() != null && row.quantity.getValue() > 0)
                .map(row -> new CartLine(row.product.getValue().getId(), row.quantity.getValue(),
                        row.comment.getValue() == null || row.comment.getValue().isBlank()
                                ? null : row.comment.getValue()))
                .toList();
    }

    public Money total() {
        return getLines().stream()
                .map(line -> catalogue.availableProducts().stream()
                        .filter(product -> product.getId().equals(line.productId()))
                        .findFirst()
                        .map(product -> product.price().times(line.quantity()))
                        .orElse(Money.ZERO))
                .reduce(Money.ZERO, Money::plus);
    }

    /** Test seam: how many rows are on screen, empty one included. */
    public int rowCount() {
        return live.size();
    }

    /** Test seam: set a row's quantity the way a user would. */
    public void setQuantity(int index, int quantity) {
        live.get(index).quantity.setValue(quantity);
    }

    public Registration addLinesChangeListener(ComponentEventListener<LinesChangeEvent> listener) {
        return addListener(LinesChangeEvent.class, listener);
    }

    private void appendRow(CartLine line) {
        var row = appendEmptyRow();
        catalogue.availableProducts().stream()
                .filter(product -> product.getId().equals(line.productId()))
                .findFirst()
                .ifPresent(row.product::setValue);
        row.quantity.setValue(line.quantity());
        row.comment.setValue(line.comment() == null ? "" : line.comment());
    }

    private Row appendEmptyRow() {
        var row = new Row();
        live.add(row);
        rows.add(row.layout);
        return row;
    }

    private void changed() {
        total.getElement().setProperty("dummy", "");
        fireEvent(new LinesChangeEvent(this));
    }

    /** One line: what, how many, what it costs, why, and a way out. */
    private final class Row {
        private final ComboBox<Product> product = new ComboBox<>();
        private final IntegerField quantity = new IntegerField();
        private final TextField comment = new TextField();
        private final Span price = new Span();
        private final Button remove = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
        private final Div layout;

        private Row() {
            Translations.bind(product, product::setLabel, "board.editor.product");
            product.setItems(catalogue.availableProducts());
            Translations.onLocale(product, locale -> product.setItemLabelGenerator(Product::getName));

            Translations.bind(quantity, quantity::setLabel, "board.editor.quantity");
            quantity.setMin(0);
            quantity.setMax(99);
            quantity.setStepButtonsVisible(true);
            quantity.setValue(1);

            Translations.bind(comment, comment::setLabel, "board.editor.comment");
            Translations.bind(remove, remove::setAriaLabel, "board.editor.remove");
            remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

            price.addClassName("order-editor__price");
            Translations.bindText(price, locale -> {
                var chosen = product.getValue();
                var count = quantity.getValue() == null ? 0 : quantity.getValue();
                return chosen == null ? "" : chosen.price().times(count).format(locale);
            });

            setEnabled(false);
            product.addValueChangeListener(event -> {
                setEnabled(event.getValue() != null);
                // Filling the last row is what earns a new empty one.
                if (event.getValue() != null && live.getLast() == this) {
                    appendEmptyRow();
                }
                changed();
            });
            quantity.addValueChangeListener(event -> {
                if (event.getValue() != null && event.getValue() == 0) {
                    removeRow();
                    return;
                }
                changed();
            });
            comment.addValueChangeListener(event -> changed());
            remove.addClickListener(event -> removeRow());

            layout = new Div(product, quantity, comment, price, remove);
            layout.addClassName("order-editor__row");
        }

        private void setEnabled(boolean enabled) {
            quantity.setEnabled(enabled);
            comment.setEnabled(enabled);
            remove.setEnabled(enabled);
        }

        private void removeRow() {
            live.remove(this);
            rows.remove(layout);
            if (live.isEmpty()) {
                appendEmptyRow();
            }
            changed();
        }
    }
}
```

- [ ] **Step 4: Add the service method that replaces an order's lines**

In `OrderService`, beside `place(...)`:

```java
    /**
     * Replaces an order's lines. The prices are taken again from the catalogue
     * rather than kept from the old items, because an order that is being
     * changed is being priced now.
     */
    @Transactional
    public Order updateLines(Order order, List<CartLine> lines, User actor) {
        if (lines.isEmpty()) {
            throw new DomainException.RuleViolation("ordering.cart.empty");
        }
        var current = orders.findById(order.getId())
                .orElseThrow(() -> new DomainException.NotFound("ordering.order.notFound"));

        var items = new java.util.ArrayList<OrderItem>();
        for (CartLine line : lines) {
            Product product = products.findById(line.productId())
                    .orElseThrow(() -> new DomainException.NotFound("catalogue.product.gone"));
            if (!product.isAvailable()) {
                throw new DomainException.RuleViolation("ordering.product.unavailable", product.getName());
            }
            var item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(line.quantity());
            item.setUnitPriceCents(product.getPriceCents());
            item.setVatRate(product.getVatRate());
            item.setComment(line.comment());
            items.add(item);
        }

        current.setItems(items);
        current.recalculateTotals();
        current.addHistory(new OrderHistoryItem(current.getState(), "ordering.history.linesChanged", actor));
        return orders.save(current);
    }
```

- [ ] **Step 5: Add the keys to all three bundles**

English and the fallback:

```properties
board.editor.product=Product
board.editor.quantity=Quantity
board.editor.comment=Comment
board.editor.price=Price
board.editor.remove=Remove this line
board.editor.total=Total {0}
board.editor.save=Save the order
ordering.history.linesChanged=Lines changed
```

Spanish:

```properties
board.editor.product=Producto
board.editor.quantity=Cantidad
board.editor.comment=Comentario
board.editor.price=Precio
board.editor.remove=Quitar esta linea
board.editor.total=Total {0}
board.editor.save=Guardar el pedido
ordering.history.linesChanged=Lineas modificadas
```

- [ ] **Step 6: Host the editor in the panel**

In `OrderDetailView.render(Order order)`, replace the read only `items` Div with the editor plus a save button:

```java
        var editor = new OrderLineEditor(catalogue);
        editor.setLines(orders.detailLines(order.getReference()).stream()
                .map(line -> new CartLine(productIdOf(line), line.quantity(), line.comment()))
                .toList());

        var save = Translations.bindText(new Button("", event -> {
            try {
                orders.updateLines(order, editor.getLines(), currentUser.get().orElse(null));
                close();
            } catch (DomainException failure) {
                Notification.show(getTranslation(failure.translationKey(), failure.arguments()))
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }), "board.editor.save");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(editor, save);
```

`OrderDetailLine` carries no product id, so add one to the read model rather than looking products up by name: change `OrderDetailLine` to `record OrderDetailLine(Long productId, int quantity, String productName, String comment, List<String> allergenKeys, Money gross)` and set it where `OrderService.detailLines` builds the record. Update `OrderDetailsBand` and any other reader the compiler names. Then the mapping above is `line.productId()` and `productIdOf` is not needed.

Inject `CatalogueService catalogue` into `OrderDetailView`'s constructor and keep it as a field.

- [ ] **Step 7: Style the editor**

Append to `board.css`:

```css
.order-editor__row {
  display: grid;
  grid-template-columns: 2fr 6rem 2fr 6rem 3rem;
  gap: var(--vaadin-gap-s, .5rem);
  align-items: end;
}

.order-editor__price {
  text-align: end;
  font-variant-numeric: tabular-nums;
  padding-block-end: .6rem;
}

.order-editor__total {
  display: block;
  text-align: end;
  font-size: 1.2rem;
  font-weight: 700;
  margin-block: var(--vaadin-gap-m, 1rem);
}

@container (max-width: 34rem) {
  .order-editor__row {
    grid-template-columns: 1fr 4rem 3rem;
  }

  .order-editor__row .order-editor__price {
    grid-column: 1 / -1;
  }
}
```

- [ ] **Step 8: Run the test and the gate**

Run: `./mvnw -o test -Dtest=OrderEditorBrowserlessTest -DfailIfNoSpecifiedTests=false`
Expected: PASS, 3 tests.

Run: `./mvnw -o verify`
Expected: BUILD SUCCESS. Fix any reader of `OrderDetailLine` the record change broke.

- [ ] **Step 9: Make it live, then close the gap and commit**

Open `/orders`, apply, open an order, add a product, watch the total follow, set a quantity to zero and watch the line go.

Delete `"OrderEditorBrowserlessTest",` from `NOT_WRITTEN_YET`.

```bash
git add src/main/java/com/vaadin/bakery/ordering src/main/resources/vaadin-i18n \
        src/main/resources/META-INF/resources/styles/views/board.css \
        src/test/java/com/vaadin/bakery/ordering/ui/OrderEditorBrowserlessTest.java \
        src/test/java/com/vaadin/bakery/base/SpecConsistencyTest.java
git commit -m "Edit the lines of an order from the panel

An order taken at the counter changes at the counter, and the price is
taken again from the catalogue rather than kept from the old line."
```

---

### Task 5: Taking an order at the counter and on the telephone

**Files:**
- Modify: `src/main/java/com/vaadin/bakery/ordering/ui/OrderBoardView.java` (a "new order" action)
- Create: `src/main/java/com/vaadin/bakery/ordering/ui/NewOrderView.java`
- Modify: `src/main/java/com/vaadin/bakery/assistant/ui/PhoneOrderView.java` (use the editor, save as a phone order)
- Modify: `src/main/resources/vaadin-i18n/*.properties`
- Modify: `src/test/java/com/vaadin/bakery/base/SpecConsistencyTest.java` (drop the `OrderCreationBrowserlessTest` gap line)
- Test: `src/test/java/com/vaadin/bakery/ordering/ui/OrderCreationBrowserlessTest.java`

**Interfaces:**
- Consumes: `OrderLineEditor` from Task 4. `OrderService.place(List<CartLine>, Customer, PickupLocation, LocalDate, LocalTime, Channel, String, User)`. `CustomerService.findOrCreate(String, String, String, String)`. `SlotService.today()` and `nextFreeTime(PickupLocation, LocalDate)`. `Channel.COUNTER` and `Channel.PHONE`.
- Produces: nothing later tasks rely on. This is the last task.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/vaadin/bakery/ordering/ui/OrderCreationBrowserlessTest.java`:

```java
package com.vaadin.bakery.ordering.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.bakery.Application;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.bakery.ordering.Channel;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.bakery.ordering.SlotService;
import com.vaadin.bakery.people.CustomerService;
import com.vaadin.browserless.SpringBrowserlessTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/** BOARD-20, BOARD-21 and BOARD-22. The bakery takes orders three ways and records all three. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderCreationBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    private OrderService orders;
    @Autowired
    private CatalogueService catalogue;
    @Autowired
    private CustomerService customers;
    @Autowired
    private PickupLocationRepository locations;
    @Autowired
    private SlotService slots;

    private com.vaadin.bakery.ordering.Order take(Channel channel) {
        var product = catalogue.availableProducts().getFirst();
        var location = locations.findByActiveTrueOrderByNameAsc().getFirst();
        var date = slots.today().plusDays(7);
        var time = slots.nextFreeTime(location, date).orElseThrow();
        var customer = customers.findOrCreate("Ada", "Nord", "ada@example.test", "+34600000000");
        return orders.place(List.of(new CartLine(product.getId(), 3, null)), customer, location, date, time,
                channel, null, null);
    }

    @Test
    void anOrderTakenAtTheCounterIsRecordedAsOne() {
        var order = take(Channel.COUNTER);
        assertEquals(Channel.COUNTER, orders.byReference(order.getReference()).orElseThrow().getChannel());
    }

    @Test
    void anOrderTakenOnTheTelephoneIsRecordedAsOne() {
        var order = take(Channel.PHONE);
        assertEquals(Channel.PHONE, orders.byReference(order.getReference()).orElseThrow().getChannel());
    }

    @Test
    void anOrderWithNoLinesIsRefused() {
        var location = locations.findByActiveTrueOrderByNameAsc().getFirst();
        var date = slots.today().plusDays(7);
        var time = slots.nextFreeTime(location, date).orElseThrow();
        var customer = customers.findOrCreate("Ada", "Nord", "ada@example.test", "+34600000000");

        var refusal = assertThrows(DomainException.class, () -> orders.place(List.of(), customer, location,
                date, time, Channel.COUNTER, null, null));

        assertTrue(refusal.translationKey().contains("empty"), "it says why, got " + refusal.translationKey());
    }

    @Test
    void theCounterAndTheTelephoneOfferTheSameEditor() {
        var counter = new OrderLineEditor(catalogue);
        var phone = new OrderLineEditor(catalogue);
        assertEquals(counter.getClass(), phone.getClass(),
                "one editor, so learning one is learning the other");
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `./mvnw -o test -Dtest=OrderCreationBrowserlessTest -DfailIfNoSpecifiedTests=false`
Expected: the first two fail if `place` is reached but nothing in the UI offers it; they pass at the service level immediately, which is the point: the service was always able, and nothing called it. If they pass at once, keep them, they are the regression guard, and the work below is what makes the behaviour reachable.

- [ ] **Step 3: Add the counter view as a second child route**

Create `src/main/java/com/vaadin/bakery/ordering/ui/NewOrderView.java`, routed into the board's detail area so a new order opens where an existing one opens:

```java
package com.vaadin.bakery.ordering.ui;

import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.base.security.CurrentUser;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.ordering.Channel;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.bakery.ordering.SlotService;
import com.vaadin.bakery.people.CustomerService;
import com.vaadin.bakery.people.Role;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/** An order taken at the counter, in the panel where every other order is read. */
@Route(value = "orders/new-counter", layout = OrderBoardView.class)
@PageTitle("New order")
@RolesAllowed({ Role.ADMIN_NAME, Role.BAKER_NAME, Role.BARISTA_NAME })
public class NewOrderView extends VerticalLayout {

    public NewOrderView(OrderService orders, CatalogueService catalogue, CustomerService customers,
            PickupLocationRepository locations, SlotService slots, CurrentUser currentUser) {
        addClassName("order-detail");
        add(Translations.bindText(new H2(), "board.new"));

        var firstName = new TextField();
        Translations.bind(firstName, firstName::setLabel, "checkout.firstName");
        var lastName = new TextField();
        Translations.bind(lastName, lastName::setLabel, "checkout.lastName");
        var email = new EmailField();
        Translations.bind(email, email::setLabel, "checkout.email");
        var phone = new TextField();
        Translations.bind(phone, phone::setLabel, "checkout.phone");
        add(new FormLayout(firstName, lastName, email, phone));

        var editor = new OrderLineEditor(catalogue);
        add(editor);

        var save = Translations.bindText(new Button("", event -> {
            try {
                var location = locations.findByActiveTrueOrderByNameAsc().getFirst();
                var date = slots.today();
                var time = slots.nextFreeTime(location, date)
                        .orElseThrow(() -> new DomainException.Conflict("ordering.slot.full", ""));
                var customer = customers.findOrCreate(firstName.getValue(), lastName.getValue(),
                        email.getValue(), phone.getValue());
                orders.place(editor.getLines(), customer, location, date, time, Channel.COUNTER, null,
                        currentUser.get().orElse(null));
                getUI().ifPresent(ui -> ui.navigate("orders"));
            } catch (DomainException failure) {
                Notification.show(getTranslation(failure.translationKey(), failure.arguments()))
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }), "board.editor.save");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(save);
    }
}
```

Add the action to the board's toolbar, in `OrderBoardView.toolbar()`:

```java
        var take = Translations.bindText(new Button("",
                event -> getUI().ifPresent(ui -> ui.navigate("orders/new-counter"))), "board.new");
        take.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
```

and include `take` in the toolbar's children.

- [ ] **Step 4: Let the telephone save**

In `PhoneOrderView`, replace the `orderLines` Div with an `OrderLineEditor` field, have `pasteAndParse()` fill it through `setLines(...)` instead of adding Divs, and add a save button that calls `place(..., Channel.PHONE, ...)` exactly as `NewOrderView` does, with the customer taken from the three fields already on that screen. Keep the assistant panel and the turn meter untouched: the difference between the two screens is the assistant, not the editor.

- [ ] **Step 5: Add the keys to all three bundles**

```properties
board.new=New order
```

Spanish: `board.new=Nuevo pedido`

- [ ] **Step 6: Run the test and the gate**

Run: `./mvnw -o test -Dtest=OrderCreationBrowserlessTest -DfailIfNoSpecifiedTests=false`
Expected: PASS, 4 tests.

Run: `./mvnw -o verify`
Expected: BUILD SUCCESS. `PhoneOrderBrowserlessTest` looks for a button reading "Fill from what they said" and a single `TextArea`; the editor adds no `TextArea`, but check it and adjust the test if the editor's comment fields change what `find(TextField.class)` returns.

- [ ] **Step 7: Make it live and take an order both ways**

Open `/orders`, apply, take a counter order end to end and confirm it appears in the list with the channel column turned on. Then open `/orders/new`, paste a sentence, and confirm the parsed lines land in the editor and save as a phone order.

- [ ] **Step 8: Close the gap and commit**

Delete `"OrderCreationBrowserlessTest",` from `NOT_WRITTEN_YET`.

```bash
git add src/main/java/com/vaadin/bakery src/main/resources/vaadin-i18n \
        src/test/java/com/vaadin/bakery/ordering/ui/OrderCreationBrowserlessTest.java \
        src/test/java/com/vaadin/bakery/base/SpecConsistencyTest.java
git commit -m "Take orders at the counter and on the telephone

The data has shown counter and phone orders since the first seed and no
part of the application could produce either."
```

---

## Self-review notes

- **Spec coverage.** AC1 is untouched existing behaviour. AC2 is Task 1. AC3 is untouched and guarded by BOARD-04. AC4 is Task 2, except the reflow half of it which is Task 3 step 8 by hand plus `OrderDetailsBandIT`, deliberately unwritten. AC5 is Task 3. AC6 is Task 4. AC7 is Task 5. AC8's keyboard half is exercised in Task 3 step 8 by hand; the named accessibility part is existing behaviour under BOARD-05. AC9 is untouched existing behaviour.
- **Known risk, flagged rather than hidden.** Task 3 assumes `@ParentLayout(MainLayout.class)` is what keeps the shell around a view that is itself acting as a layout under this project's automatic `@Layout`. That combination is not something I verified against a running application. Step 8 of that task is where it is caught, and if the shell disappears the fix is in the routing annotations, not in the design.
- **`OrderDetailLine` gains a field** in Task 4 step 6. That record is read by `OrderDetailsBand` from Task 2 and by anything else the compiler names. Doing it in Task 4 rather than Task 2 keeps Task 2's diff about the tiles.
