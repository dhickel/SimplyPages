# Context
Senior review identified release-blocking mobile UX and responsiveness issues in framework shell/layout behavior (missing mobile nav control, column overflow, and sticky-sidebar mobile ergonomics). Work also required a new documentation phase describing how mobile rendering works in SimplyPages.

# Goal
Deliver release-ready mobile behavior for phases 1-3 of the mobile responsiveness review and add decision-complete framework documentation for mobile rendering ownership and extension.

# In Scope
- `ShellBuilder` mobile sidebar toggle markup wiring.
- `framework.js` mobile sidebar toggle logic.
- `framework.css` responsive stacking/wrap/sticky improvements and small-screen InfoBox tuning.
- `Page.withStickySidebar(...)` mobile collapse markup hook.
- Test updates and snapshot updates for affected shell/integration outputs.
- New core mobile rendering documentation + TOC/link updates.
- Builder/layout package `AGENTS.md` drift prevention updates for changed contracts.

# Out of Scope
- Review Phase 4 performance/minification and caching work.
- New public Java API for mobile behavior configuration.
- Visual regression automation tooling rollout.

# Implementation Steps
1. Add mobile toggle button to shell output when sidebar exists (`mobile-sidebar-toggle`, ARIA controls/expanded).
2. Implement `toggleMobileSidebar()` in framework JS to toggle `main-sidebar.mobile-open` and update toggle ARIA state.
3. Preserve desktop collapsible sidebar behavior and hide desktop collapse button on mobile breakpoint.
4. Harden responsive CSS:
   - force mobile full-width stacking for row columns,
   - add `min-width: 0` and word-wrapping guards for `.col` and `.module-content`,
   - retain existing table/pre horizontal overflow affordances.
5. Implement sticky-sidebar mobile collapse model:
   - update `Page.withStickySidebar(...)` to emit `details/summary` wrapper hooks,
   - add CSS for mobile-collapsed/desktop-expanded behavior.
6. Add `<480px` InfoBox typography/spacing/icon adjustments.
7. Update/extend tests:
   - `ShellBuilderTest`, `FullPageRenderingTest`, `FrameworkAssetCompatibilityTest`, `PageTest`.
8. Regenerate changed integration snapshots under `simplypages/src/test/resources/snapshots/integration/full-page/`.
9. Add dedicated docs page: `docs/core/07-mobile-rendering-model-and-responsive-behavior.md`.
10. Update docs indexes/cross-references and touched package `AGENTS.md` files.

# Validation
- `./mvnw -pl simplypages -Dtest=FullPageRenderingTest -DupdateSnapshots=true test`
- `./mvnw -pl simplypages -Dtest=ShellBuilderTest,PageTest,FrameworkAssetCompatibilityTest,FullPageRenderingTest test`
- Manual viewport QA checklist (phone/tablet/desktop): sidebar toggle visibility and open/close, no horizontal overflow, row stacking readability, sticky-sidebar mobile accessibility, docs/javadocs no regression.

# Exit Criteria
- Mobile sidebar is operable on framework shell pages with sidebar.
- Multi-column content stacks cleanly on mobile without overflow regressions.
- Sticky sidebar remains usable on mobile with collapsed presentation.
- Targeted tests pass and snapshots are updated.
- Mobile rendering model is documented with clear framework-vs-consumer responsibility boundaries.
