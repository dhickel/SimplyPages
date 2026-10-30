# SimplyPages - Engineering Agent Guide


## `.internal-dev` Development Document Store

`.internal-dev/` is the persistent engineering document store for plans, bugs, changelogs, reviews, notes, and reusable knowledge.

### When you finish a task, you must use `.internal-dev` for:
- Always create a changelog in `.internal-dev/changelogs/` for finalized work (no exceptions).
- Add any general knowledge to : `.internal-dev/knowledge/`
- Add any notes to : `.internal-dev/notes/`, using or creating the future_consideration.md for future improvement/concerns that should be addressed
- Add any out-of-scope bugs to:`.internal-dev/bugs/`
- Use `.internal-dev/knowledge/agent-test-implementation-loop.md` as the default testing implementation reference for agent work loops.


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
- `Component` is the root interface; implementations must provide `render(RenderContext)`, while `render()` remains the empty-context convenience path
- `Component.compile()` is the discoverable shortcut for `Template.of(component)`
- `HtmlTag` is the common base for most concrete components
- `Module` provides high-level composition with a build lifecycle (`build()` + `buildContent()`)
- Escaping and attribute safety are handled at framework level via OWASP encoder in core rendering paths

### Template and Slot System (First-Class)
- Dynamic data injection is modeled via `SlotKey<T>`, `Slot`, and `RenderContext`
- `Template` compiles static component structure for repeated rendering with context values
- Compilation preserves generated child streams; custom `render(RenderContext)` overrides remain live delegates
- Template sharing requires retained custom delegates to be safe to share
- Prefer slot-driven rendering for request-time dynamic content over mutating built module structure

### Shell and Client Ownership
- The library bundles unmodified HTMX 2.0.11 through WebJars; the Boot 4.1.1 demo uses BOM-managed locator-lite for the versionless URL.
- Full/template shells that include HTMX privately initialize legacy selfRequestsOnly=false, GET-only URL parameters (DELETE body), and smooth-scroll defaults at the top of existing framework JS, before deferred custom JS. Explicit upstream host meta and custom-script/ready overrides win. The private head marker is not public configuration; no insertion/settle reapplication.
- withHtmx(false), body fragments, and unmarked/manual hosts do not select compatibility defaults. Hosts load assets once and explicitly configure desired upstream policy. Compatibility is not destination/CORS/CSRF/authorization enforcement.
- The Form-source configRequest hook repairs only missing Object.prototype-collision headers from an own literal exactly-one-string-pair attribute. Preserve event-stage own entries/case aliases/null/accessors/nonextensible maps/cancellation and later overrides. No generic inherited/evaluated/options parser, Form runtime/JSON change, header rewrite, token verification, or alternate transport. See the asset reference for upstream pre-event ajax-options limits.

### Module Lifecycle Contract
- Modules are built lazily and idempotently through `build()`
- `buildContent()` is structure composition, not per-request mutation logic
- Failed initial builds restore entry-state shallow children/attributes/cached id and remain retryable. Explicit rebuild failure also restores prior build status before rethrowing the same RuntimeException/Error: an already-built module does not retry during ordinary render. This excludes moduleId/title/text/slots/trusted HTML/subclass/descendant/external state; no deep transaction guarantee.
- Paragraph/Divider/EditableRow/EditablePage derive readonly temporary output and remain live template delegates. Editable wrappers retain generic inherited children without emitting them; source rows keep exactly-once same-context dispatch. Finish configuration before sharing; arbitrary delegates/contexts/descendants have separate obligations.
- Divider reserves exact orientation-local generated styling in temporary output; inactive canonical horizontal names and whole divider-with-text token are suppressed even when caller-configured. Preserve outside/case-variant styles and stored configuration; see the catalog for the exact namespace.
- SimpleList lookup/removal uses literal non-null raw item ids, not positional editable descriptors. add/remove are configuration-only; applyEdits explicitly rebuilds.
- Preserve actual fluent declared returns/bridges; Column.withAttribute declares HtmlTag for legacy subclass source compatibility. Do not introduce a blanket covariant/self-type rewrite.
- Module width methods (`withWidth`, `withMaxWidth`, `withMinWidth`) are intentionally blocked on `Module`
- Layout controls module sizing via `Row`/`Column` containers

