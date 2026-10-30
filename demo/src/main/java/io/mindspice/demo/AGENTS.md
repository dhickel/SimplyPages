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
- Demo editing endpoints must enforce the same permissions shown in the UI controls.

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

The nested-list example has an immutable private LIST kind and stores CSV using split/trim. Only seeded module9 is LIST; all new modules are CONTENT, even when titled `Nested Editing Demo`. Rename and proposals never change kind. Child IDs (`item-0`, `item-1`, …) describe current visible positions, not stable identities: a stale request can address a different current item. Child labels are trimmed, nonblank, comma-free plain text, escaped only when rendered. Preserve existing stored strings without decoding. Serialize/reparse must retain every remaining visible item in order; reject changes that would lose retained empty items. Deleting the final item leaves an empty module.

Child URL templates encode raw module IDs once; the framework substitutes its child placeholder, while child forms encode the raw child ID once. Use strict URI encoding, including reserved characters such as `;`, before HTML attribute escaping. Error-form retries retain the original identity and selected mode; malformed IDs do not become different valid targets. Ordinary generated ASCII URLs remain unchanged.

The child text field uses the existing `TextArea`, with soft wrapping and no extra label restrictions. Internal line breaks and tabs remain allowed. Core textarea serialization compensates for the native parser's initial LF rule for escaped text; do not add a second padding LF to drafts. Native textarea values normalize CR and CRLF to LF; bundled HTMX submits that LF value. Save still trims only the label's edges. This is native form semantics, not an exact raw-wire or arbitrary UTF-16 preservation guarantee.

`canEdit` controls all child actions and their endpoint enforcement. `canDelete` only controls whole-parent deletion. OWNER changes content alone. USER starts from live content and replaces the entire previous module proposal with a content-only snapshot; the UI explicitly warns that earlier pending child and property changes are discarded. Approval/rejection reuse the existing flow without changing unrelated fields. Mode and flags are illustrative, not authentication or authorization.

The private `/js/editing-demo.js` asset belongs once in the home/demo full shells and direct editing shell, not in fetched fragments. It enables native HTMX swaps only for server-marked child-modal 4xx responses targeting the current modal host from its own generated controls. Errors retain submitted drafts and accessible escaped messages without page/modal OOB clearing. Successful changes clear the modal and replace the page via existing OOB markup; successful generated child DELETE uses `HX-Reswap: none` to avoid a competing in-band swap. Unmarked, foreign and 5xx failures retain HTMX defaults, with no automatic retries. Use existing framework modal focus/lifecycle handling, not a private focus API or generic fragment script executor.

One private editing monitor covers every state-dependent lookup, flag check, validation, allocation, mutation, proposal and rendered page/modal/OOB snapshot. HTTP writes happen after return. This establishes serial state outcomes, not rollback for unforeseen failures or HTTP/OOB arrival ordering. Parent edit/save/approval require current canEdit; parent delete independently requires canDelete and atomically removes its proposal. Child actions still require only canEdit. Add modal/submission check current row existence, canAddModule and the three-module capacity (404/403/409). Missing targets/proposals return 404; rejection cleanup is independent of flags. Selectable modes and approval remain shared sandbox simulations, not authentication.

## Private chat lifecycle and exposure

Normal `/chat` and explicit POST `/chat/new` initialize one exact opaque conversation per servlet session. HX redirect, history, messages and stream never allocate chat state; APIs never create sessions. Exact IDs are not trimmed or decoded again. Missing/blank IDs are 400, foreign/unknown are 404, owned expired are 410. All private chat responses are no-store, including MVC negotiation/method/mapping failures through the route-local servlet filter. Session ownership is not authentication; no CSRF, rate protection or production guarantees are supplied.

The concrete service owns bounded history and emitter accounting under one monitor: 32 conversations, two streams per session, 64 process streams, 30-minute idle expiry and five-minute streams. Expired resources are reclaimed before admission rejects; valid active chats are never evicted. Stream admission checks exact ownership first, then process exhaustion (503), then the per-session limit (429). Process exhaustion takes precedence when both apply: the accepted 32 owners times two streams equals the 64-stream process cap. History retains the welcome plus the latest 25 complete pairs (51 entries), with a monotonic cursor. The decoded message bound is 4096 UTF-16 units before trim, not a raw HTTP heap or rate limit. Preserve the existing String.trim()/empty behavior for message bodies and internal display names; do not introduce a wider Unicode whitespace/name grammar. Display names use a fixed controller fallback; internal alternatives longer than 64 units use that fallback. Successful owned page/history/message activity refreshes idle time; invalid calls and stream reconnects alone do not.

