[Previous](03-htmx-endpoint-and-swap-patterns.md) | [Index](../INDEX.md)

# Editing Workflows: Owner, User, Approval

SimplyPages editing APIs support direct edits and staged edits.

## Core Building Blocks

- `Editable<T>` for module edit contracts.
- `EditModalBuilder` for consistent modal composition.
- `EditMode` for owner vs user behavior.
- `AuthWrapper` and `AuthorizationChecker` for access control.

## Owner Edit Workflow

1. Open edit modal.
2. Validate input.
3. Apply edits directly.
4. Persist changes.
5. Return OOB swaps.

## User Edit Workflow (Staged)

1. Open edit modal.
2. Validate input.
3. Store change request in approval queue.
4. Return pending status UI.
5. Owner approves or rejects.

## Minimal Handler Shape

```java
public interface ModuleEditHandler<T> {
    Component renderEditForm(String moduleId);
    Component handleUpdate(String moduleId, Map<String, String> editData, EditMode editMode);
    Component handleDelete(String moduleId, EditMode editMode);
}
```

## Request Mechanics

- Edit open: `GET /modules/{id}/edit`
- Save: `POST /modules/{id}/save?editMode=OWNER_EDIT|USER_EDIT`
- Delete: `DELETE /modules/{id}/delete?editMode=...`
- Nested child edit: `GET /modules/{moduleId}/children/{childId}/edit`
- Nested child save/delete: `POST` or `DELETE` to the matching child endpoint

## Operational Rules

1. Treat every edit endpoint as auth-protected.
2. Validate server-side before `applyEdits`.
3. Keep audit trail for staged edits.
4. Enforce authorization in the controller/service handler, not only by hiding buttons in the UI.
5. Escape at render time. Store validated plain text from forms unless the application owns a
   sanitization pipeline for trusted HTML.
6. Keep path identifiers constrained to safe path segments and URL-encode child identifiers when
   expanding child endpoint templates.
7. Return explicit `403`, `404`, or `400` responses for blocked permissions, missing records, and
   invalid positions or IDs.
