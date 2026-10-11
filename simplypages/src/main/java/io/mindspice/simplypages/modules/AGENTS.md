# Modules Package Agent Guide

## Purpose
Owns high-level composed modules built from components/layout primitives.

## Owns
- Content/data/form/forum/media modules
- Chat module (`ChatModule`) for embeddable transcript/composer shells
- Dynamic modules and editable wrappers (`EditableModule`)
- Opinionated feature modules (hero, stats, timeline, tabs, accordion, quote, callout, comparison)

## Invariants
- Modules compose structure in `buildContent()`.
- Module lifecycle uses `build()` idempotently.
- Module direct width APIs are blocked; layout should size modules.
- Public fluent APIs remain chainable and readable.

- Tabs and accordions generate stable, unique per-instance fallback IDs when no module ID is supplied; explicit module IDs retain the existing target ID format.
- Tab triggers have type=button so embedding a tab module in a form does not submit it.
- Tabs emit reciprocal tab/panel ARIA relationships and a horizontal SSR orientation fallback (viewport unknown). Initially only the selected tab has tabindex=0; other tabs have tabindex=-1 and their panels have hidden. Keep selected/active/hidden state consistent during build.
- Framework JS synchronizes tablist orientation to computed `.tab-nav` flex direction on initialization, resize and dynamic insertion, without changing focus/selection/nested state. Horizontal uses Left/Right, vertical uses Up/Down (default through 768px); both wrap and automatically activate, with Home/End. Perpendicular arrows and modified keys retain native behavior. Cover real generated and packaged desktop/mobile output with `tests/browser/responsive-tabs.spec.js` and `default-accessibility.spec.js`.
- Framework JS owns tab activation, roving focus and keyboard handling after render. Preserve all tab/accordion CSS hooks and nearest `.tabs-container`/`.accordion` ownership; nested widgets must not alter an outer widget's state or target another owner.
- Accordion region names use aria-labelledby referencing stable generated header button IDs (`<base>-item-<index>-header`); preserve existing content IDs, expanded classes and single-expand configuration. Tall-content sizing and contrast are separate from this semantic markup contract.
- EditableModule edit buttons remain normal non-submit HTMX launchers with configurable target/swap. Modal focus/close/restoration belongs to framework JS, not per-wrapper scripts; preserve the launcher when focus should return after closing.

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
- Semantic tab/accordion regressions: initial selected/tabindex/hidden state, reciprocal relationships, named regions, stable unique default IDs, explicit IDs, nested ownership and existing expansion modes.
- Deliberately review affected module snapshots and rerun tests without update mode after accepting them.
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
- Patterns: `docs/patterns/01-static-page-serving-patterns.md`, `docs/patterns/02-dynamic-fragment-caching-patterns.md`, `docs/patterns/03-htmx-endpoint-and-swap-patterns.md`, `docs/patterns/04-editing-workflows-owner-user-approval.md`, `docs/patterns/05-forum-helper-implementation-and-customization.md`
- Security: `docs/security/01-security-boundaries-and-safe-rendering.md`, `docs/security/02-authwrapper-authorizationchecker-integration.md`
- Operations: `docs/operations/01-performance-threading-and-cache-lifecycles.md`, `docs/operations/02-testing-and-troubleshooting-playbook.md`, `docs/operations/03-writing-tests-for-components-and-modules.md`
- Reference: `docs/reference/components-and-modules-catalog.md`, `docs/reference/builders-shell-nav-banner-accountbar.md`, `docs/reference/forum-helper-api-reference.md`, `docs/reference/editing-api-reference.md`

## Documentation Sync Requirement
- Any API-surface change or major internal behavior change must trigger a docs review.
- Update affected docs in the same workstream when applicable (`README.md`, `docs/INDEX.md`, and related pages).
- If no docs update is needed, explicitly note why in the PR/commit/task summary.
