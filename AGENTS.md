# SimplyPages - Engineering Agent Guide


## `.internal-dev` Development Document Store

`.internal-dev/` is the persistent engineering document store for plans, bugs, changelogs, reviews, notes, and reusable knowledge.

### When you are finish task you must use internal-dev for (after asking the user it if time to first):
- Making a changelog to: `.internal-dev/changelogs/`:
- Add any general knowledge to : `.internal-dev/knowledge/`
- Add any notes to : `.internal-dev/notes/`, using or creating the futuer_consideration.md for future improvement/concerns that should be addressed
- Add any out of scope bugs to:`.internal-dev/bugs/`


When generating plans or reviews you are to always use  `.internal-dev/plans/` or `.internal-dev/reviews/`, large multistep plans should have their own directory.

- Operating guide and templates: `.internal-dev/AGENTS.md`
- `.internal-dev/` is intentionally untracked in this repo so the workflow can stay stable across repos.
- Structure:
- `.internal-dev/bugs/`: out-of-scope bugs found during other work (log immediately).
- `.internal-dev/plans/`: active plans in nested plan directories with phase files.
- `.internal-dev/reviews/`: review outputs.
- `.internal-dev/notes/`: deferred ideas/future considerations.
- `.internal-dev/knowledge/`: reusable research and learner-facing summaries.
- `.internal-dev/changelogs/`: finalized change records.
- Do not read `.internal-dev` broadly by default.
- Use controlled access: read only files needed for the active task.
- Ask before logging future considerations in `notes/` when they are out of scope.
- Move finalized bug/plan artifacts to sibling `.archive/` directories.
- Create changelog entries for finalized work.
- Keep AGENTS and `.internal-dev` documentation aligned with major architecture/process changes.


## Project Positioning
SimplyPages is an active internal Java-first SSR framework for building data-heavy web applications with minimal JavaScript and pragmatic HTMX usage.

### Primary Audience
- Java backend engineers (Spring-friendly) who want server-rendered UI composition in Java
- Teams prioritizing maintainability and type-safety over frontend framework complexity

### Scope
- In scope: framework architecture, component/module design rules, rendering model, integration expectations
- Out of scope: speculative roadmap and phase tracking (track those in `.internal-dev/notes/future_consideration.md`)

## Architecture (High-Level)

### Core Rendering Model
- `Component` is the root interface and supports `render()` and `render(RenderContext)`
- `HtmlTag` is the common base for most concrete components
- `Module` provides high-level composition with a build lifecycle (`build()` + `buildContent()`)
- Escaping and attribute safety are handled at framework level via OWASP encoder in core rendering paths

### Template and Slot System (First-Class)
- Dynamic data injection is modeled via `SlotKey<T>`, `Slot`, and `RenderContext`
- `Template` compiles static component structure for repeated rendering with context values
- Prefer slot-driven rendering for request-time dynamic content over mutating built module structure

### Module Lifecycle Contract
- Modules are built lazily and idempotently through `build()`
- `buildContent()` is structure composition, not per-request mutation logic
- Module width methods (`withWidth`, `withMaxWidth`, `withMinWidth`) are intentionally blocked on `Module`
- Layout controls module sizing via `Row`/`Column` containers

## Package Map
- `core`: rendering primitives, slots/templates, attributes/styles, module base
- `components`: low-level UI components by domain (`forms`, `display`, `media`, `forum`, `navigation`)
- `layout`: page/grid primitives (`Page`, `Row`, `Column`, etc.)
- `modules`: high-level composed feature modules
- `editing`: editing contracts, helpers, auth wrappers, validation and edit flow utilities
- `builders`: shell and navigation builders for app scaffolding
- `demo`: framework usage examples and integration demonstrations

Package-specific operational rules live in package-level `AGENTS.md` files.

## Maintenance Requirements
- Keep this root `AGENTS.md` updated as architecture/process changes during development.
- When making large or cross-package changes, always review and update affected package-level `AGENTS.md` files in the same workstream.
- If a package contract changes and no package `AGENTS.md` update is needed, explicitly validate that no guidance drift was introduced.

## Global Engineering Rules

### Design Rules
- Prefer composition over inheritance for feature assembly
- Keep fluent APIs consistent (`create()` factories, chaining methods, typed returns)
- Keep module/component responsibilities clear: components are primitives, modules are compositions
- Keep docs and tests aligned with behavior-changing code

### Security Responsibility Split
- Framework responsibilities:
- Escape untrusted text and attributes in render paths
- Validate sensitive style inputs where implemented
- Consumer application responsibilities:
- CSRF, authorization, endpoint security, and HTMX request policy at HTTP/controller layer
- Input validation and domain authorization on business operations

### Testing Expectations
- Any behavior/API change requires targeted tests in the matching package test suite
- Prioritize regression coverage for rendering, escaping, lifecycle, editing flow, and layout behavior
- Demo changes should not replace framework tests

## Build and Test Commands

### From Repository Root
```bash
./mvnw clean install
./mvnw test
./mvnw -pl simplypages test
./mvnw -pl demo spring-boot:run
```

### Module-Scoped
```bash
cd simplypages && ../mvnw test
cd demo && ../mvnw spring-boot:run
```

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

## Naming Conventions
Use `SimplyPages` terminology in new docs and updates. `JHF` appears in older materials and should be treated as legacy naming.
