REPO: vaadin/web-components
TITLE: AppLayout paints its content at the full window width before reserving the drawer's space
---
### Description

On a cold load, `AppLayout` paints the view at the full window width and then reflows it by the drawer's width twenty to fifty milliseconds later. Its own attributes carry their final values throughout, so nothing observable says the layout has settled.

### Reproduction

Load any route inside `AppLayout` on a screen wide enough for an inline drawer, and sample the content every twenty milliseconds. Measured at a 1533 pixel viewport:

| t | drawer-opened | overlay | drawer width | content width |
| --- | --- | --- | --- | --- |
| 169 ms | true | absent | 256 | **1533** |
| 190 ms | true | absent | 256 | **1277** |

At 169 ms the drawer, the navigation and the view are all rendered and opaque, and the content is laid out as though the drawer took no space. An in application navigation to the same route has no shift at all.

### Why it matters

Two things.

Every cold load of an application built on `AppLayout` shifts all of its content sideways by the drawer's width, after painting it.

And because `drawer-opened` and `overlay` are already final during the shift, there is nothing to wait for. A layout test that measures on arrival measures the wrong frame. We tried waiting for `drawer-opened`, for the absence of `overlay`, and for two equal readings in a row: four runs in nine still measured the pre shift width.

### Expected

The drawer's space is reserved before the content is painted, or an attribute or event marks the point where the layout has settled.

### Workaround

Do not compare an absolute width against one read earlier. Read a width and its container's in one call and compare the share.

Found on 25.3.0-beta1. Not verified against 24.x, and `AppLayout` is old enough that this is probably not new.
