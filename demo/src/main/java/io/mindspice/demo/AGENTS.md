# Demo Package Agent Guide

## Purpose
Owns Spring Boot demo app wiring and integration examples for SimplyPages.

## Owns
- Demo application bootstrap and controllers in this package
- Consolidated `/demos/*` route surface and docs/javadocs integration routes

## Invariants
- Demo remains illustrative and runnable.
- Demo code should reflect real framework usage patterns.
- Demo must not redefine framework internals.

- Docs use canonical paths, safe Markdown with heading anchors, and exact resource lookup.
- Documentation HTMX links replace the content wrapper with outerHTML; keep one docs-content ID.
- The docs page uses framework module-content styling and descriptive navigation labels.

## Do
- Keep demo endpoints focused on examples and docs support.
- Update demo flows when framework APIs change.
- Maintain clarity over completeness.
- Keep top navigation stable (`Home`, `Demos`, `Javadocs`, `Forum`, `Chat`, `Blog`, `Docs`) and keep legacy routes removed.

## Do Not
- Move framework logic into demo package as a workaround.
- Treat demo patterns as hard framework requirements unless documented.

## Common Pitfalls
- Demo drift after framework API changes.
- Hardcoding outdated versions/paths.
- Coupling demo controllers too tightly to transient test data.

## Private HTML and nested-list demo contract

Rendered controller routes produce `text/html`. Only exact case-insensitive `HX-Request: true` selects an alternative representation; whitespace is not trimmed. Preserve existing `Vary` tokens. Fragment-only endpoints stay fragments, Chat retains full-document `HX-Redirect` navigation, and its SSE endpoint stays `text/event-stream`. Incompatible Accept negotiation may return 406, not a rendered HTML error.

Row insertion accepts `-1 <= position < current row count`; `-1` inserts before the first row, including on an empty page. Preserve the original Spring integer conversion (including hexadecimal and whitespace-bearing spellings). Validate before allocating an ID or changing state. Missing page is 404; malformed or out-of-range positions are 400.

The nested-list example still identifies its kind by the exact title `Nested Editing Demo` and stores CSV using split/trim. Child IDs (`item-0`, `item-1`, …) describe current visible positions, not stable identities: a stale request can address a different current item. Child labels are trimmed, nonblank, comma-free plain text, escaped only when rendered. Preserve existing stored strings without decoding. Serialize/reparse must retain every remaining visible item in order; reject changes that would lose retained empty items. Deleting the final item leaves an empty module.

Child URL templates encode raw module IDs once; the framework substitutes its child placeholder, while child forms encode the raw child ID once. Use strict URI encoding, including reserved characters such as `;`, before HTML attribute escaping. Error-form retries retain the original identity and selected mode; malformed IDs do not become different valid targets. Ordinary generated ASCII URLs remain unchanged.

The child text field uses the existing `TextArea`, with soft wrapping and no extra label restrictions. Internal line breaks and tabs remain allowed. A separate initial rendering LF accounts for HTML's textarea parser rule so leading-newline invalid drafts remain visible. Native textarea values normalize CR and CRLF to LF; bundled HTMX submits that LF value. Save still trims only the label's edges. This is native form semantics, not an exact raw-wire or arbitrary UTF-16 preservation guarantee.

`canEdit` controls all child actions and their endpoint enforcement. `canDelete` only controls whole-parent deletion. OWNER changes content alone. USER starts from live content and replaces the entire previous module proposal with a content-only snapshot; the UI explicitly warns that earlier pending child and property changes are discarded. Approval/rejection reuse the existing flow without changing unrelated fields. Mode and flags are illustrative, not authentication or authorization.

The private `/js/editing-demo.js` asset belongs once in the home/demo full shells and direct editing shell, not in fetched fragments. It enables native HTMX swaps only for server-marked child-modal 4xx responses targeting the current modal host from its own generated controls. Errors retain submitted drafts and accessible escaped messages without page/modal OOB clearing. Successful changes clear the modal and replace the page via existing OOB markup; successful generated child DELETE uses `HX-Reswap: none` to avoid a competing in-band swap. Unmarked, foreign and 5xx failures retain HTMX defaults, with no automatic retries. Use existing framework modal focus/lifecycle handling, not a private focus API or generic fragment script executor.

Applications still own CSRF, authentication, authorization, concurrent state, durable identity and production storage. This private demo is not a production security model or a library API extension.

## Required Tests
- Demo integration tests covering major editing/HTMX flows
- Sanity checks for any new major demo route

## Dependencies
- Depends on `simplypages` published module APIs.
- No dependency from framework modules back to demo.

## Maintenance Requirement
Keep this file updated whenever demo architecture or integration patterns change.

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
