REPO: vaadin/web-components
TITLE: A Chart ignores the theme until styled mode is turned on, and the docs that list the style properties do not say so
---
### Description

A `Chart` renders with Highcharts' own palette on a hardcoded white plot area, whatever the colour scheme is. The `--vaadin-charts-*` style properties are inert: the component's stylesheet guards every one of them behind `:where([styled-mode])`, and Flow does not set it.

The switch is `configuration.getChart().setStyledMode(true)`.

### Why it matters

On Aura, and in dark mode, this is four white rectangles on a dark page, and nothing says why. The default is also wrong for 25.3, where Aura and its colour schemes are the default theme.

### Documentation

It is documented only on the Charts styling sub page. "Basic Use", which is what a reader opens to draw their first chart, does not mention it, and neither does the table of style properties that lists the properties it enables. That table also gives Lumo variables as its default values, in a release whose default theme is Aura; the shipped stylesheet actually falls back to the base `--vaadin-*` properties.

### Expected

Styled mode on by default, or a note where the properties are listed saying none of them do anything until it is on.

Found on 25.3.0-beta1.
