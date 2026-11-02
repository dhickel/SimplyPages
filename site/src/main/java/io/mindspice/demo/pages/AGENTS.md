# Site Pages Agent Guide

## Purpose
Owns official site page composition classes used by site controllers; the Java package remains `io.mindspice.demo.pages`.

## Owns
- Demo page builders/compositions under `demo.pages`
- Discoverable framework demonstrations and documentation pages for the replacement site

## Invariants
- Pages are examples of framework usage, not framework internals.
- Page classes should stay readable and educational.
- Compositions should reflect current recommended APIs.

- Docs use canonical paths, safe Markdown with heading anchors, and exact resource lookup.
- Documentation HTMX links replace the content wrapper with outerHTML; keep one docs-content ID.
- Documentation navigation uses descriptive labels and the accepted replacement site design.

## Do
- Keep page classes focused on rendering/composition.
- Prefer reusable helper patterns when examples repeat.
- Update examples when APIs evolve.
- Keep section-level in-page navigation concise and implementation-focused.

## Do Not
- Embed business/domain persistence logic in page classes.
- Depend on unstable, deprecated framework APIs without explicit note.

## Common Pitfalls
- Example code lagging behind module/component API changes.
- Overly complex page classes that hide framework concepts.
- Mixing unrelated concerns in one demo page.

## Site composition and demo discovery

Keep the homepage card-led, with representative rendered output rather than an exhaustive symbol list. Retain the shared top navigation and grouped demo rail: a sticky left rail on desktop, a native disclosure at 900px and below, an accessible current category and links to its subsections. JavaScript handles mobile collapse, Escape and focus restoration; native links remain usable without it. Use ordinary document navigation, including for chat, so its script and SSE initialize in the current document rather than relying on HTMX bootstrap.

Group related module examples at `/demos/modules/{reading,insights,interaction,storytelling}` and builders at `/demos/builders/composition`; retain individual example URLs and anchors. Preserve the source-checked 141-type catalog, including 19 modules and 6 builders, with real previews and relevant source, API and documentation links. Site-owned forest, teal and ochre styling must not obscure the colors of framework specimens.

## Chat and sandbox composition

Editing, forum, and chat use bounded temporary session-owned state with server-enforced ownership and same-origin/CSRF protection, including browser token and form wiring. Validate changes against fresh local artifacts. Disclose non-durable state, expiry, limits and the absence of rate protection; distinguish simulated roles and session ownership from authentication. Do not claim production readiness.

Use actual framework chat components and preserve finite-stream behavior: Resume opens the same owned ID only; Start New is explicit. Keep composer maxlength=4096 (UTF-16 units), with the server authoritative before trim. Preserve drafts and settle original queued work before generation replacement; do not add framework transport/bootstrap/focus hooks. Chat metadata remains app-owned routing, not library authentication.

## Required Tests
- Add/adjust demo integration coverage when page behavior changes materially
- Ensure key routes still render after API migrations

## Dependencies
- Depends on framework APIs and demo controllers.
- Keep direction one-way: framework should not depend on demo pages.

## Maintenance Requirement
Keep this file updated whenever demo page composition patterns or API usage changes.

See root `AGENTS.md` for global standards.

Update this file in the same change whenever package-level behavior or conventions drift.

## Documentation TOC (Terse)
- Full index: `docs/INDEX.md`
- Learning path: `docs/getting-started/README.md` → `docs/tutorials/first-page.md` → `docs/tutorials/dynamic-page.md` → `docs/how-to/serve-pages-and-fragments.md`; current examples use locally installed 1.2.1 and JDK 21 or later.
- Task guides: `docs/how-to/build-a-markdown-blog.md`, `docs/how-to/build-a-forum.md`, `docs/how-to/build-user-editable-pages.md`, `docs/how-to/add-chat.md`, `docs/how-to/build-a-dashboard.md`, `docs/how-to/handle-form-submissions.md`. Keep request-owned composition and application security/data boundaries explicit; do not imply these listings are existing demo pages.
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
