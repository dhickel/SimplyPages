# Navigation Components Agent Guide

## Purpose
Owns navigation primitives and reusable nav structures.

## Owns
- `Link`, `NavBar`, `SideNav`, `Breadcrumb`

## Invariants
- Navigation components remain semantic and link-safe.
- Active/selected state APIs produce stable class and attribute output.
- Side navigation structure remains builder-friendly.

## Do
- Keep navigation APIs concise and predictable.
- Preserve compatibility with builders relying on nav components.
- Add tests for active state rendering and link attributes.

## Do Not
- Embed app route policies in component layer.
- Break class hooks used by framework CSS/builders.

## Common Pitfalls
- Inconsistent active class naming.
- Regressing nested nav/section structures.
- Generating malformed URLs/attributes.

## Required Tests
- `LinkTest`, `NavBarTest`, `SideNavTest`, `BreadcrumbTest`
- Regression tests for new nav states/options

## Dependencies
- Depends on `core` and base components.
- Keep no dependency on demo logic.

## Maintenance Requirement
Keep this file updated whenever navigation contracts or class/state behavior changes.

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
