[Previous](02-dynamic-pages-with-slotkey-rendercontext.md) | [Index](../INDEX.md)

# Editing System First Implementation

This guide sketches an editing flow inside an authenticated application.
Application services, endpoint authorization, and CSRF configuration are prerequisites.
For response-target details, see [the editing how-to](../how-to/edit-content.md).

## Step 1: Use an Editable Module

`ContentModule` already implements `Editable<ContentModule>`.

```java
ContentModule module = ContentModule.create()
    .withModuleId("content-42")
    .withTitle("Release Notes")
    .withContent("Initial body");
```

## Step 2: Render an Edit Modal Endpoint

```java
@GetMapping("/modules/{id}/edit")
@ResponseBody
public String editModule(@PathVariable String id, Principal principal) {
    ContentModule module = moduleService.loadContentModule(id);

    return AuthWrapper.requireForEdit(
        () -> authz.canEdit(id, principal.getName()),
        () -> EditModalBuilder.create()
            .withTitle("Edit Module")
            .withModuleId(id)
            .withEditable(module)
            .withSaveUrl("/modules/" + id + "/save")
            .withDeleteUrl("/modules/" + id + "/delete")
            .withPageContainerId("page-content")
            .withModalContainerId("edit-modal-container")
            .build()
            .render()
    );
}
```

## Step 3: Handle Save Endpoint

```java
@PostMapping("/modules/{id}/save")
@ResponseBody
public String saveModule(@PathVariable String id, @RequestParam Map<String, String> formData,
                         Principal principal) {
    if (principal == null || !authz.canEdit(id, principal.getName())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    ContentModule module = moduleService.loadContentModule(id);

    ValidationResult vr = module.validate(formData);
    if (!vr.isValid()) {
        return new Div().withId("edit-errors").withAttribute("hx-swap-oob", "true")
            .withChild(Alert.danger(String.join(", ", vr.errors()))).render();
    }

    module.applyEdits(formData);
    moduleService.save(module);

    String closeModal = new Div().withId("edit-modal-container")
        .withAttribute("hx-swap-oob", "true").render();
    String updateModule = new Div().withId("container-" + id)
        .withAttribute("hx-swap-oob", "true").withChild(module).render();
    return closeModal + updateModule;
}
```

The page must already contain `edit-errors`, `edit-modal-container`, and the distinct
`container-<id>` module wrapper. The save example assumes HTTP-layer CSRF validation and
request-owned module instances; error updates use OOB because the modal saves with
`hx-swap="none"`. `ResponseStatusException` and `HttpStatus` are Spring types.

## Request Mechanics

- `GET /modules/{id}/edit` returns modal HTML.
- User edits fields and submits.
- `POST /modules/{id}/save` validates and applies edits.
- Response uses OOB swaps to close modal and update content.

## Owner vs User Edit Mode

Use `EditMode.OWNER_EDIT` for direct updates.
Use `EditMode.USER_EDIT` when changes must be staged for approval.
