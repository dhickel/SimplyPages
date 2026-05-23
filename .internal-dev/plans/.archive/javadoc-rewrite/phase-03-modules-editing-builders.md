# Phase 03 - Modules, Editing, and Builders

## Context
These packages define high-level composition behavior, editing workflows, and shell/navigation assembly. They contain lifecycle-heavy and integration-sensitive contracts.

## Goal
Rewrite Javadocs so module lifecycle, editing boundaries, and builder output contracts are explicit, terse, and safe for contributors/agents.

## In Scope
- `simplypages/src/main/java/io/mindspice/simplypages/modules`
- `simplypages/src/main/java/io/mindspice/simplypages/editing`
- `simplypages/src/main/java/io/mindspice/simplypages/builders`
- All symbol visibilities (including private/internal helpers).

## Out of Scope
- Layout/components/core rewrites beyond cross-reference cleanup.
- Implementation changes to editing/auth/business logic.

## Implementation Steps
1. Rewrite module docs around build-once lifecycle, render-time dynamics, and mutation windows.
2. Document decorators/wrappers and composition assumptions (including edit/delete wrappers).
3. Rewrite editing docs to clarify trust boundaries and responsibility split:
- framework rendering behavior,
- application-level auth/authorization/CSRF/validation.
4. Rewrite builder docs for output-shape guarantees, defaults, and configuration side effects.
5. Ensure mutable APIs explain why mutable and how to use safely.
6. Clarify deprecated/compatibility surfaces where present.
7. Use examples only where needed for constrained flows (e.g., modal/HTMX editing boundaries).

## Validation
- Rubric audit across modules/editing/builders.
- Focused review of high-risk classes (editable wrappers, auth wrappers, shell/edit builders).
- Javadoc generation validation for link integrity and tag correctness.

## Exit Criteria
- Composition-layer contracts are explicit and consistent.
- Editing safety boundaries and lifecycle expectations are clear.
- All symbols in these packages are rewrite-complete and rubric-compliant.

## Assumptions and Defaults
- Documentation-only phase; no behavior changes.
- Private state/method docs are included where they affect maintainability.
- Terminology stays aligned with `SimplyPages` naming.
