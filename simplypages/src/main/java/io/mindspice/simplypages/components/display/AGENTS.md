# Display Components Agent Guide

## Purpose
Owns display-oriented UI components and visual containers.

## Owns
- `Alert`, `Badge`, `Card`, `CardGrid`, `DataTable`, `InfoBox`, `Label`, `Modal`
- `OrderedList`, `UnorderedList`, `ProgressBar`, `Spinner`, `Table`, `Tag`

## Invariants
- Display components remain composable and class-driven.
- Card.withImage encodes an application-controlled or separately image-context-validated source without Image's validator. ProgressBar.withHeight directly assembles raw CSS and retains declaration separators; require application-controlled or separately CSS-context-validated values. Neither gains a new semantic policy from HTML encoding or inherited width/addStyle guards.
- `DataTable` and `Table` rendering contracts remain stable.
- Modal behavior and IDs remain predictable/safe.
- Modal body/footer and spinner messages must render consistently across direct, nested, context, and template paths.

- Alert severity is styling only; preserve caller role/live attributes and exact non-dismissible output. Do not add an automatic announcement policy.
- Alert.dismissible configures one named native type=button .alert-close child through ordinary HtmlTag structure, before rendering/compilation. Repeated calls retain the class without duplicating the control; rendering must not mutate its configuration.
- Load framework CSS/JS once in the shell. Delegated activation hides, never removes or persists, only the nearest usable dismissible Alert. Cancellation, disabled/wrong-type controls, invalid intervening ownership and background/covered-modal activation must not mutate an outer Alert. Direct/HTMX insertion needs no initialization.
- Alert focus is bounded: preserve unrelated/application-selected focus; contained focus in the top Modal uses its private dialog boundary without closing/restoring its launcher or scrolling background/backdrop. On pages, capture containing elements before callbacks, try native focus first, and borrow absent tabindex=-1 only when needed. Never overwrite caller values, including malformed/empty ones; preserve later changes and same-value writes. Retain borrowed state while focused, release on blur/failure/disconnection, and keep only alert-specific modal-opener provenance needed for restoration. Do not export a general focus service.
- Default dismissal CSS reserves control space, wraps long messages, forces hidden alerts out of layout and provides readable normal/hover/focus paint on all four severities. Verify geometry/paint at1440/390 using actual Java-generated fixtures and packaged assets in tests/browser/alert-dismissal.spec.js; custom themes/layouts remain application-owned.
- Modal body and footer receive the parent RenderContext; generated modal IDs are unique.
- Modal retains backdrop/container hooks and validated IDs. Its container is a named `role=dialog` with `aria-modal` and a title ID relationship (untitled/blank titles fall back to `aria-label=Dialog` without forcing a header).
- Framework JS owns modal focus entry, Tab containment, top-dialog close flags and connected-launcher restoration on direct/HTMX insertion and external removal. Prefer usable autofocus, then a body control, then any control, then the dialog itself; do not restore inline backdrop/key handlers. Reveal initial/keyboard focus through internal scroll containers up to the dialog only, preserving background scroll.
- Modal traversal includes native first direct summary controls, honors negative tabindex and excludes closed details content through every ancestor even when CSS leaves positive boxes. Do not use viewport intersection to reject scrollable focus targets.
- Cover modal behavior with Java semantic assertions plus `tests/browser/modal-focus-lifecycle.spec.js` and generated Page/Modal cases in `tests/browser/default-accessibility.spec.js` against the real assets and packaged demo.
- ProgressBar preserves its supplied value until rendering, clamps against a positive maximum, and reports the clamped value on its role-bearing .progress root together with min/max/name. withLabel names the progressbar; existing attribute APIs support custom/external names. Rendering remains pure.
- Keep .progress and progress-bar-striped/progress-bar-animated CSS hooks aligned, with legacy aliases and reduced-motion support. Default height is 20px; custom height belongs to the wrapper.
- Default labeled ProgressBar variants must maintain at least 4.5:1 text contrast across all gradient and stripe paints, not one favorable stop. White text belongs to default/primary; success/warning/error/info use dark text. Verify actual computed paints for plain/striped/animated and reduced-motion states with `tests/browser/progress-contrast.spec.js`; application theme overrides own their contrast.
- CSS owns the modal base layer (--sp-modal-layer); JS sets only relative --sp-modal-stack offsets. Mobile navigation controls must not overlay/activate through a dialog; test real geometry and elementFromPoint, not just z-index arithmetic.

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
- Dropping `RenderContext` when rendering modal body/footer or other child content.
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
- Core: `docs/core/01-components-htmltag-and-module-lifecycle.md`, `docs/core/02-layout-page-row-column-grid.md`, `docs/core/03-template-rendercontext-slotkey-reference.md`, `docs/core/04-rendering-pipeline-high-and-low-level.md`, `docs/core/05-css-defaults-overrides-and-structure.md`, `docs/core/06-shell-project-structure-and-asset-load-chain.md`, `docs/core/07-mobile-rendering-model-and-responsive-behavior.md`
- Patterns: `docs/patterns/01-static-page-serving-patterns.md`, `docs/patterns/02-dynamic-fragment-caching-patterns.md`, `docs/patterns/03-htmx-endpoint-and-swap-patterns.md`, `docs/patterns/04-editing-workflows-owner-user-approval.md`, `docs/patterns/05-forum-helper-implementation-and-customization.md`, `docs/patterns/06-chat-helper-sse-and-ws-hooks.md`, `docs/patterns/07-chat-conversation-scoping-and-authorization-patterns.md`, `docs/patterns/08-static-content-helper-markdown-directory-pipeline.md`
- Security: `docs/security/01-security-boundaries-and-safe-rendering.md`, `docs/security/02-authwrapper-authorizationchecker-integration.md`
- Operations: `docs/operations/01-performance-threading-and-cache-lifecycles.md`, `docs/operations/02-testing-and-troubleshooting-playbook.md`, `docs/operations/03-writing-tests-for-components-and-modules.md`, `docs/operations/04-migrating-to-1.0.1.md`
- Reference: `docs/reference/components-and-modules-catalog.md`, `docs/reference/builders-shell-nav-banner-accountbar.md`, `docs/reference/forum-helper-api-reference.md`, `docs/reference/chat-helper-api-reference.md`, `docs/reference/content-helper-api-reference.md`, `docs/reference/editing-api-reference.md`

## Documentation Sync Requirement
- Any API-surface change or major internal behavior change must trigger a docs review.
- Update affected docs in the same workstream when applicable (`README.md`, `docs/INDEX.md`, and related pages).
- If no docs update is needed, explicitly note why in the PR/commit/task summary.
