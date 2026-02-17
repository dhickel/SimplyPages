# Components Package Agent Guide

## Purpose
Owns shared low-level UI components and base component conventions.

## Owns
- Generic components in this package (`Div`, `Header`, `Paragraph`, `Markdown`, etc.)
- Cross-cutting component behavior not specific to subpackages

## Invariants
- Components follow `HtmlTag` extension patterns consistently.
- `create()` factory methods and fluent return types stay consistent.
- Attribute and text rendering remains safely escaped by default.

## Do
- Keep component APIs minimal and composable.
- Reuse `HtmlTag` capabilities before introducing custom render logic.
- Add targeted component tests for new behavior.

## Do Not
- Duplicate functionality already covered in subpackages.
- Embed app-specific routing/business concerns into components.

## Common Pitfalls
- Returning wrong fluent type and breaking method chaining.
- Overriding `render()` without preserving core safety semantics.
- Inconsistent class naming versus framework CSS.

## Required Tests
- Component tests under `simplypages/src/test/java/.../components`
- Security-related markdown/raw HTML tests where applicable

## Dependencies
- Depend on `core`; avoid dependencies on `modules`.
- Subpackages hold domain-specific component families.

## Maintenance Requirement
Keep this file updated whenever shared component conventions or base APIs change.

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