Callbacks are configured before stream publication and detach by registration identity at most once. Snapshot recipients and detach expired resources under the monitor; send/complete outside it. Terminal emitter completion catches `IllegalStateException` for a potentially recycled response; do not generalize that catch to unrelated operations. Two packaged 64-stream shutdown reproductions did not show the recycled-response destroy warning, but 30-second graceful shutdown timeout and async timeout warnings remained. A bounded periodic sweep, access/admission expiry, actual servlet-session destruction and shutdown release state and emitters. No session object is retained by the service, no global tombstones or cookie/identity logging. Stream expiry/error closes native EventSource and offers explicit same-ID Resume; owned expiry offers explicit Start New. The client drains original queued snapshots/responses before generation replacement and preserves the newest composer draft. No automatic retry, replay, resend or allocation.

The demo-local default is server.address=127.0.0.1 with the existing port and ordinary Spring override precedence. Startup always warns that this is private/non-durable; explicit non-loopback overrides are permitted but conspicuously warned. Visible warnings survive editing fragments and chat generations. Applications still own authentication, authorization, CSRF, durable identity/storage and production resource policy. These app-local boundaries do not change framework APIs.

## Required Tests
- Demo integration tests covering major editing/HTMX flows
- Permission, bounds, nested-edit, and OOB response regressions for editing demos
- Sanity checks for any new major demo route

## Dependencies
- Depends on `simplypages` published module APIs.
- Demo integration uses Boot 4.1.1, Spring MVC 7.0.9, Servlet 6.1, modular webmvc/webmvc-test starters, and Boot-managed webjars-locator-lite 1.1.4 for versionless unmodified HTMX 2.0.11. The private demo uses parent-property overrides `jackson-bom.version=3.1.7` and `tomcat.version=11.0.27`; Jackson annotations remain 2.21. This is not an unmodified vendor-tested Boot dependency set or a security/support certification. Do not retain locator-core/ClassGraph or add Jackson 2 databind fallback; the test launcher uses Jackson 3. Production servlet listener/filter registration imports retain their existing packages.
- Keep JDK 25 runtime/library bytecode and Java 21 demo bytecode. Use the checksum-pinned Maven wrapper; this module independently enforces the tool versions. Boot migration does not alter private editing/session/SSE policies; the separate terminal-completion fix does not resolve all shutdown timeout warnings.
- Full/template shell-owned legacy client defaults precede deferred custom JS; explicit upstream meta/custom overrides win. Body/manual/disabled hosts own configuration. The bounded framework Form-header hook is compatibility, not demo endpoint security or CSRF verification.
- No dependency from framework modules back to demo.

## Maintenance Requirement
Keep this file updated whenever demo architecture or integration patterns change.

See root `AGENTS.md` for global standards.

## Documentation TOC (Terse)
- Full index: `docs/INDEX.md`
- Learning path: `docs/getting-started/README.md` → `docs/tutorials/first-page.md` → `docs/tutorials/dynamic-page.md` → `docs/how-to/serve-pages-and-fragments.md`; use locally installed 1.1.1 and JDK 25, including the Boot 4.1.1 demo runtime.
- Task guides: `docs/how-to/build-a-markdown-blog.md`, `docs/how-to/build-a-forum.md`, `docs/how-to/build-user-editable-pages.md`, `docs/how-to/add-chat.md`, `docs/how-to/build-a-dashboard.md`, `docs/how-to/handle-form-submissions.md`. Listings are not installed demo routes; the private sandbox does not fulfill the authenticated editing/form recipes' security prerequisites.
- Diátaxis entry points: `docs/tutorials/README.md`, `docs/how-to/README.md`, `docs/reference/README.md`, `docs/explanation/README.md`.
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