## Package Map
- `core`: rendering primitives, slots/templates, attributes/styles, module base
- `components`: low-level UI components by domain (`forms`, `display`, `media`, `forum`, `chat`, `content`, `navigation`)
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
- For visual fixes affecting default framework components, update `simplypages/src/main/resources/static/css/framework.css` instead of demo-local styling.

### Security Responsibility Split
- Framework responsibilities:
- Escape untrusted text and attributes in render paths
- Validate sensitive style inputs where implemented
- Consumer application responsibilities:
- CSRF, authorization, endpoint security, and HTMX request policy at HTTP/controller layer
- Input validation and domain authorization on business operations

### Testing Expectations
- Any behavior/API change requires targeted tests in the matching package test suite
- Always implement tests for new features in the same workstream; do not defer test coverage.
- Prioritize regression coverage for rendering, escaping, lifecycle, editing flow, and layout behavior
- Demo changes should not replace framework tests
- For bug investigations, run relevant local automated tests and reproduce the issue against a freshly packaged local process when applicable; verify the fix with focused regression coverage.
- For UI bug reports, validate both behavior and visual alignment/layout in a local browser before classifying the issue as framework-level.

## Build and Test Commands

- Use the official Maven Wrapper 3.3.4 (`only-script`), pinned to Maven 3.10.0 with a distribution checksum. Reactor and independent module POMs enforce JDK 25 and Maven `[3.10.0,4)`.
- Library output is Java 25 bytecode; demo output is Java 21 bytecode but still requires Java 25 at runtime.
- Keep pinned plugins, output timestamps, source/Javadoc attachments, and fatal structural Javadoc lint (`all,-missing`). Missing prose remains tracked documentation debt, not a completeness guarantee.
- Browser checks use the existing `tests/browser` harness after a fresh Maven package including tests/fixtures, then `npm ci` and pinned Playwright/Chromium. See `docs/how-to/build-test-and-document.md`; do not substitute stale artifacts.
- The controlled-beta support/known-issues record is `docs/reference/build-and-assets.md#beta-status-known-issues-and-support`. Do not infer remote CI, continuous security, coverage gates, publication, or production readiness from local checks.

### From Repository Root
```bash
./mvnw clean install
./mvnw clean verify
./mvnw test
./mvnw -pl simplypages test
./mvnw -pl demo spring-boot:run
```

### Module-Scoped
```bash
cd simplypages && ../mvnw test
cd demo && ../mvnw spring-boot:run
```

## Local Process and Browser Validation
- For feature improvements and bug fixes, run relevant local automated tests and verify applicable runtime behavior against a freshly packaged local process before finalizing.
- For browser-facing changes, use the existing browser harness with fresh artifacts and verify both behavior and visual alignment/layout as relevant.
- Do not deploy to or verify against a live host.

## Documentation Structure
- `docs/README.md` is the Diátaxis landing page and the default `/docs` entry point.
- `docs/tutorials/`: runnable learning exercises; `docs/how-to/`: task-oriented instructions.
- `docs/reference/`: API/build contracts; `docs/explanation/`: design and integration reasoning.
- Keep existing topic-based URLs stable and link new pages from their section and `docs/INDEX.md`.
- Documentation tests validate local links/anchors, guide discoverability, and complete Java examples.
- JDK 25 is required; retain library Java 25 bytecode and the demo's Java 21 bytecode target. Dependency migration does not authorize API/bridge or target changes.

## Documentation TOC (Terse)
- Full index: `docs/INDEX.md`
- Learning path: `docs/getting-started/README.md` → `docs/tutorials/first-page.md` → `docs/tutorials/dynamic-page.md` → `docs/how-to/serve-pages-and-fragments.md`; current examples use locally installed 1.1.1 and JDK 25.
- Task guides: `docs/how-to/build-a-markdown-blog.md`, `docs/how-to/build-a-forum.md`, `docs/how-to/build-user-editable-pages.md`, `docs/how-to/add-chat.md`, `docs/how-to/build-a-dashboard.md`, `docs/how-to/handle-form-submissions.md`. Keep application identity/CSRF/storage/transport prerequisites explicit; these listings are not installed demo routes.
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

## Naming Conventions
Use `SimplyPages` terminology in docs and updates.
