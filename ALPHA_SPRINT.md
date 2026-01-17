# Alpha Sprint Review

This document captures the Phase 1-3 and 6.5 framework review findings plus the Phase 7 rollback decisions. It is intended as a living document for ongoing changes and progress tracking.

## Status Legend

- Open: confirmed issue, not started
- In Progress: actively being addressed
- Needs Recheck: may have changed due to rollback; verify current behavior
- Resolved: fixed
- Removed: no longer applicable after rollback

## Decision: Phase 6.5 Baseline

- Revert the edit contract to `EditAdapter` (module-level only); remove `Editable` and component-level editing.
- Remove nested child-edit UI and Phase 7 demo controllers; keep `EditModalBuilder` focused on module edit views.
- Keep post-6.5 improvements that do not depend on nested editing.
- `Editable`/`EditableChild` removed; component edit UIs removed; module edit UIs remain via `EditAdapter`.

## Recent Changes (Rollback)

- `EditModalBuilder` simplified to module edit views only and uses OOB-friendly save/delete.
- Phase 7 nested editing findings removed from backlog; see status table for history.

## Findings Backlog (Functional/Security)

| ID | Severity | Area | Finding | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| F-1 | High | Security (Phase 1-3) | Markdown rendering returns unsanitized HTML; stored XSS risk. `simplypages/src/main/java/io/mindspice/simplypages/components/Markdown.java:14` | Resolved | Fixed: Added `escapeHtml(true)` default, `createUnsafe()` for trusted content. |
| F-2 | High | Bug (Phase 3) | Column widths only recomputed for newest module; total can exceed 12. `simplypages/src/main/java/io/mindspice/simplypages/editing/EditableRow.java:230` | Resolved | Fixed: Moved column building to render() - all modules now get equal width. |
| F-3 | High | Bug (Phase 6.5) | Delete defaults to swapping button, not module; wrapper has no id/target. `simplypages/src/main/java/io/mindspice/simplypages/modules/EditableModule.java:337` | Resolved | Fixed: Wrapper now has auto-generated id, delete targets wrapper by default. |
| F-4 | Medium | Security | `Modal` injects `modalId` directly into HTML/JS; XSS if untrusted ids. `simplypages/src/main/java/io/mindspice/simplypages/components/display/Modal.java:57` | Resolved | Fixed: Added regex validation for modal IDs (alphanumeric, hyphens, underscores only). |
| F-5 | Medium | Bug | Row insertion has no row context; `/rows/insert` ambiguous. `simplypages/src/main/java/io/mindspice/simplypages/editing/EditablePage.java:174` | Resolved | Fixed: Insert URL now includes `?position=N` query parameter. |
| F-6 | Medium | Bug | Empty pages render no insert controls. `simplypages/src/main/java/io/mindspice/simplypages/editing/EditablePage.java:159` | Resolved | Fixed: Empty pages now render "Add First Row" button with position=0. |
| F-7 | Medium | Bug | `withEditMode` stored but never applied. `simplypages/src/main/java/io/mindspice/simplypages/editing/EditableRow.java:110` | Resolved | Fixed: Added withEditMode() to EditableModule, applied in EditableRow.render(). |
| F-8 | Medium | Bug/UX | Add-module button targets `#add-module-modal` not single container. `simplypages/src/main/java/io/mindspice/simplypages/editing/EditableRow.java:266` | Resolved | Fixed: Changed target to `#edit-modal-container` for consistency. |
| F-9 | Medium | Bug | Paragraph alignment appends classes on every render. `simplypages/src/main/java/io/mindspice/simplypages/components/Paragraph.java:129` | Resolved | Fixed: Made alignment application idempotent with `alignmentApplied` flag. |
| F-10 | Medium | Security | Link validation only blocks `javascript:`; other schemes allowed. `simplypages/src/main/java/io/mindspice/simplypages/components/navigation/Link.java:132` | Resolved | Fixed: Now blocks `javascript:`, `vbscript:`, and `data:` schemes. |
| F-11 | Low | Bug | Component ids stored but not applied to DOM `id` attributes (limits targeting). `simplypages/src/main/java/io/mindspice/simplypages/components/Header.java:85`, `.../Paragraph.java:87`, `.../Image.java:82`, `.../Link.java:56`, `.../ListItem.java:45` | Resolved | Fixed: Removed auto-generated UUIDs; withId() now applies to DOM when set. |
| F-12 | High | Architecture (Phase 7) | Nested child editing wiring (editChildUrl/single save mapping). | Removed | Removed with rollback to EditAdapter. |
| F-13 | Medium | Architecture (Phase 7) | Add-child UI posts `text` only but expects `src/alt/href`. | Removed | Removed with rollback to EditAdapter. |
| F-14 | Medium | Architecture (Phase 7) | No metadata contract for child identity/labels. | Removed | Removed with rollback to EditAdapter. |
| F-15 | Medium | Bug (Phase 7) | Header alignment edits do not update CSS classes. | Removed | Component edit UI removed. |
| F-16 | Low | Bug (Phase 7) | `Header.HeaderLevel.valueOf` throws on invalid add-child input. | Removed | Add-child flow removed. |
| F-17 | Low | Bug/Consistency | `EditModalBuilder` delete used main swap; diverged from OOB. | Resolved | Builder now module-only with OOB-style save/delete. |

