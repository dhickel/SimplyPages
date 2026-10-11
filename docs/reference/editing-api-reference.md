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
