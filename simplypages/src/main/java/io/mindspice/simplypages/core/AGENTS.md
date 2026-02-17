# Core Package Agent Guide

## Purpose
Owns rendering primitives and contracts used by the whole framework.

## Owns
- `Component`, `HtmlTag`, `Module`
- `RenderContext`, `SlotKey`, `Slot`, `SlotKeyMap`
- `Template`, `TemplateComponent`
- `Attribute`, `Style`

## Invariants
- Rendering must be safe by default for untrusted text/attributes.
- `render(RenderContext)` behavior must remain compatible with `render()` defaults.
- `Module.build()` is idempotent and drives build-once lifecycle semantics.
- Slot/template rendering must preserve type-safe key usage.

## Do
- Add core API only when needed by multiple packages.
- Keep low-level logic deterministic and side-effect minimal.
- Document behavior changes in Javadocs and tests.

## Do Not
- Put domain-specific UI logic here.
- Bypass escape/encoding paths for untrusted data.
- Break existing render path contracts without migration notes.

## Common Pitfalls
- Mixing per-request mutable state into built module structure.
- Changing render order semantics unintentionally.
- Breaking default `RenderContext.empty()` behavior.

## Required Tests
- `HtmlTagTest`, `ModuleTest`, `RenderContextTest`, `TemplateTest`, `SlotKeyTest`
- Escaping/attribute safety regressions
- Slot/default value semantics

## Dependencies
- Keep `core` dependency-light and foundational.
- Other packages may depend on `core`; avoid reverse dependencies.

## Maintenance Requirement
Keep this file updated whenever core contracts or invariants change.

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
