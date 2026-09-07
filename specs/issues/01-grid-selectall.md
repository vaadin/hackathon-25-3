REPO: vaadin/web-components
TITLE: Grid paints the "Select All unavailable" screen reader text and it sizes the selection column
---
### Description

A `Grid` in multi select mode whose items come from a lazy data provider renders

```html
<th part="cell header-cell first-column-cell">
  <slot name="vaadin-grid-header-cell-content-0-29"></slot>
  <span class="sr-only">Select All unavailable</span>
</th>
```

into the selection column's header. Nothing inside the grid's shadow root styles `.sr-only`, so the sentence is painted as ordinary text, and because a header sizes its column the selection column comes out **199 pixels wide** with a checkbox in the middle of it.

Setting `setSelectAllCheckboxVisibility(HIDDEN)` does not remove it: the checkbox goes to `visibility: hidden` and the span stays.

### Reproduction

Any Flow grid populated lazily:

```java
grid.setSelectionMode(Grid.SelectionMode.MULTI);
grid.setItemsPageable(pageable -> repository.findAll(pageable).getContent(),
        pageable -> Math.toIntExact(repository.count()));
```

Open it and measure the first header cell: `document.querySelector('vaadin-grid').shadowRoot.querySelector('thead th')`. Its `innerText` is the sentence and its width is that of the sentence.

### Expected

The text is announced to a screen reader and not drawn. The selection column is the width of its checkbox.

### Workaround

`i18n.setSelectAllUnavailable("")`, which empties the span. The column then falls back to 59 pixels.

### Notes

The string is also not translated by the application's own i18n, so an application in another language shows one English sentence inside its table.

Found on 25.3.0-beta1.
