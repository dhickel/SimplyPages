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
- Module URL and inline-style configuration must use shared core validation paths.
- Module-emitted classes must match `framework.css` hooks or preserve documented aliases.
- Mutators that remain valid after first render must invalidate/rebuild module content deliberately.

## Do
- Keep each module focused on one UI concern.
- Compose existing components instead of custom HTML assembly where possible.
- Expose sensible defaults and explicit configuration methods.

## Do Not
- Re-introduce mutable render-time structure hacks.
- Duplicate core escaping behavior inside module code unless necessary.

## Common Pitfalls
- Mutating children post-build without rebuild semantics.
- Emitting module-specific class names that are not styled by the framework CSS.
- Assuming width methods on modules are supported.
- Creating tight coupling to demo controllers/routes.

## Required Tests
- Module tests in `simplypages/src/test/java/.../modules`
- Lifecycle regressions (`ModuleLifecycleTest`)
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
- Core: `docs/core/01-components-htmltag-and-module-lifecycle.md`, `docs/core/02-layout-page-row-column-grid.md`, `docs/core/03-template-rendercontext-slotkey-reference.md`, `docs/core/04-rendering-pipeline-high-and-low-level.md`, `docs/core/05-css-defaults-overrides-and-structure.md`, `docs/core/06-shell-project-structure-and-asset-load-chain.md`, `docs/core/07-mobile-rendering-model-and-responsive-behavior.md`
- Patterns: `docs/patterns/01-static-page-serving-patterns.md`, `docs/patterns/02-dynamic-fragment-caching-patterns.md`, `docs/patterns/03-htmx-endpoint-and-swap-patterns.md`, `docs/patterns/04-editing-workflows-owner-user-approval.md`, `docs/patterns/05-forum-helper-implementation-and-customization.md`, `docs/patterns/06-chat-helper-sse-and-ws-hooks.md`, `docs/patterns/07-chat-conversation-scoping-and-authorization-patterns.md`, `docs/patterns/08-static-content-helper-markdown-directory-pipeline.md`
- Security: `docs/security/01-security-boundaries-and-safe-rendering.md`, `docs/security/02-authwrapper-authorizationchecker-integration.md`
- Operations: `docs/operations/01-performance-threading-and-cache-lifecycles.md`, `docs/operations/02-testing-and-troubleshooting-playbook.md`, `docs/operations/03-writing-tests-for-components-and-modules.md`, `docs/operations/04-migrating-to-1.0.1.md`
- Reference: `docs/reference/components-and-modules-catalog.md`, `docs/reference/builders-shell-nav-banner-accountbar.md`, `docs/reference/forum-helper-api-reference.md`, `docs/reference/chat-helper-api-reference.md`, `docs/reference/content-helper-api-reference.md`, `docs/reference/editing-api-reference.md`

## Documentation Sync Requirement
- Any API-surface change or major internal behavior change must trigger a docs review.
- Update affected docs in the same workstream when applicable (`README.md`, `docs/INDEX.md`, and related pages).
- If no docs update is needed, explicitly note why in the PR/commit/task summary.
