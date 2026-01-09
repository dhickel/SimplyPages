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

### Gotchas and Inconsistencies
- `EditableRow` column widths are only calculated for the newest module; existing columns are not resized, so total width can exceed 12 columns. `simplypages/src/main/java/io/mindspice/simplypages/editing/EditableRow.java`
- `EditableRow` uses `#add-module-modal`, which conflicts with the single modal container rule (`#edit-modal-container`). `simplypages/src/main/java/io/mindspice/simplypages/editing/EditableRow.java`
- `EditablePage` insert-row POST lacks row context and empty pages render no insert controls, so insertion is ambiguous or impossible. `simplypages/src/main/java/io/mindspice/simplypages/editing/EditablePage.java`
- `EditableModule` delete lacks a default target (wrapper has no id), so HTMX swaps the button itself unless callers set a target. `simplypages/src/main/java/io/mindspice/simplypages/modules/EditableModule.java`
- `EditModalBuilder` delete uses main swap (`outerHTML`) instead of OOB-only, diverging from the Phase 6.5 OOB guidance. `simplypages/src/main/java/io/mindspice/simplypages/editing/EditModalBuilder.java`
- Header alignment edits do not update CSS classes; paragraph alignment appends classes every render (duplicates). `simplypages/src/main/java/io/mindspice/simplypages/components/Header.java`, `simplypages/src/main/java/io/mindspice/simplypages/components/Paragraph.java`
- `Modal` injects `modalId` directly into inline JS/HTML; only pass trusted ids. `simplypages/src/main/java/io/mindspice/simplypages/components/display/Modal.java`
- `EditAdapter` is deprecated but still appears in docs; the framework has moved to `Editable`. `simplypages/src/main/java/io/mindspice/simplypages/editing/EditAdapter.java`

### Documentation Drift
- `docs/EDITING_SYSTEM.md` is legacy and contradicts single-modal/OOB patterns; prefer `EDITING_SYSTEM_GUIDE.md` and `MODAL_OVERLAY_USAGE.md`.
- `EDITING_SYSTEM_API.md` still documents `EditAdapter` as primary and mentions `withErrors` (not implemented).

### Framework Behavior Reminders
- Modules are build-once and not thread-safe; editing must clear children and rebuild (`children.clear(); buildContent();`) after apply edits or child changes.
- `HtmlTag.withInnerText` escapes using OWASP; `withUnsafeHtml` bypasses escaping and should never be used on user content.

### Additional Context for Agents
- `EditablePage` and `EditableRow` override `render()` to build new wrapper `Div`s; attributes set on the instance are not rendered.
- `EditModalBuilder` only shows child editing if `withEditable(...)` is used (setting `withEditView(...)` alone does not populate `editable`).
- `EditModalBuilder` defaults to `pageContainerId="page-container"` while most docs/demos use `page-content`; mismatch breaks OOB swaps unless overridden.
- `ValidationResult` docs mention `addError`, but the method is not implemented; use `invalid(...)` or `invalid(List)` instead.
- `HtmlTag.withAttribute` replaces existing attributes of the same name; some components override `withClass` to replace instead of append (class handling is inconsistent).
- `Modal.closeOnBackdrop(false)` still allows ESC key to close; there is no built-in way to disable ESC close.
- `EditableModule` is idempotent after first render; set edit/delete config before rendering.
