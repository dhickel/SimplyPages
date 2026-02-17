# Modules Package Agent Guide

## Purpose
Owns high-level composed modules built from components/layout primitives.

## Owns
- Content/data/form/forum/media modules
- Dynamic modules and editable wrappers (`EditableModule`)
- Opinionated feature modules (hero, stats, timeline, tabs, accordion, quote, callout, comparison)

## Invariants
- Modules compose structure in `buildContent()`.
- Module lifecycle uses `build()` idempotently.
- Module direct width APIs are blocked; layout should size modules.
- Public fluent APIs remain chainable and readable.

## Do
- Keep each module focused on one UI concern.
- Compose existing components instead of custom HTML assembly where possible.
- Expose sensible defaults and explicit configuration methods.

## Do Not
- Re-introduce mutable render-time structure hacks.
- Duplicate core escaping behavior inside module code unless necessary.

## Common Pitfalls
- Mutating children post-build without rebuild semantics.
- Assuming width methods on modules are supported.
- Creating tight coupling to demo controllers/routes.

## Required Tests
- Module tests in `simplypages/src/test/java/.../modules`
- Lifecycle regressions (`ModuleLifecycleTest`)
- Editing wrapper regressions when changing `EditableModule`

## Dependencies
- Depend on `core`, `components`, `layout`, and optional `editing` helpers.
- Avoid depending on demo package code.

## Maintenance Requirement
Keep this file updated whenever module lifecycle, API surface, or module catalog changes.

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
