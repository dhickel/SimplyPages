[Previous](02-dynamic-pages-with-slotkey-rendercontext.md) | [Index](../INDEX.md)

# Editing System First Implementation

This guide shows a minimal, production-shaped editing flow.

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
public String saveModule(@PathVariable String id, @RequestParam Map<String, String> formData) {
    ContentModule module = moduleService.loadContentModule(id);

    ValidationResult vr = module.validate(formData);
    if (!vr.isValid()) {
        return Alert.danger(String.join(", ", vr.errors())).render();
    }

    module.applyEdits(formData);
    moduleService.save(module);

    String closeModal = "<div id=\"edit-modal-container\" hx-swap-oob=\"true\"></div>";
    String updateModule = "<div id=\"" + id + "\" hx-swap-oob=\"true\">" + module.render() + "</div>";
    return closeModal + updateModule;
}
```

## Request Mechanics

- `GET /modules/{id}/edit` returns modal HTML.
- User edits fields and submits.
- `POST /modules/{id}/save` validates and applies edits.
- Response uses OOB swaps to close modal and update content.

## Owner vs User Edit Mode

Use `EditMode.OWNER_EDIT` for direct updates.
Use `EditMode.USER_EDIT` when changes must be staged for approval.
