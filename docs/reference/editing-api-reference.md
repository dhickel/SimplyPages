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

`ValidationResult` defensively copies error lists and returns immutable error collections.

## EditMode

- `OWNER_EDIT`: direct application
- `USER_EDIT`: staged/approval-oriented flow

## EditModalBuilder

Primary builder for edit modal UI.

Common methods:

- `withTitle(...)`
- `withModuleId(...)` for safe path-segment identifiers
- `withEditable(...)` or `withEditView(...)`
- `withSaveUrl(...)`
- `withDeleteUrl(...)`
- `withChildEditUrl(...)`
- `withChildDeleteUrl(...)`
- `withPageContainerId(...)`
- `withModalContainerId(...)`
- `hideDelete()`

Child URL templates replace `{id}` with a URL-encoded child identifier. Container IDs and module
IDs are validated before rendering generated HTMX attributes.

## EditablePage and EditableRow

- `EditablePage.create(pageId)` requires a safe path-segment page ID.
- `EditableRow.wrap(row, rowId, pageId)` requires safe path-segment row and page IDs.
- `EditableRow.addEditableModule(module, moduleId)` requires a safe path-segment module ID.
- `EditableRow` renders from a shallow copy of the wrapped `Row`, preserving configured row
  attributes and existing children while avoiding repeated render-time mutation of the wrapped row.

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
