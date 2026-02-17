# Display Components Agent Guide

## Purpose
Owns display-oriented UI components and visual containers.

## Owns
- `Alert`, `Badge`, `Card`, `CardGrid`, `DataTable`, `InfoBox`, `Label`, `Modal`
- `OrderedList`, `UnorderedList`, `ProgressBar`, `Spinner`, `Table`, `Tag`

## Invariants
- Display components remain composable and class-driven.
- `DataTable` and `Table` rendering contracts remain stable.
- Modal behavior and IDs remain predictable/safe.

## Do
- Keep display APIs concise with sensible presets.
- Preserve semantic HTML output and class naming stability.
- Validate user-controlled identifiers where used in JS hooks.

## Do Not
- Couple display components to application routing/state.
- Assume trusted content unless explicitly designated as trusted.

## Common Pitfalls
- Breaking CSS contract class names used by framework styles.
- Changing modal container behavior without integration tests.
- Regressing table/list rendering order.

## Required Tests
- `MarkdownSecurityTest`, `MarkdownTest`
- `Modal`-related and display component tests in `.../components`
- New behavior needs focused render assertions

## Dependencies
- Depends on `core` and base components.
- Keep independent from demo-only concerns.

## Maintenance Requirement
Keep this file updated whenever display component contracts, classes, or modal behavior change.

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
