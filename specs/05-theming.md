# 05 Theming, responsive and PWA

The old app used `@Theme("bakery")`, a `themes/` folder, eight per component CSS files and a `shared-styles.js` imported by every Lit element. None of that survives.

## Four variants, chosen at runtime

The theme selector in the header offers each theme twice:

| Variant | What loads |
| --- | --- |
| Bakery (Aura) | Aura plus `styles/themes/bakery.css` |
| Bakery (Lumo) | Lumo plus `styles/themes/bakery.css`, the default |
| Default (Aura) | `Aura.STYLESHEET` and nothing else |
| Default (Lumo) | `Lumo.STYLESHEET` and nothing else |

The label names the palette first and the theme in brackets, because the palette is the thing being chosen and the theme is which one it sits on. The default variants exist so each theme can be seen as itself. An application that paints over both is an application that cannot answer "which theme do we prefer", which is the question worth asking during a hackathon on the release that introduced the second one.

- No theme is declared on the application shell. The shell loads exactly one at runtime, so neither theme leaks into the other.
- Only the two bakery variants add the palette, and that stylesheet sets colour and nothing else.
- The choice, and the light or dark scheme, live in a `@VaadinSessionScope` bean. In the layout they were lost on every navigation, which is not a preference.
- The selector is single choice. A checkable menu item does not clear its siblings, so the handler does it: without that it reads as a set of checkboxes and lets somebody appear to pick two themes at once.

Application stylesheets are written against the shared `--vaadin-*` tokens rather than either theme's own names, which is why the same CSS looks right under both. The one exception is the accent colour, which has no shared name and therefore lives only in the palette.

## How styles are loaded

- No `themes/` folder, no `@CssImport`, no `@Theme`.
- `Application` declares the Lumo stylesheets plus `@StyleSheet("styles.css")`.
- CSS lives under `src/main/resources/META-INF/resources/`. `styles.css` contains nothing but `@import` lines, one per view or component file.
- Files: `base/layout.css`, `base/typography.css`, then `views/<view-name>.css` and `components/<component-name>.css`.
- Every view sets its own class name and its stylesheet scopes everything under it, so no rule leaks.

## The palette

The bakery identity is declared once as `--bakery-*` tokens and mapped onto both themes, so the caramel accent and the warm paper survive a theme switch. Customisation is done with style properties, not with selectors into shadow parts.

```css
html {
  --aura-accent-color-light: #b4541f;
  --aura-accent-color-dark: #f0a06a;
  --aura-background-color-light: #fbf7f2;
  --aura-background-color-dark: #16130f;
  --aura-base-radius: 8;
  --aura-base-size: 20;
  --aura-contrast-level: 2;
  --aura-font-family: var(--aura-font-family-system);
}
```

Rules that keep light and dark honest:

- Customise with the `-light` and `-dark` suffixed properties. Apply with the unsuffixed one.
- Any colour that is not an Aura property uses `light-dark()`, never a hard coded pair.
- Dark mode goes through `Page.setColorScheme`, not a theme attribute. Both themes follow the CSS colour scheme, and so does every `light-dark()` value in our own stylesheets: setting `theme="dark"` on the body, which is what the Lumo era examples show, changes nothing at all and fails silently.
- Prefer built in component variants before writing CSS. Aura ships more of them than Lumo did, including the theme neutral `ERROR` and `SUCCESS` variants on ProgressBar and the reverse variant on Checkbox.

The palette is a bakery: warm off white paper, a burnt caramel accent, deep brown ink. Dark mode is not a colour inversion, it is a night bakery: near black background, warmer accent, lower contrast on large surfaces.

## Building blocks

Three classes carry most of the layout, defined once in `base/layout.css`:

| Class | What it is |
| --- | --- |
| `.page-block` | Full width. A `VerticalLayout` aligns its children to the start, so a plain block shrinks to its content and leaves the page looking half empty. Saying it once beats every view rediscovering it |
| `.panel` | One card: surface, radius, hairline, shadow, and table styling inside it |
| `.page-grid` | As many columns as fit, never narrower than the content needs |

Before these, five views had each defined their own panel with five slightly different shadows, and two pages were bare tables stacked in a column.

## Layout and motion

- Layout is CSS grid and flex with Aura spacing properties. Utility classes from `LumoUtility` are used for one off spacing, never for a whole layout.
- The storefront is a responsive card grid: four columns wide, two on a tablet, one on a phone, driven by CSS, not by Java.
- Structural changes that CSS cannot express react to `Page.windowSizeSignal()`. That is how the order board switches from a table to stacked cards, and how the checkout stepper collapses.
- Motion is limited to state transitions that a user caused: a card lifting on hover, a row flashing when a shared signal updates it, a badge counting up. Everything respects `prefers-reduced-motion`.

## Mobile

The bakery is used on a phone at the counter and by customers on the street.

| Surface | Behaviour on a small screen |
| --- | --- |
| Storefront | Single column cards, sticky filter bar, cart badge in the header |
| Cart and checkout | Full width steps, one field per row, the primary action pinned to the bottom |
| Order board | Cards instead of a grid, each card showing slot, customer, state and total |
| Kitchen board | One column per state becomes a horizontally scrolled row of columns, touch targets at least 44 pixels |
| Admin | Read and simple edits work, bulk editing is explicitly a desktop feature and says so |

Touch targets never go below 44 pixels. Nothing depends on hover to be discoverable.

## PWA

- `@PWA(name = "Bakery", shortName = "Bakery", offlinePath = "offline.html", offlineResources = { "images/offline.webp" })` on the application shell.
- Icons generated from one 512 pixel source, maskable variant included.
- The offline page is self contained: inline CSS, one image, the opening hours, the phone number, and a listener that reloads when the connection returns.
- Installability is verified by a Lighthouse pass in the demo checklist, not by an automated test.

## Accessibility

Not a separate epic, a rule everywhere.

- Every grid sets `GridI18n` so selection checkboxes and sorters are announced properly.
- Dialog, Popover, Badge and Card use `HasAriaRole`. Fields use `HasAriaDescription` for their helper text. Tooltips choose between label and description with `AriaLinkMode`.
- Login and Accordion set an explicit heading level so the page outline is sane.
- Colour never carries meaning alone. Order states pair a colour with a label and an icon.
- Contrast is checked in both schemes with the Aura contrast level, and the checklist in `08-testing.md` includes an axe pass per view.

## Product photographs

Forty eight photographs, 640 by 480, WebP, all opaque: none of them carries an alpha channel, and none is letterboxed. Five were shot on a white or near white background, one of them pure 255, and on a white card those had no edge at all and read as though the image had failed to load.

So a photo is not flush with the card edge. It sits inside a tinted frame, inset by a quarter of the gap unit and rounded, which gives every photograph a boundary whatever it was shot against. It costs a few pixels of image and it is the difference between a catalogue and a list of floating shapes.
