[Previous](builders-shell-nav-banner-accountbar.md) | [Index](../INDEX.md)

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
