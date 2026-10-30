# Core Package Agent Guide

## Purpose
Owns rendering primitives and contracts used by the whole framework.

## Owns
- `Component`, `HtmlTag`, `Module`
- `RenderContext`, `SlotKey`, `Slot`, `SlotKeyMap`
- `Template`, `TemplateComponent`
- `Attribute`, `Style`, `SafeUrl`, `CssClassNames`

## Invariants
- Rendering must be safe by default for untrusted text/attributes.
- Shared URL allowlist behavior belongs in `SafeUrl` so link-like packages do not drift.
- `Template` must not bypass custom `HtmlTag` subclass rendering; custom renderers belong in opaque component segments.
- `Component` implementations must provide `render(RenderContext)`.
- `render()` remains the empty-context convenience path.
- `Module.build()` is idempotent and drives build-once lifecycle semantics.
- Slot/template rendering must preserve type-safe key usage.
- `SlotKey` rejects null names with `NullPointerException` in its canonical constructor and all factories. Every non-null name remains literal, including empty/blank strings; no trimming, case folding, or Unicode normalization. Providers remain nullable and unevaluated during construction; key identity remains name-only.

- Template compilation traverses getChildrenStream() and preserves custom render(RenderContext) overrides as live delegates.
- Stable standard tag rendering must not mutate configured structure. Configuration is not thread-safe; retained custom delegates (including framework overrides) must be safe to share, and contexts remain request-owned.
- Direct slots and text slots honor explicit compiled entries and COMPILE_ON_FIRST_HIT; contexts remain request-owned.
- For textarea tags, escaped static text and text slots beginning with LF or CR receive one leading LF in direct and compiled output to compensate for native HTML parsing. Slot caches retain unpadded payloads. Browsers normalize CR/CRLF to LF; trusted HTML and children are unchanged, not arbitrary markup repairs.
- Tag/attribute names reject markup delimiters; attribute replacement is case-insensitive.
- HTML encoding is not URL/CSS/HTMX policy. Generic withAttribute (including href/src replacements), raw style and HTMX endpoint helpers require application-controlled or separately context-validated values. Replacements do not synchronize Link/Image getters or invoke their typed validators.
- addStyle validates property names and rejects declaration breakout, not semantic CSS/URL values. addTrustedStyle bypasses value-breakout checks; Style.addInlineStyle is raw fragment assembly without those checks. Width helpers retain their narrower token grammar. Do not retrofit generic semantic restrictions.
- Failed initial Module builds restore entry-state ordered shallow children/attributes/cached id and remain retryable. Explicit rebuild snapshots before clearing and also restores prior built status on RuntimeException/Error, rethrowing the same failure; ordinary render of a previously built instance does not retry. Caller changes before rebuilding belong to entry state. Module id/title/text/slots/trusted HTML/subclass/descendant/external state is excluded, not a deep transaction.
- Paragraph/Divider/EditableRow/EditablePage preserve stored state during render/compile while retaining their public render overrides and live template configuration. Use component-local temporary derivation, not new generic core hooks.
- Preserve existing fluent descriptors/bridges and Column.withAttribute's declared HtmlTag return for legacy subclass compatibility.

## Do
- Add core API only when needed by multiple packages.
- Keep low-level logic deterministic and side-effect minimal.
- Treat `render(RenderContext)` as the canonical render path for custom components.
- Use `CssClassNames` for token-aware class replacement when multiple packages need the behavior.
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
- `SafeUrlTest`
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
- Core: `docs/core/01-components-htmltag-and-module-lifecycle.md`, `docs/core/02-layout-page-row-column-grid.md`, `docs/core/03-template-rendercontext-slotkey-reference.md`, `docs/core/04-rendering-pipeline-high-and-low-level.md`, `docs/core/05-css-defaults-overrides-and-structure.md`, `docs/core/06-shell-project-structure-and-asset-load-chain.md`, `docs/core/07-mobile-rendering-model-and-responsive-behavior.md`
- Patterns: `docs/patterns/01-static-page-serving-patterns.md`, `docs/patterns/02-dynamic-fragment-caching-patterns.md`, `docs/patterns/03-htmx-endpoint-and-swap-patterns.md`, `docs/patterns/04-editing-workflows-owner-user-approval.md`, `docs/patterns/05-forum-helper-implementation-and-customization.md`, `docs/patterns/06-chat-helper-sse-and-ws-hooks.md`, `docs/patterns/07-chat-conversation-scoping-and-authorization-patterns.md`, `docs/patterns/08-static-content-helper-markdown-directory-pipeline.md`
- Security: `docs/security/01-security-boundaries-and-safe-rendering.md`, `docs/security/02-authwrapper-authorizationchecker-integration.md`
- Operations: `docs/operations/01-performance-threading-and-cache-lifecycles.md`, `docs/operations/02-testing-and-troubleshooting-playbook.md`, `docs/operations/03-writing-tests-for-components-and-modules.md`, `docs/operations/04-migrating-to-1.0.1.md`
- Reference: `docs/reference/components-and-modules-catalog.md`, `docs/reference/builders-shell-nav-banner-accountbar.md`, `docs/reference/forum-helper-api-reference.md`, `docs/reference/chat-helper-api-reference.md`, `docs/reference/content-helper-api-reference.md`, `docs/reference/editing-api-reference.md`

## Documentation Sync Requirement
- Any API-surface change or major internal behavior change must trigger a docs review.
- Update affected docs in the same workstream when applicable (`README.md`, `docs/INDEX.md`, and related pages).
- If no docs update is needed, explicitly note why in the PR/commit/task summary.
