# Demo Pages Agent Guide

## Purpose
Owns page composition classes used by demo controllers.

## Owns
- Demo page builders/compositions under `demo.pages`

## Invariants
- Pages are examples of framework usage, not framework internals.
- Page classes should stay readable and educational.
- Compositions should reflect current recommended APIs.

## Do
- Keep page classes focused on rendering/composition.
- Prefer reusable helper patterns when examples repeat.
- Update examples when APIs evolve.

## Do Not
- Embed business/domain persistence logic in page classes.
- Depend on unstable, deprecated framework APIs without explicit note.

## Common Pitfalls
- Example code lagging behind module/component API changes.
- Overly complex page classes that hide framework concepts.
- Mixing unrelated concerns in one demo page.

## Required Tests
- Add/adjust demo integration coverage when page behavior changes materially
- Ensure key routes still render after API migrations

## Dependencies
- Depends on framework APIs and demo controllers.
- Keep direction one-way: framework should not depend on demo pages.

## Maintenance Requirement
Keep this file updated whenever demo page composition patterns or API usage changes.

See root `AGENTS.md` for global standards.

Update this file in the same change whenever package-level behavior or conventions drift.

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
