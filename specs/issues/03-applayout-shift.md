REPO: vaadin/web-components
TITLE: AppLayout paints its content at the full window width before reserving the drawer's space
---
### Description

On a cold load, `AppLayout` paints the view at the full window width and then reflows it by the drawer's width twenty to fifty milliseconds later. Its own attributes carry their final values throughout, so nothing observable says the layout has settled.

### Reproduction

[`03-applayout-shift/`](https://github.com/vaadin/hackathon-25-3/tree/9cf6235d4bf170002c44ac69c0d51063376828ba/specs/issues/03-applayout-shift) is a project of two classes: a shell with a drawer, and a view that samples its own width every twenty milliseconds and prints each change. Run it with `mvn spring-boot:run`, open port 8093 and **reload**, because an in application navigation does not shift.

What it printed at a 1400 pixel viewport, with a drawer 84 pixels wide:

```
21 ms  {"drawerOpened":true,"overlay":false,"drawerWidth":84,"contentWidth":1386}
63 ms  {"drawerOpened":true,"overlay":false,"drawerWidth":84,"contentWidth":1302}
```

The drawer is already 84 pixels wide in the first sample and the content is laid out as though it were not. Both attributes are identical in both samples.

The same thing in a real application, at a 1533 pixel viewport with a 256 pixel drawer:

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

### Getting the project

```
git clone --branch manolo --depth 1 https://github.com/vaadin/hackathon-25-3
cd hackathon-25-3/specs/issues/03-applayout-shift
mvn spring-boot:run
```

Found on 25.3.0-beta1. Not verified against 24.x, and `AppLayout` is old enough that this is probably not new.
