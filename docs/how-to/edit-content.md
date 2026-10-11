# Implement an authorized editing flow

Use this guide when you already have application persistence and authenticated users.
SimplyPages supplies forms and rendering helpers; your application owns permissions,
CSRF handling, validation, storage, and approval state.

1. Load the resource and check permission before returning its edit form. Use
   `Editable.buildEditView()` directly or configure `EditModalBuilder.withEditable(...)`,
   `withSaveUrl(...)`, and the page/modal container IDs.
2. At the save endpoint, authenticate again, check resource-specific edit permission,
   and validate the request's CSRF token through your HTTP security layer.
3. Load a request-owned editable instance. Call `validate(formData)` and return visible
   errors if invalid. Do not mutate or persist on validation failure.
4. For an authorized direct edit, call `applyEdits(formData)` and persist the result.
   For approval workflows, store a proposed change instead. `EditMode.USER_EDIT` does not
   implement approval persistence for you.
5. Return escaped component output with the HTMX swap shape your client expects.

## Return out-of-band replacements

`EditModalBuilder` saves with `hx-swap="none"`; successful responses must contain out-of-band
updates. Given a rendered component `updated`, construct the response through safe attributes:

```java
String html = new io.mindspice.simplypages.components.Div()
    .withId("edit-modal-container").withAttribute("hx-swap-oob", "true").render()
    + new io.mindspice.simplypages.components.Div()
        .withId("module-container").withAttribute("hx-swap-oob", "true")
        .withChild(updated).render();
```

The outer `module-container` must already exist in the browser and have an ID distinct from
any ID on `updated`. For validation errors, send an OOB update to an existing error region
or replace the modal through a configured response target. A plain alert with no OOB marker
will not become visible under `hx-swap="none"`.

Never concatenate request-provided IDs into raw HTML. `AuthWrapper` can gate a callback,
but does not replace endpoint security or establish the user's identity.

See the [editing API](../reference/editing-api-reference.md),
[existing editing walkthrough](../getting-started/03-editing-system-first-implementation.md),
and [security explanation](../explanation/security-and-integration.md).

[All how-to guides](README.md)
