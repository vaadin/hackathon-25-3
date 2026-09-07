REPO: vaadin/web-components
TITLE: LoginOverlay renders its CSRF field with no name or value until the component itself submits
---
### Description

`LoginOverlay` renders

```html
<form method="POST" action="login">
  <input id="csrf" type="hidden">
  ...
</form>
```

The hidden input has neither `name` nor `value`. The token lives in `<meta name="_csrf">` and `<meta name="_csrf_parameter">`, and the component copies it into that field inside its own submit handler.

Any code that fills the two visible inputs and calls `form.submit()` therefore posts a login with **no token at all**. Spring rejects it, and because the rejection is a redirect the browser lands on the application's landing page, which is usually open to everyone: the result looks exactly like a successful login with nobody signed in.

### Reproduction

```js
const f = document.querySelector('form');
f.querySelector('input[name=username]').value = 'user';
f.querySelector('input[name=password]').value = 'password';
f.submit();
```

against a Vaadin application with Spring Security and CSRF enabled. Compare with clicking the component's own submit button.

### Why it matters

It is invisible whether it works or not. The obvious defence, waiting for the hidden field to appear before submitting, is worthless: the field is in the first paint. Our browser test suite failed two runs in three on assertions about charts and panels, every one of them made against the login screen.

### Expected

The field carries its `name` and `value` from the moment it is rendered.

### Workaround

Fill it from the metas before submitting.

Found on 25.3.0-beta1.
