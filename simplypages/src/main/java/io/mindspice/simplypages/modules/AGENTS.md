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
- Accordion region names use aria-labelledby referencing stable generated header button IDs (`<base>-item-<index>-header`); preserve existing content IDs, active/expanded classes and single-expand configuration. Text and custom components belong inside the panel's `.accordion-body` wrapper, retaining their render context.
- Expanded accordion panels use intrinsic height with no fixed content cap. Default framework item/panel wrappers must not clip composed disclosures or late-growing content; keep padding/background on `.accordion-body` rather than restoring overflow clipping to round the item. Application-owned styles or surrounding layouts can impose their own limits; framework-default clipping is a framework defect, not an application responsibility.
- Initially closed accordion panels have hidden, inert and aria-hidden=true; open panels omit hidden/inert and use aria-hidden=false. Framework JS synchronizes these with header aria-expanded and active/expanded classes on every owned transition, restoring focus to a closing panel's header before making its descendants unavailable. Reveal the restored header after the entire owned transition, including opening a preceding panel in single-expand mode. Inside a Modal, reuse dialog-internal nearest scrolling without moving the page/backdrop; oversized targets retain native-like nearest alignment. Single-expand closes only items belonging to the nearest `.accordion`; outer closure preserves nested expansion state for reopening.
- Default accordion active/hover paints must maintain readable header contrast without retaining the muted gradient beneath the active foreground. Header/icon transitions respect reduced motion; intrinsic panel toggling does not require height animation. These defaults do not certify application accessibility.
- EditableModule edit buttons remain normal non-submit HTMX launchers with configurable target/swap. Modal focus/close/restoration belongs to framework JS, not per-wrapper scripts; preserve the launcher when focus should return after closing.
- EditableModule edit/delete setters accept complete caller URLs, not templates; never implicitly substitute or globally encode them. Append a configured editMode into the pre-fragment query, preserving all existing parameters (including editMode) and the exact fragment. Handle absent/empty queries and trailing ?/& without redundant separators; null mode leaves the URL unchanged. Routing/decoding/authorization/CSRF stay application-owned. Page/row/child template interpolation belongs only to the editing package.

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
- Accordion Java/snapshot coverage preserves `.accordion-body`, custom content/context and initial hidden/inert/ARIA state. Real generated-markup browser coverage at 390px/1440px must exercise >3000px bodies, late growth, final-control geometry and hit testing, non-clipped child disclosures, closed focus exclusion/restoration, native Enter/Space, single/multiple/nested state, dynamic insertion, computed header contrast and reduced motion. Positive rectangles or visibility assertions alone do not establish absence of clipping. Focus identity alone does not prove usable restoration: include ordinary following page content, assert post-transition header viewport bounds/hit testing in both single-expand directions and nested ancestor closure, and cover modal internal clipping with unchanged page/backdrop scroll.
- Deliberately review affected module snapshots and rerun tests without update mode after accepting them.
- Editing wrapper regressions when changing `EditableModule`, including parsed URL attributes with queries/fragments, question marks only in fragments, trailing ?/&, existing mode append semantics, unchanged complete URLs with null mode and unchanged permission/URL gates

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
