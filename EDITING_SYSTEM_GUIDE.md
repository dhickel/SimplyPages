# Editing System Guide

> **Current Version**: Phase 2 Implementation
> **Related**: [MODAL_OVERLAY_USAGE.md](MODAL_OVERLAY_USAGE.md) for modal patterns.

The SimplyPages editing system enables in-place content editing, page building, and dynamic module management using HTMX. It follows a wrapper/decorator pattern to add editing capabilities to any module without modifying the original module code.

## Architecture

The system uses a "Decorator" pattern where standard `Module` components are wrapped in an `EditAdapter` (or `EditableModule` wrapper) that adds:
1. **Edit Toolbar**: Edit/Delete buttons
2. **HTMX Attributes**: Wiring for dynamic updates
3. **Visual State**: Indicators for "Edit Mode"

### Core Components

- **`EditAdapter`**: Interface for adapting modules for editing.
- **`EditModalBuilder`**: Standardized builder for edit forms in modals.
- **`ValidationResult`**: Server-side validation framework.
- **`EditableRow` / `EditablePage`**: Wrappers for layout management.

## Integration Steps

### 1. Wrap a Module for Editing

```java
// 1. Create standard module
ContentModule module = ContentModule.create()
    .withTitle("My Content")
    .withContent("Variable content...");

// 2. Wrap if user has permission
if (userCanEdit) {
    // Note: Implementation detail - use the wrapper helper or adapter
    // See EditableModule.wrap(module) pattern
    return EditableModule.wrap(module)
        .withModuleId("mod-123")
        .withEditUrl("/api/modules/mod-123/edit")
        .withDeleteUrl("/api/modules/mod-123/delete")
        .render();
}

return module.render();
```

### 2. Create the Edit Form (Controller)

Endpoints usually return a `Modal` containing the form.

```java
@GetMapping("/api/modules/{id}/edit")
public String editForm(@PathVariable String id) {
    // 1. Fetch data
    var data = service.get(id);

    // 2. Build form fields
    var titleField = TextInput.create("title", data.getTitle());
    var contentField = TextArea.create("content", data.getContent());

    // 3. Use EditModalBuilder
    return EditModalBuilder.create()
        .withTitle("Edit Module")
        .withSaveUrl("/api/modules/" + id) // POST
        .withTargetId("#module-" + id)     // Update original module
        .addField("Title", titleField)
        .addField("Content", contentField)
        .render();
}
```

### 3. Handle Updates (Controller)

```java
@PostMapping("/api/modules/{id}")
public String update(@PathVariable String id, @RequestParam Map<String, String> formData) {
    // 1. Validate
    if (invalid) return Alert.danger("Error").render();

    // 2. Save
    service.save(id, formData);

    // 3. Return Updated Module (HTML)
    // The swapped HTML replaces the *old* module in the page
    return service.renderModule(id);
}
```

## Z-Index & Overlays

The editing system relies heavily on Modals.
**Critical**: Ensure `framework.css` z-index values are correct so modals appear *above* sidebars.

- **Sidebar**: 1050
- **Modal**: 1110

See [MODAL_OVERLAY_USAGE.md](MODAL_OVERLAY_USAGE.md) for details.

## Current Limitations (Phase 2)

- **Deep Nesting**: Editing nested structures (like a grid inside a card) is experimental.
- **Drag & Drop**: Layout re-ordering is not yet implemented.
- **Rich Text**: Markdown is supported, but WYSIWYG editors are not yet integrated.

## Authorization

Always check permissions *before* rendering edit controls and *before* processing updates.

```java
@PreAuthorize("@auth.canEdit(#id, principal)")
@PostMapping("/update")
...
```