## Documentation Backlog

| ID | Severity | Finding | Status | Notes |
| --- | --- | --- | --- | --- |
| D-1 | Medium | `withErrors` documented but not implemented. `EDITING_SYSTEM_API.md:277` | Open | Update docs or implement method. |
| D-2 | Medium | `docs/EDITING_SYSTEM.md` teaches multiple modal containers and non-OOB swap patterns. `docs/EDITING_SYSTEM.md:203`, `docs/EDITING_SYSTEM.md:409`, `docs/EDITING_SYSTEM.md:1099` | Open | Legacy doc; consider archival or rewrite. |
| D-3 | Medium | Phase 7 analysis docs reference `Editable`/nested editing. `STATE_MANAGEMENT_ANALYSIS.md`, `DEVELOPMENT_NOTES.md` | Resolved | Marked historical after rollback. |

## Phase 6.5-Aligned Modal and Editing Usage

- Use exactly one modal container: `<div id="edit-modal-container"></div>` at the end of the page; wrap page content in `<div id="page-content">...</div>`.
- Edit buttons should `hx-get` into `#edit-modal-container` with `hx-swap="innerHTML"` (EditableModule default).
- Save/delete should use `Button.create()` and `hx-swap="none"`, and endpoints should return OOB swaps to clear `#edit-modal-container` and refresh `#page-content`.
- For add-module/insert-row, open a modal into `#edit-modal-container`, then return OOB swaps on submit to refresh `#page-content`.
- Enforce permissions server-side (e.g., `AuthWrapper`/`AuthorizationChecker`) even when UI flags hide buttons.

## Open Questions

- ~~Is Markdown expected to allow raw HTML, or should it always be sanitized?~~ **Resolved**: Default is sanitized (`escapeHtml(true)`). Use `Markdown.createUnsafe()` for trusted content.
- Should `EditableRow` own column width recalculation or should callers manage layout explicitly? **Resolved**: EditableRow now calculates equal widths at render time for all modules.

## Testing Gaps

- ~~Column resizing behavior in `EditableRow`~~ **Fixed**: Columns built at render() time with equal widths
- ~~Delete targeting behavior in `EditableModule`~~ **Fixed**: Wrapper has auto-generated id, delete targets wrapper by default
- ~~Row insertion behavior for empty pages and row context~~ **Fixed**: Empty pages show "Add First Row", position param added to URLs

**Remaining testing needed:**
- Verify editMode is correctly appended to edit/delete URLs
- Test modal ID validation with edge cases
- Verify Link URL scheme blocking works correctly
