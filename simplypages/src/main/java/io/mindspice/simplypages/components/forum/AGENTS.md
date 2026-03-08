# Forum Components Agent Guide

## Purpose
Owns discussion/community components and helper rendering contracts.

## Owns
- `ForumPost`, `PostList`, `Comment`, `CommentThread`
- `ForumHelper` unified category/topic/comment rendering API
- Forum tag parsing and hydration contracts (`ForumTagParser`, resolver registry/interfaces)
- Forum action decorator contracts (`ForumActionDecorator`, `DefaultForumActionDecorator`)
- Topic/comment pagination rendering contracts (`TopicPagination`, `CommentPagination`) with HTMX previous/next control wiring

## Invariants
- Forum component structure remains composable and render-safe.
- Markdown/text content handling remains aligned with core escaping rules.
- List/thread ordering behavior is predictable.
- Tag parsing is fault-tolerant: malformed/unresolved tags preserve visible literal output.
- Tag resolver registration is deterministic and duplicate keys fail fast.
- Comment identity layout reserves a 150x150 avatar slot; missing avatars must remain blank without collapsing layout.
- Pagination controls must enable/disable deterministically from page bounds and include scope/topic-scoped HTMX refresh links.

## Do
- Keep components generic enough for multiple forum-like domains.
- Preserve fluent builders for post/comment metadata.
- Add tests for any structure or metadata output changes.
- Keep data lookups out of render-time component paths; use batch resolver interfaces.

## Do Not
- Embed moderation/business workflow logic in component layer.
- Depend on demo controller behavior.

## Common Pitfalls
- Breaking nested comment/thread rendering structure.
- Mixing trusted/raw HTML without explicit intent.
- Drift between forum CSS classes and generated markup.
- Introducing per-node lookup callbacks that create N+1 render-time behavior.

## Required Tests
- Existing forum component test suite and any new render-path tests
- Regression tests for hierarchy/nesting and metadata fields

## Dependencies
- Depends on `core` and base/shared components.
- Keep independent from service/controller concerns.

## Maintenance Requirement
Keep this file updated whenever forum component structure or rendering contracts change.

See root `AGENTS.md` for global standards.

Update this file in the same change whenever package-level behavior or conventions drift.

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
