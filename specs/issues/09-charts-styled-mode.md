REPO: vaadin/web-components
TITLE: A Chart ignores the theme until styled mode is turned on, and the docs that list the style properties do not say so
---
### Description

A `Chart` renders with Highcharts' own palette on a hardcoded white plot area, whatever the colour scheme is. The `--vaadin-charts-*` style properties are inert: the component's stylesheet guards every one of them behind `:where([styled-mode])`, and Flow does not set it.

The switch is `configuration.getChart().setStyledMode(true)`.

### Reproduction

[`09-charts-styled-mode/`](https://github.com/vaadin/hackathon-25-3/tree/9cf6235d4bf170002c44ac69c0d51063376828ba/specs/issues/09-charts-styled-mode) is one view with the same chart twice, on a page in the dark colour scheme, and one line of difference between them. Run it with `mvn spring-boot:run` and open port 8110. `default-vs-styled.png` is what it draws.

What the SVG carries:

| | Default | `setStyledMode(true)` |
| --- | --- | --- |
| `.highcharts-background` fill | `#ffffff` | no attribute |
| series fill | `#2caffe` | no attribute |
| title colour | `rgb(102, 102, 102)` | the theme's, `oklch(1 0.002 260 / 0.65)` |

So the default chart bakes its colours into the SVG, and on a dark page it is a white rectangle with Highcharts' own palette and grey labels beside a chart that reads correctly.

### Why it matters

On Aura, and in dark mode, this is a white rectangle on a dark page and nothing says why. The default is also wrong for 25.3, where Aura and its colour schemes are the default theme.

### Documentation

It is documented only on the Charts styling sub page. "Basic Use", which is what a reader opens to draw their first chart, does not mention it, and neither does the table of style properties that lists the properties it enables. That table also gives Lumo variables as its default values, in a release whose default theme is Aura; the shipped stylesheet actually falls back to the base `--vaadin-*` properties.

### Expected

Styled mode on by default, or a note where the properties are listed saying none of them do anything until it is on.

### Getting the project

```
git clone --branch manolo --depth 1 https://github.com/vaadin/hackathon-25-3
cd hackathon-25-3/specs/issues/09-charts-styled-mode
mvn spring-boot:run
```

Found on 25.3.0-beta1.
