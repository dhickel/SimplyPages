# Demo Pages Agent Guide

## Purpose
Owns page composition classes used by demo controllers.

## Owns
- Demo page builders/compositions under `demo.pages`
- Consolidated demo pages for `/demos` sidebar navigation

## Invariants
- Pages are examples of framework usage, not framework internals.
- Page classes should stay readable and educational.
- Compositions should reflect current recommended APIs.

- Docs use canonical paths, safe Markdown with heading anchors, and exact resource lookup.
- Documentation HTMX links replace the content wrapper with outerHTML; keep one docs-content ID.
- The docs page uses framework module-content styling and descriptive navigation labels.

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

## Private chat composition

ChatDemoPage renders actual framework ChatModule/Form/Button/Alert components and private finite-stream controls. Visible copy must disclose non-durable history, session ownership not authentication, pruning/limits and no CSRF/rate/production guarantees. Resume opens the same owned ID only; Start New is an explicit demo action. Keep generated composer maxlength=4096 (UTF-16 units), with the server authoritative before trim. New generations use the existing app-private chat-demo.js asset, preserve drafts and settle original queued work before replacement; do not add framework transport/bootstrap/focus hooks. Public chat metadata remains app-owned routing, not library authentication.

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
- Learning path: `docs/getting-started/README.md` → `docs/tutorials/first-page.md` → `docs/tutorials/dynamic-page.md` → `docs/how-to/serve-pages-and-fragments.md`; current examples use locally installed 1.1.0a and JDK 25.
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
