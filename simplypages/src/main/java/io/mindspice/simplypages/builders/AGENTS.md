# Builders Package Agent Guide

## Purpose
Owns higher-level shell and navigation builder utilities.

## Owns
- `ShellBuilder`, `BannerBuilder`
- `AccountBarBuilder`, `TopNavBuilder`, `SideNavBuilder`

## Invariants
- Builders remain fluent and simple to compose.
- `build()` output shape stays stable for consumers.
- Default values keep generated output functional.

## Do
- Keep generated markup predictable and integration-friendly.
- Validate required fields where absence would break output.
- Preserve backward-compatible defaults when adding options.

## Do Not
- Push unrelated business logic into builders.
- Depend on demo routes unless explicitly demo-only and documented.

## Common Pitfalls
- Breaking CSS class hooks used by framework styles.
- Changing default IDs/targets unexpectedly.
- Regressing HTMX bootstrap behavior in shell output.

## Required Tests
- `ShellBuilderTest`, `BannerBuilderTest`
- `TopNavBuilderTest`, `SideNavBuilderTest`, `AccountBarBuilderTest`

## Dependencies
- Depends on `core`, `components`, and `layout` only as needed.
- Keep builders reusable outside demo app.

## Maintenance Requirement
Keep this file updated whenever builder defaults, markup contracts, or integration hooks change.

See root `AGENTS.md` for global standards.

## Documentation TOC (Terse)
- Full index: `docs/INDEX.md`
- Fundamentals: `docs/fundamentals/01-web-and-htmx-primer.md`, `docs/fundamentals/02-simplypages-mental-model.md`, `docs/fundamentals/03-css-fundamentals.md`
- Getting started: `docs/getting-started/README.md`, `docs/getting-started/01-installation-and-first-static-page.md`, `docs/getting-started/02-dynamic-pages-with-slotkey-rendercontext.md`, `docs/getting-started/03-editing-system-first-implementation.md`
- Core: `docs/core/01-components-htmltag-and-module-lifecycle.md`, `docs/core/02-layout-page-row-column-grid.md`, `docs/core/03-template-rendercontext-slotkey-reference.md`, `docs/core/04-rendering-pipeline-high-and-low-level.md`, `docs/core/05-css-defaults-overrides-and-structure.md`, `docs/core/06-shell-project-structure-and-asset-load-chain.md`
- Patterns: `docs/patterns/01-static-page-serving-patterns.md`, `docs/patterns/02-dynamic-fragment-caching-patterns.md`, `docs/patterns/03-htmx-endpoint-and-swap-patterns.md`, `docs/patterns/04-editing-workflows-owner-user-approval.md`
- Security: `docs/security/01-security-boundaries-and-safe-rendering.md`, `docs/security/02-authwrapper-authorizationchecker-integration.md`
- Operations: `docs/operations/01-performance-threading-and-cache-lifecycles.md`, `docs/operations/02-testing-and-troubleshooting-playbook.md`
- Reference: `docs/reference/components-and-modules-catalog.md`, `docs/reference/builders-shell-nav-banner-accountbar.md`, `docs/reference/editing-api-reference.md`

## Documentation Sync Requirement
- Any API-surface change or major internal behavior change must trigger a docs review.
- Update affected docs in the same workstream when applicable (`README.md`, `docs/INDEX.md`, and related pages).
- If no docs update is needed, explicitly note why in the PR/commit/task summary.
