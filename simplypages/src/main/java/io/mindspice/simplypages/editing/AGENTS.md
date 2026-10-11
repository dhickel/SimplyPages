# Editing Package Agent Guide

## Purpose
Owns editing contracts and reusable helpers for inline/module editing flows.

## Owns
- Editing contract (`Editable<T>`)
- Edit helpers/builders (`EditFormBuilder`, `EditModalBuilder`)
- Validation/auth wrappers (`ValidationResult`, `AuthWrapper`, `AuthorizationChecker`)
- Edit mode and editable-child primitives

## Invariants
- `Editable<T>` is the primary editing contract.
- Validation must be explicit and non-throwing for normal user errors.
- Helper outputs must be HTMX-friendly for modal/OOB workflows.
- EditModalBuilder disables backdrop closing but retains Modal's default Escape/header close. Its footer clears the configured modal host; framework JS must restore a still-connected launcher after this or HTMX/OOB removal. Include the framework assets once in the page shell.

- FormFieldHelper text inputs and textareas have associated HTML labels.
- EditableRow renders the supplied non-null Row unchanged, then a separate row of registered editable modules and an eligible add control. Preserve source attributes, content and custom `render(RenderContext)` dispatch exactly once with the same context, including template rendering; do not copy, merge or mutate source structure.
- Source content and registrations are independent; callers must avoid registering content already present in the supplied Row. Outer wrapper attributes stay distinct. Empty source rows are retained; null omits only the source row.
- EditableRow preserves registration order, module-ID assignment, equal integer `12 / count` widths, capacity/mode/permission gates and endpoint conventions. Capacities above twelve use fill columns rather than invalid zero-width grid columns.
- EditableRow remains mutable and not thread-safe, including as a retained template delegate. Source custom renderers/descendants retain their own ownership requirements.

## Do
- Keep editing abstractions framework-generic (not app-domain specific).
- Preserve existing field names and edit-view contracts.
- Keep auth wrapper behavior explicit and testable.

## Do Not
- Hardcode app-level authorization policy here.
- Mix HTTP controller concerns into low-level editing contracts.

## Common Pitfalls
- Breaking expected field names between form builders and apply/validate paths.
- Treating edit flows as trusted input.
- Forgetting compatibility with existing edit interfaces.

## Required Tests
- `EditableTest`, `EditFormBuilderTest`, `EditModalBuilderTest`
- `ValidationResultTest`, `AuthWrapperTest`, `EditableRowTest`
- EditableRow source attributes/columns/text/slots, unchanged configured source structure across renders, independent wrapper content, custom delegate once/same-context direct and compiled paths, null/empty compatibility and generated control/width/gate/order regressions
- Validation success/failure coverage for new edit flow behavior

## Dependencies
- Depends on `core`, `components`, and module wrappers where needed.
- Keep no dependency on demo-specific services/controllers.

## Maintenance Requirement
Keep this file updated whenever editing contracts, modal flow, or validation behavior changes.

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
