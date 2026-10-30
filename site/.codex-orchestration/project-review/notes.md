# Global Assumptions
- Review scope is the full SimplyPages repository rooted at `/home/hickelpickle/Code/Java/cannasite/java-html-framework`.
- Existing user changes in tracked files are not part of this task and must not be reverted.
- Package review agents are read-only. Documentation improvements are to be recommended in agent output and integrated serially by the orchestrator if needed.
- User requested `gpt-5.5` with `xhigh` reasoning for package review agents.

# Active Agents
- Package/domain review agents launching for core, layout, components, modules, editing, builders, demo, and docs/process.

# Completed Work
- Read root `AGENTS.md` and `.internal-dev/AGENTS.md`.
- Confirmed current demo runs on port 8081 from prior turn; no code changes made for that.
- Created `.internal-dev/plans/2026-05-23-project-review-orchestration/phase-01-cleanup-and-review.md`.
- Archived completed/stale plan artifacts under `.internal-dev/plans/.archive/` while leaving open bug reports active.

# Validation Results
- `./mvnw -pl simplypages test` passed: 450 tests, 0 failures, 0 errors.
- `./mvnw -pl demo test` passed: 37 tests, 0 failures, 0 errors.
- Required review, changelog, and plan headings verified with `rg`.

# Remediation Notes
- Pending.

# Blockers
- None currently.

# Closeout Work
- Created `.internal-dev/reviews/2026-05-23-project-quality-review.md`.
- Created `.internal-dev/changelogs/2026-05-23-project-review-and-internal-dev-cleanup.md`.
- Archived completed stale plan artifacts conservatively.

# Final Validation Status
- Complete for review/artifact scope.

# Handoff Notes
- Keep review findings evidence-based with file references and distinguish bugs from refactor targets.

## Components Package Review Notes - 2026-05-23
- Scope reviewed: `simplypages/src/main/java/io/mindspice/simplypages/components/**`, matching component tests, and reference/pattern docs for components, forum, chat, and content.
- High-priority findings: direct anchor rendering bypasses `Link` URL validation in several nav/account/dropdown helpers; custom `render()` implementations lose `RenderContext` behavior when nested; `Code.block(...).withTitle(...)` emits title text without escaping.
- Medium findings/refactors: `Form.withHxPostCsrf(...)` hand-builds JSON for `hx-headers`; `Divider.withHeight(...)` is overwritten during render and `withColor(...)` bypasses style validation; forum topic/comment renderers duplicate tag/pagination/body assembly.
- Tests were not run because this was a read-only review pass.

## Modules Package Review Notes - 2026-05-23
- Reviewed `simplypages/src/main/java/io/mindspice/simplypages/modules`, matching module tests, and module reference/docs surfaces.
- Highest-risk items: `HeroModule` bypasses existing URL/style safety helpers for CTA links and background styles; `TabsModule` default IDs collide across instances and JS activates panels globally; `QuoteModule`, `TimelineModule`, and `StatsModule` emit markup/classes that miss framework CSS contracts.
- Production-readiness gaps: module tests rely heavily on brittle `html.contains(...)`; catalog omits most module classes/API contracts; test-guide RichContent sample is stale.

# Core Package Review Notes
- Core review found security/API hardening gaps around raw tag and attribute names, style replacement order bugs, stale `innerTextSlot` after `withUnsafeHtml`, and `SlotKey` name-only type collisions.
- Recommended follow-up: validate names, fix style declaration replacement with a parsed map/boundary-aware model, clear text slot state on unsafe HTML, and expand core/security regression tests.

# Builders Package Review Notes
- Scope reviewed: `simplypages/src/main/java/io/mindspice/simplypages/builders`, matching builder/integration tests, and builder/shell docs.
- Key risks: builder style APIs bypass hardened `HtmlTag.addStyle(...)`; several required-ish shell/nav inputs accept null/blank and can emit broken boolean attributes or throw late; `buildBody()` drops configured header/top-nav content despite the body-only contract wording.
- Coverage gaps: negative validation tests for builder style/target/title/layout inputs, `buildBody()` parity expectations, and `withCollapsibleSideNav(true)` without a sidebar.
- Docs gap: builder reference lists APIs but lacks parameter contracts, validation/security notes, `buildBody()` behavior detail, and production guidance for default `/home` autoload vs explicit content/template rendering.

# Layout Package Review Notes
- Scope reviewed: `simplypages/src/main/java/io/mindspice/simplypages/layout`, matching layout/integration tests, `docs/core/02-layout-page-row-column-grid.md`, and layout catalog entries.
- Key risks: `Column` default `.col` normalization only runs through zero-arg `render()`, not nested/context/template render paths; layout mutators overwrite prior/custom classes; `Row`/`Grid` gap and alignment APIs can emit classes not backed by framework CSS.
- Coverage gaps: context/template rendering for bare/custom columns, fluent class-composition ordering, invalid grid column/gap values, sticky-sidebar null/reconfiguration paths, and CSS-backed class assertions.
- Docs gap: layout docs/catalog are too terse for production use and omit API parameter contracts, supported gap/column tokens, sticky-sidebar ordering/null behavior, class preservation expectations, and responsive/mobile semantics.

# Editing Package Review Notes
- Scope reviewed: `simplypages/src/main/java/io/mindspice/simplypages/editing`, matching editing tests, and owned editing/getting-started/pattern/security/reference docs.
- Key risks: `EditableRow.wrap(Row, ...)` stores but never renders the supplied row; `ValidationResult` exposes/stores mutable error lists despite immutable/thread-safe contract; edit/page/row IDs are interpolated into DOM ids and HTMX URLs without validation or URL encoding.
- Coverage gaps: wrapped-row preservation, max modules above 12/render failure behavior, immutable `ValidationResult` snapshots, invalid/null identifiers and URLs, modal required-field validation, and save-endpoint auth examples.
- Docs gap: getting-started and README snippets call nonexistent `vr.errors()`; production-shaped save example omits authorization; reference docs omit child URL builder methods, validation result semantics, and parameter contracts.

# Demo Package Review Notes - 2026-05-23
- Scope reviewed: `demo/src/main/java/io/mindspice/demo`, `demo/src/test`, `demo/src/main/resources`, `demo/pom.xml`, demo AGENTS files, and demo-facing docs/navigation references.
- Validation: `./mvnw -pl demo test` passed 37 tests.
- Highest-risk items: editing-demo permission flags are UI-only because direct edit/save/delete endpoints do not enforce `canEdit`/`canDelete`; nested SimpleList child action URLs are emitted without matching controller endpoints; editing/chat/forum in-memory state is mutable and mostly unbounded.
- Contract/docs gaps: demo docs/navigation do not provide a route map, endpoint safety caveats, or production hardening guidance for session identity, SSE lifecycle, in-memory storage, and generated Javadocs/source-static Javadocs behavior.
- Coverage gaps: locked editing endpoint bypasses, missing nested child endpoint 404s, invalid insert-row bounds, escaping round trips, chat cross-session conversation IDs/resource caps, and docs fallback filename collisions.
