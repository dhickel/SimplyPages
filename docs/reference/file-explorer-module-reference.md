[Previous](components-and-modules-catalog.md) | [Index](../INDEX.md)

# File Explorer Module Reference

`FileExplorerModule` and `FilePickerModule` are reusable SSR/HTMX modules for app-supplied file explorer models.

## Package

- `io.mindspice.simplypages.modules.file`

## Types

- `FileExplorerModule`
- `FilePickerModule`
- `FileExplorerConfig`
- `FileExplorerState`
- `FileEntryView`
- `FileBreadcrumbItem`
- `FileExplorerEndpoints`
- `FileExplorerAction`
- `FileExplorerInspectorSpec`
- `FileExplorerMode`
- `FilePickerMode`

## Boundary and Responsibilities

- Framework provides rendering and HTMX attribute wiring only.
- Consumer app owns filesystem/persistence access, path validation, authz/authn, and CSRF policy.
- Consumer app supplies safe endpoint routes and enforces per-action permissions.

## Core Configuration

Use `FileExplorerConfig` as the stable API contract:

- Endpoint templates via `FileExplorerEndpoints`:
- `listEndpoint`
- `navigateEndpointTemplate`
- `viewerEndpointTemplate`
- `inspectorEndpointTemplate`
- `modalEndpointTemplate`
- `actionEndpointTemplate`
- `pickerSelectEndpointTemplate`
- Target ids:
- `rootId`
- `listTargetId`
- `inspectorTargetId`
- `viewerTargetId`
- `modalContainerId`
- `pickerCallbackTargetId`
- Mode and controls:
- `explorerMode` (`LIST` or `CARDS`)
- `pickerMode` (`FILES`, `DIRECTORIES`, `FILES_OR_DIRECTORIES`)
- allow flags: `allowCreateFolder`, `allowCreateText`, `allowCreateMarkdown`, `allowRename`, `allowDelete`, `allowCopyMove`, `allowTags`

`FileExplorerEndpoints` supports either:

- query mode (`/route` -> `/route?path=<encoded>`)
- placeholder mode (`/route/{path}` -> `/route/<encoded>`)

Path values are URL encoded by the module contract.

## Slots and Panes

- List pane uses `listTargetId`.
- Inspector pane uses `inspectorTargetId` and can be app-supplied via `state.inspectorContent()`.
- Viewer pane uses `viewerTargetId` and can be app-supplied via `state.viewerContent()`.
- Modal responses target `modalContainerId`.
- Picker callback responses target `pickerCallbackTargetId`.

## Picker Mode Behavior

- `FILES`: only file entries show/select successfully.
- `DIRECTORIES`: only directory entries show/select successfully.
- `FILES_OR_DIRECTORIES`: both are selectable.

## HTMX Behavior

- Entry open buttons call navigate/viewer templates by entry type.
- Inspector buttons call inspector endpoint template.
- Delete buttons call modal endpoint template when `allowDelete` is enabled.
- Consumer-defined entry actions can provide custom `hx-*` attributes using `FileExplorerAction`.

## Security Notes

- Treat all incoming `path` values as untrusted in your controller/service layer.
- Enforce authorization and path traversal protections in your app.
- Enforce CSRF and method restrictions for mutating endpoints.

## Stable Targets

Use `withPaneIds(root, list, inspector, viewer, modal)` to set deterministic HTMX target ids.
