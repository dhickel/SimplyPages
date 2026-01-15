# Editing System Guide

This guide provides a comprehensive overview of the SimplyPages in-place editing system. It allows developers to make any component or module editable with minimal configuration, leveraging HTMX for a seamless user experience.

## Core Concepts

The system is built on a wrapper pattern, ensuring a clean separation of concerns between a component's content and its editing functionality.

### 1. `EditableModule` Wrapper

Any `Module` can be made editable by wrapping it in an `EditableModule`. This decorator adds the necessary UI controls (edit and delete buttons) and HTMX attributes.

**Example:**
```java
// 1. Create a standard module
ContentModule content = ContentModule.create()
    .withTitle("My Article")
    .withContent("This is some editable content.");

// 2. Wrap it to make it editable
EditableModule editable = EditableModule.wrap(content)
    .withModuleId("article-123")
    .withEditUrl("/api/articles/123/edit")
    .withDeleteUrl("/api/articles/123/delete");
```

- **`withModuleId`**: Assigns a unique DOM ID, which is crucial for HTMX targeting.
- **`withEditUrl`**: The endpoint that should return an HTML form for editing.
- **`withDeleteUrl`**: The endpoint to handle the deletion of the module.

### 2. The Modal and OOB Swaps

The editing framework relies on a **single modal container** and **Out-of-Band (OOB) swaps** for all operations.

- **Modal Container**: Your main application shell must include a single div: `<div id="edit-modal-container"></div>`. All editing forms are loaded into this container.
- **OOB Swaps**: When an edit is saved, the server should return two blocks of HTML:
    1.  The updated module content to be swapped back into the main page.
    2.  An empty modal container to close the form.

**Example Save Response:**
```html
<!-- 1. The updated module content -->
<div id="article-123" hx-swap-oob="true">
  <!-- ... new content of the module ... -->
</div>

<!-- 2. An empty container to clear the modal -->
<div id="edit-modal-container" hx-swap-oob="true"></div>
```
This pattern ensures the modal closes and the content updates in a single, atomic response.

### 3. `Editable<T>` Interface (Advanced)

For fine-grained control over the editing process, components can implement the `Editable<T>` interface. This allows for custom edit views and logic, which is used by more complex modules like `RichContentModule`. For most standard cases, `EditableModule.wrap()` is sufficient.

## Security

Permissions should always be checked on the server before rendering edit controls or processing updates. The `EditableModule` can be conditionally applied based on user roles.

```java
Module module = createMyModule();

if (currentUser.canEdit()) {
    return EditableModule.wrap(module)
        .withEditUrl(...)
        // ... configuration
        .render();
} else {
    return module.render();
}
```
