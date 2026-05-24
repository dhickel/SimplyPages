[Previous](components-and-modules-catalog.md) | [Index](../INDEX.md)

# File Explorer Module Reference

`FileExplorerModule` and `FilePickerModule` are reusable SSR/HTMX modules for app-supplied file explorer models.

## Package

- `io.mindspice.simplypages.modules.file`

## Types

- `FileExplorerModule`
- `FilePickerModule`
- `FileExplorerState`
- `FileEntryView`
- `FileBreadcrumbItem`
- `FileExplorerEndpoints`
- `FileExplorerAction`
- `FileExplorerInspectorSpec`

## Boundary

- No filesystem, persistence, or authorization is included.
- Consumers provide pre-shaped entries and secure endpoints.

## Stable Targets

Use `withPaneIds(root, list, inspector, viewer, modal)` to set deterministic HTMX target ids.
