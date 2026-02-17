# Layout Package Agent Guide

## Purpose
Owns page composition and responsive layout primitives.

## Owns
- `Page`, `Row`, `Column`, `Grid`, `Container`, `Section`

## Invariants
- `Row` remains the row container abstraction.
- `Row.withChild` auto-wrap behavior for non-`Column` components stays predictable.
- `Column.withWidth(int)` accepts only 1..12.
- `Column.auto()` and `Column.fill()` semantics remain stable.

## Do
- Keep layout APIs fluent and simple.
- Validate width/shape constraints at API boundaries.
- Preserve backward-compatible CSS class semantics where possible.

## Do Not
- Move module-specific logic into layout classes.
- Add framework business logic to layout components.

## Common Pitfalls
- Overwriting `class` attributes and dropping prior classes.
- Changing row/column wrapping behavior without tests.
- Breaking mobile stacking assumptions in framework CSS alignment.

## Required Tests
- `RowTest`, `ColumnTest`, `PageTest`, `GridTest`, `ContainerTest`, `SectionTest`
- Regression coverage for width validation and class generation

## Dependencies
- Depend on `core` and generic components only.
- Do not introduce dependencies on `modules` or demo-specific code.

## Maintenance Requirement
Keep this file updated whenever layout behavior, constraints, or breakpoints change.

See root `AGENTS.md` for global standards.

## Documentation TOC (Terse)
- Full index: `docs/INDEX.md`
- Fundamentals: `docs/fundamentals/01-web-and-htmx-primer.md`, `docs/fundamentals/02-simplypages-mental-model.md`, `docs/fundamentals/03-css-fundamentals.md`
- Getting started: `docs/getting-started/README.md`, `docs/getting-started/01-installation-and-first-static-page.md`, `docs/getting-started/02-dynamic-pages-with-slotkey-rendercontext.md`, `docs/getting-started/03-editing-system-first-implementation.md`
- Core: `docs/core/01-components-htmltag-and-module-lifecycle.md`, `docs/core/02-layout-page-row-column-grid.md`, `docs/core/03-template-rendercontext-slotkey-reference.md`, `docs/core/04-rendering-pipeline-high-and-low-level.md`, `docs/core/05-css-defaults-overrides-and-structure.md`
- Patterns: `docs/patterns/01-static-page-serving-patterns.md`, `docs/patterns/02-dynamic-fragment-caching-patterns.md`, `docs/patterns/03-htmx-endpoint-and-swap-patterns.md`, `docs/patterns/04-editing-workflows-owner-user-approval.md`
- Security: `docs/security/01-security-boundaries-and-safe-rendering.md`, `docs/security/02-authwrapper-authorizationchecker-integration.md`
- Operations: `docs/operations/01-performance-threading-and-cache-lifecycles.md`, `docs/operations/02-testing-and-troubleshooting-playbook.md`
- Reference: `docs/reference/components-and-modules-catalog.md`, `docs/reference/builders-shell-nav-banner-accountbar.md`, `docs/reference/editing-api-reference.md`

## Documentation Sync Requirement
- Any API-surface change or major internal behavior change must trigger a docs review.
- Update affected docs in the same workstream when applicable (`README.md`, `docs/INDEX.md`, and related pages).
- If no docs update is needed, explicitly note why in the PR/commit/task summary.
