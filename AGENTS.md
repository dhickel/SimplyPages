# Agent Notes - SimplyPages Editing System

## Scope
Summarizes core editing-system constraints plus Phase 1-8 commit-message history for later code review.

## Key Editing System Constraints
- Single modal container: `#edit-modal-container`; always render overlays via `Modal.create()` or `EditModalBuilder`.
- HTMX save/delete: use `Button.create()` (type=button), `hx-swap="none"`, and OOB swaps to clear modal + refresh `#page-content`.
- Module sizing is a layout concern: use grid columns (`Column.withWidth`); do not add width to module edit contracts.
- Escape user input with `HtmlUtils.htmlEscape()` before rendering.
- Each phase should have a dedicated `Phase{N}TestController` (or `Phase{N}And{N+1}`) for manual verification.

## Phase 1-8 Commit Message Digest (Chronological)
- `39771b5` Phase 2 docs update: added advanced/examples/quick-reference/troubleshooting docs; new examples (live dashboard, wiki-style editing); template system cheat sheet and troubleshooting guide.
- `3eab63c` Editing demo enhancement plan (phases 1-6): added width/position fields, PageData, save/load, add/edit modals with width presets, row insertion fix, compact edit buttons, and modal containers (`#add-module-modal`, `#edit-module-modal`) in demo; pre-dates single-container modal guidance.
- `a951062` Phase 1-2: Modal component + CSS; edit button size increase; EditAdapter + ValidationResult + EditModalBuilder; ContentModule implements EditAdapter.
- `9e4bd26` Phase 1-2 test page: `/test/phase1-2` verifying modal rendering, EditAdapter, EditModalBuilder, validation, HTMX.
- `161f90f` Plan doc update: created `EDITING_SYSTEM_PLAN.md`; documented Phase 2.5 bug fixes (button type, hx-include selector, OOB swaps); updated `CLAUDE.md`.
- `3efd5dd` Phase 3-4: auto-save architecture (remove save/load); add/insert row opens module modal; delete module auto-deletes empty rows.
- `192d1d1` Phase 3-4 test + modal overlay doc: `MODAL_OVERLAY_USAGE.md`; `/test/phase3-4`; formalized single modal container + OOB swap pattern.
- `fbceace` Phase 3-4 UX: width dropdown in edit modal (test controller); improved markdown label; edit/delete button styling tweaks; Phase 6.5 locking design added to plan.
- `52fbb0e` and `25fcf01` Phase 5: EditableModule wrapper + framework CSS buttons; fixes for ContentModule rebuild and EditableModule render/build idempotency; updates to controllers; Phase5TestController; consistent z-index + styling.
- `a19ec5f` Phase 6: editing system styling improvements (row/module spacing, add-module button, hover); Phase6TestController.
- `2edd48f` Row insert pattern only: removed "Add Row at Bottom" button, aligning with upcoming Phase 6.5 row locking.
- `4b11a4d` Phase 6.5: permission flags (canEdit, canDelete, canAddModule); Phase6_5TestController; docs updated.
- `093d204` Phase 6.5 follow-up: Phase6_5TestController rewritten with full CRUD endpoints and OOB swap patterns.
- `edf1722` + `415cb54` Phase 6.5 docs: updated plan, notes, API/overview with completion status and permission APIs.
- `bff114c` Phase 7a: new `Editable<T>` interface replacing EditAdapter; EditableChild wrapper; EditModalBuilder supports inline child edits; ContentModule + tests migrated; `STATE_MANAGEMENT_ANALYSIS.md`; Phase7aTestController.
- `67919cf` Phase 7b: Editable on core components (Image, Link, Paragraph, Header, ListItem); new RichContentModule/SimpleListModule; Phase7TestController.
- `9ebfd88` Phase 7 fix: Module base class gets `getModuleId()` and `getTitle()` to avoid reflection and fix Phase 7 bug.
- `8e5e0dc` Phase 8: AuthWrapper pattern (`require*` helpers); Phase8TestController with role-based access; docs updated.
- `37b6225` Post-Phase 8: added `EDITING_SYSTEM_GUIDE.md` as comprehensive implementation guide.

## Agent Review Context (Alpha Sprint)
Notes gathered from a framework-only review (exclude demo-only behavior unless explicitly referenced).

### Resolved/Verified Issues
- **Markdown Security**: `Markdown` component correctly escapes HTML by default. Verified via `MarkdownXssTest`.
- **EditableRow Modal Target**: Verified code uses `#edit-modal-container`.
- **EditableRow Column Widths**: Code implements logic for equal-width columns.
- **Component IDs**: Verified `Header`, `Paragraph`, `Image`, `Link`, `ListItem` correctly apply IDs to the DOM. Verified via `ComponentIdTest`.
- **RichContentModule Editing**: Implemented child editing (updates only) via `buildEditView` and `applyEdits`.

### Active Issues & Inconsistencies
- `EditModalBuilder` nested editing: `editChildUrl` is unused/missing. Adding new children requires a pattern not yet implemented in the core `EditModalBuilder`.
- `EditablePage` insert-row POST lacks row context and empty pages render no insert controls. `simplypages/src/main/java/io/mindspice/simplypages/editing/EditablePage.java`
- `EditableModule` delete lacks a default target (wrapper has no id). `simplypages/src/main/java/io/mindspice/simplypages/modules/EditableModule.java`
- Header alignment edits do not update CSS classes (needs verification, similar to ID issue which was false).
- `Modal` injects `modalId` directly into inline JS/HTML.
- Link validation only blocks `javascript:`.
- `EditAdapter` is deprecated but used extensively.

### Documentation Drift
- `docs/EDITING_SYSTEM.md` is legacy.
- `EDITING_SYSTEM_API.md` mentions unimplemented `withErrors`.

### Framework Behavior Reminders
- Modules are build-once; rebuild children on edit.
- `HtmlTag.withInnerText` escapes using OWASP.
