# Layout Package Agent Guide

## Purpose
Owns page composition and responsive layout primitives.

## Owns
- `Page`, `Row`, `Column`, `Grid`, `Container`, `Section`

## Invariants
- `Row` remains the row container abstraction.
- `Row.withChild` auto-wrap behavior for non-`Column` components stays predictable.
- `Column.withWidth(int)` accepts only 1..12.
- `Column.auto()` and `Column.fill()` semantics remain stable.
- `Column` establishes the exact `col` token during configuration, never during rendering; preserve its documented replacement semantics and HtmlTag-declared withAttribute return.
- Row layout token helpers emit CSS-backed classes. Grid accepts columns 1..6 and retains caller gap spelling; Grid updates preserve whole custom tokens and recognize class aliases without folding token case.
- Container/Section and Row modifiers preserve base/custom classes and replace their own token families; do not extend this rule to Column's class replacement helpers.
- `Page.withStickySidebar(...)` keeps one initially closed native details/first summary/content stream and stable responsive hooks, separate from ShellBuilder's drawer. Generated aside has tabindex -1 for programmatic fallback, not a sequential stop; caller details attributes are never overwritten.
- Shared framework JS keeps desktop >=769 natively open and restores each element's remembered observable native/application open preference at <=768. Desktop close updates mobile preference before reopening. Ordinary nested details are untouched; new/replacement nodes initialize independently, same-element reinsert/reparent retains preference, detached nodes receive no responsive writes/focus.
- Page native state uses private target-only open observers with pending drains/isolated owned writes, not coalesced-toggle ignore flags/property interception/general mutation services. Desktop state initializes before modal traversal. Transition-contained focus uses summary/content/root and existing private top-modal/reveal boundaries; preserve deliberate/application focus and caller attrs, never steal covered/background focus or scroll. Reveal retained in-dialog focus after responsive reflow within existing internal clipping/scroll boundaries. Alert loan/opener observers remain separate.
- CSS follows native open state. Without JS closed desktop summaries remain usable, and open summaries stay displayed while themselves focused for keyboard recovery. Validate native visibility/Tab eligibility, not positive boxes/display alone.
- `PageBuilder.build()` remains idempotent for unchanged builder state.

- Column establishes its base `col` token during configuration, including generic class attributes; standard rendering must not mutate attributes.
- `Column.withClass`, `withWidth(int)`, `auto`, and `fill` replace classes; `addClass` appends.
- Default Column styling must be present in nested context-aware rendering.
- Finish mutable configuration before sharing; standard Column compiles as tag structure, while custom subclass render overrides remain caller-owned live delegates.

## Do
- Keep layout APIs fluent and simple.
- Validate width/shape constraints at API boundaries.
- Preserve backward-compatible CSS class semantics where possible.
- Prefer `CssClassNames` when adding layout class mutators.

## Do Not
- Move module-specific logic into layout classes.
- Add framework business logic to layout components.

## Common Pitfalls
- Overwriting `class` attributes and dropping prior classes.
- Changing row/column wrapping behavior without tests.
- Breaking mobile stacking assumptions in framework CSS alignment.

## Required Tests
- `RowTest`, `ColumnTest`, `PageTest`, `GridTest`, `ContainerTest`, `SectionTest`
- Regression coverage for width validation and class generation
- Fresh Column/template concurrent first rendering without warm-up, independent contexts, unchanged attributes, direct/compiled parity, and custom subclass delegation

## Dependencies
- Depend on `core` and generic components only.
- Do not introduce dependencies on `modules` or demo-specific code.

## Maintenance Requirement
Keep this file updated whenever layout behavior, constraints, or breakpoints change.

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
