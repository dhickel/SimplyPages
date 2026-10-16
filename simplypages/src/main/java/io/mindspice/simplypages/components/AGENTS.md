# Components Package Agent Guide

## Purpose
Owns shared low-level UI components and base component conventions.

## Owns
- Generic components in this package (`Div`, `Header`, `Paragraph`, `Markdown`, etc.)
- Cross-cutting component behavior not specific to subpackages
- Chat helper contracts live under `components/chat` and follow forum-style render contracts
- Static content helper contracts live under `components/content` and generate stateless markdown section artifacts

## Invariants
- Components follow `HtmlTag` extension patterns consistently.
- `create()` factory methods and fluent return types stay consistent.
- Attribute and text rendering remains safely escaped by default; encoding is not URL/CSS policy.
- Image constructors/factories/withSrc classify boundary-C0/ASCII-space-trimmed, TAB/LF/CR-removed, ASCII-case-folded values independently of locale, rejecting javascript/non-image data only. Preserve original storage/emission and existing custom/FTP/blob/data:image prefix acceptance; no payload/MIME guarantee or Link allowlist.
- Dropdown.addLink and AccountWidget's four destination setters/dynamic endpoint remain application-controlled or separately destination/context-validated, not Link-validated. Preserve their null/optional behavior and encoded output.
- Spacer.custom assembles raw CSS, retaining separators; require application-controlled or separately CSS-context-validated values. It does not inherit addStyle breakout protection or width-token validation.

- Custom rendering must override render(RenderContext) and forward context to all children.
- Code titles are escaped; code, spinner, and paragraph presentation must work when nested or compiled.
- Paragraph/Divider rendering and compilation derive temporary attributes without changing stored state. Retain public render overrides/live template configuration, virtual child streams, inherited children, text slots/trusted HTML/id fallback/escaping/order. Paragraph recognizes logical class aliases and filters only exact known alignment tokens.
- Horizontal Divider reserves exact lowercase border-top,width,display,line-height,position,text-align,margin and whole outer divider-with-text. Null text generates first four and suppresses last three/token; any non-null text including empty generates last three/token and suppresses first four, even caller-configured. Vertical generates only border-left/display, with no inactive cleanup/no outer-token ownership. Cleanup is temporary; preserve outside styles/classes/case variants/height/custom properties and helper order/null handling. No generalized CSS normalization, caller validation or history/provenance tracking.
- Dropdown and authenticated AccountWidget use native disclosure buttons and hidden ordinary-link/item panels, not partial ARIA menus. Preserve data-sp-disclosure hooks and stable aria-controls/expanded relationships; framework JS handles activation/dismissal including child Escape before modal Escape.
- AccountWidget generates state-derived child streams without clearing/repopulating stored children. Preserve inherited children, context dispatch, compile-on-first-use and retained custom child delegates; configuration still must finish before sharing.

## Do
- Keep component APIs minimal and composable.
- Reuse `HtmlTag` capabilities before introducing custom render logic.
- Add targeted component tests for new behavior.

## Do Not
- Duplicate functionality already covered in subpackages.
- Embed app-specific routing/business concerns into components.

## Common Pitfalls
- Returning wrong fluent type and breaking method chaining.
- Overriding `render()` without preserving core safety semantics.
- Inconsistent class naming versus framework CSS.

## Required Tests
- Component tests under `simplypages/src/test/java/.../components`
- Security-related markdown/raw HTML tests where applicable

## Dependencies
- Depend on `core`; avoid dependencies on `modules`.
- Subpackages hold domain-specific component families.

## Maintenance Requirement
Keep this file updated whenever shared component conventions or base APIs change.

See root `AGENTS.md` for global standards.

## Documentation TOC (Terse)
- Full index: `docs/INDEX.md`
- Fundamentals: `docs/fundamentals/01-web-and-htmx-primer.md`, `docs/fundamentals/02-simplypages-mental-model.md`, `docs/fundamentals/03-css-fundamentals.md`
- Getting started: `docs/getting-started/README.md`, `docs/getting-started/01-installation-and-first-static-page.md`, `docs/getting-started/02-dynamic-pages-with-slotkey-rendercontext.md`, `docs/getting-started/03-editing-system-first-implementation.md`
- Core: `docs/core/01-components-htmltag-and-module-lifecycle.md`, `docs/core/02-layout-page-row-column-grid.md`, `docs/core/03-template-rendercontext-slotkey-reference.md`, `docs/core/04-rendering-pipeline-high-and-low-level.md`, `docs/core/05-css-defaults-overrides-and-structure.md`
- Patterns: `docs/patterns/01-static-page-serving-patterns.md`, `docs/patterns/02-dynamic-fragment-caching-patterns.md`, `docs/patterns/03-htmx-endpoint-and-swap-patterns.md`, `docs/patterns/04-editing-workflows-owner-user-approval.md`, `docs/patterns/08-static-content-helper-markdown-directory-pipeline.md`
- Security: `docs/security/01-security-boundaries-and-safe-rendering.md`, `docs/security/02-authwrapper-authorizationchecker-integration.md`
- Operations: `docs/operations/01-performance-threading-and-cache-lifecycles.md`, `docs/operations/02-testing-and-troubleshooting-playbook.md`
- Reference: `docs/reference/components-and-modules-catalog.md`, `docs/reference/builders-shell-nav-banner-accountbar.md`, `docs/reference/content-helper-api-reference.md`, `docs/reference/editing-api-reference.md`

## Documentation Sync Requirement
- Any API-surface change or major internal behavior change must trigger a docs review.
- Update affected docs in the same workstream when applicable (`README.md`, `docs/INDEX.md`, and related pages).
- If no docs update is needed, explicitly note why in the PR/commit/task summary.
