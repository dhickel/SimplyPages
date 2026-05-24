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
- action placeholder mode (`/modal/{action}` or `/action/{action}/{path}`)

Query values use normal query encoding. `{path}` placeholder values use URI path-component percent encoding, so spaces render as `%20` instead of form-style `+`.

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

- Toolbar refresh calls `listEndpoint` and targets `listTargetId`.
- Toolbar create buttons call `modalEndpointTemplate` with actions `create-folder`, `create-text`, and `create-markdown` when their allow flags are enabled.
- Entry open buttons call navigate/viewer templates by entry type.
- Inspector buttons call inspector endpoint template.
- Rename and delete buttons call `modalEndpointTemplate` with actions `rename` and `delete` when their allow flags are enabled.
- Copy and move buttons call `actionEndpointTemplate` with actions `copy` and `move` when `allowCopyMove` is enabled.
- Consumer-defined entry actions can provide custom `hx-*` attributes using `FileExplorerAction`.

The module only renders the controls and HTMX attributes. Apps decide whether a modal is one-step, two-step, read-only, or rejected based on the selected entry and server-side policy.

## Security Notes

- Treat all incoming `path` values as untrusted in your controller/service layer.
- Enforce authorization and path traversal protections in your app.
- Enforce CSRF and method restrictions for mutating endpoints.

## Stable Targets

Use `withPaneIds(root, list, inspector, viewer, modal)` to set deterministic HTMX target ids.
