[Previous](content-helper-api-reference.md) | [Index](../INDEX.md)

# Editing API Reference

## Editable<T>

Contract for editable modules.

Required methods:

- `Component buildEditView()`
- `T applyEdits(Map<String, String> formData)`

Optional methods:

- `ValidationResult validate(Map<String, String> formData)`
- `List<EditableChild> getEditableChildren()`

## EditMode

- `OWNER_EDIT`: direct application
- `USER_EDIT`: staged/approval-oriented flow

## EditModalBuilder

Primary builder for edit modal UI.

Common methods:

- `withTitle(...)`
- `withModuleId(...)`
- `withEditable(...)` or `withEditView(...)`
- `withSaveUrl(...)`
- `withDeleteUrl(...)`
- `withPageContainerId(...)`
- `withModalContainerId(...)`
- `hideDelete()`

The returned `Modal` disables backdrop closing; Escape and the header close button remain
enabled. The footer Close button clears the configured modal container. Load the framework
CSS/JS once in the page shell for focus entry, Tab containment and connected-launcher
restoration, including external HTMX/OOB removal. Save uses `hx-swap="none"`; applications
must return OOB updates to clear the modal and update content. If an update also replaces
the launcher, the old disconnected element cannot receive focus.

See the [Modal rendering and interaction contract](components-and-modules-catalog.md#modal)
and [editing flow guide](../how-to/edit-content.md).

## EditableRow

`EditableRow.wrap(Row row, String rowId, String pageId)` creates an outer
`div.editable-row-wrapper` with id `row-{rowId}`. Its content renders in this order:

1. The supplied `Row`, unchanged, with its own id, classes, attributes, text and children.
2. A **separate** row containing modules registered through `addEditableModule(module, moduleId)`.
3. The add-module control, when adding is permitted and registration capacity remains.

The supplied row and registered modules are independent content, not a single editable grid.
The wrapper does not merge, deduplicate or move source children into editable columns.
**Callers must avoid registering content already present in the supplied row.** Outer wrapper
attributes do not replace or propagate to the supplied row. An empty source row is retained;
`null` omits only that source row, not the generated row or eligible control.

Each wrapper render passes the same `RenderContext` to the supplied row exactly once,
including a custom `render(RenderContext)` override. Rendering through `Template` retains
this dispatch and resolves changing context values rather than flattening the source renderer.
The wrapper does not mutate the supplied row's structure; custom renderers and descendants
remain responsible for their own behavior. `EditableRow` itself is mutable and not thread-safe,
including when retained by a template.

Configuration methods:

- `addEditableModule(module, moduleId)`: appends a registration in order and assigns that id
  to the underlying module. It throws `IllegalStateException` if capacity is reached.
- `withMaxModules(max)`: sets registration capacity (default `3`, minimum `1`). Source
  children do not count toward this capacity.
- `withEditMode(mode)`: sets the generated module controls' edit mode (default `OWNER_EDIT`).
- `withCanAddModule(canAdd)`: permits or hides the add control (default `true`); it does not
  gate the registered modules' edit/delete controls or enforce server authorization.

Generated columns use integer width `12 / registeredModuleCount`; counts above twelve use
fill columns. Source columns retain their configured widths. Generated endpoint conventions are:

- Edit: `/api/pages/{pageId}/modules/{moduleId}/edit`
- Delete: `/api/pages/{pageId}/modules/{moduleId}/delete`
- Add form: `/api/pages/{pageId}/rows/{rowId}/add-module-form`

The edit mode is appended to edit/delete URLs as `editMode`. Edit and add controls target
`#edit-modal-container` with `innerHTML`; delete targets its generated module wrapper with
`outerHTML`. Applications supply these endpoints and enforce authorization, CSRF and business
validation. Wrapper controls alone do not grant permission.

## ModuleEditHandler<T>

Service-layer contract for edit, update, and delete handlers.

- `renderEditForm(String moduleId)`
- `handleUpdate(String moduleId, Map<String, String> editData, EditMode editMode)`
- `handleDelete(String moduleId, EditMode editMode)`

## AuthWrapper

Authorization wrappers:

- `require(...)`
- `requireForEdit(...)`
- `requireForDelete(...)`
- `requireForCreate(...)`

## AuthorizationChecker

App-owned permission contract:

- `canEdit(String moduleId, String userId)`
- `canDelete(String moduleId, String userId)`
- `getEditMode(String moduleId, String userId)`
