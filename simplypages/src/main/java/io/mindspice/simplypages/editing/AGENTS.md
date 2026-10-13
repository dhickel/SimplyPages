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
- EditableRow/EditablePage render and compile without changing stored attributes/children/configuration; retain public render overloads/live templates and later configuration behavior. Generic inherited wrapper children remain stored but un-emitted; do not expand output. Finish mutable configuration before sharing. Source custom renderers, registered modules, descendants and request contexts retain their own lifecycle/sharing obligations.
- Generic EditablePage insertion positions remain 0 for empty and 1..N after rows, with existing targets/swaps. Do not copy a private controller's different positions or overwrite supplied row settings.
- SimpleList raw helper identity is literal non-null ListItem.getId: null queries never match, unassigned ids are skipped, find returns first/remove deletes all duplicates. Positional editable descriptors remain separate and may renumber; no fallback or ID assignment. Model add/remove do not rebuild; applyEdits explicitly does.
- Editing endpoint templates have exactly four purpose-specific page/row setters. Page insertion configuration must not overwrite supplied row configuration; row edit/delete/add-form settings remain independent. Keep interpolation in the package-private EditingUrlTemplate helper, not a public routing/resolver framework.
- Supported tokens occupy only whole path segments or whole query values in both literal and browser-relevant structure (C0/space edge trimming, ignored TAB/LF/CR and path-backslash separators, classification only). In browser-relevant structure, token-bearing absolute URLs require scheme://nonempty-literal-authority; protocol-relative, root-relative, relative-path and query-only references remain supported. Scheme shorthands, empty authorities and opaque templated URLs are unsupported; fixed URLs do not acquire structural validation. Validate templates at setters; reject null new page/row templates, unknown tokens and invalid placements. Existing child URL setters accept null to disable the action but otherwise use the same grammar with {id}. Repeated/omitted tokens and fixed URLs remain valid.
- Encode raw ids as UTF-8 RFC3986 components (unreserved literal, space %20, literal percent encoded). Never trim, normalize, decode or recursively interpolate. Reject null/empty/whitespace-only/exact dot/dotdot/malformed UTF-16 only when a token actually uses the id. Empty child lists, omitted tokens and gated controls do not add global descriptor validation. DOM ids/selectors remain separate.
- Modal save/delete and low-level EditableModule edit/delete URLs remain complete caller URLs, not implicit templates. Literal URL portions and server routing/decoding/encoded-slash policy, authorization and CSRF remain application-owned.

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
- EditingUrlTemplateTest and page/row/modal parsed-attribute tests cover defaults, custom row-scoped routes, independent settings, repeated/omitted/fixed tokens, grammar, raw reserved/percent/Unicode/supplementary ids and used-versus-unused rejection. EditingUrlBrowserFixtureTest generates fresh actual markup for tests/browser/editing-url-conventions.spec.js; browser normalization checks are not encoded-slash server acceptance tests.

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
