# Context

Layout and modules have CSS contract drift: class mutators overwrite previous classes, Row/Grid tokens do not match CSS, `TabsModule` IDs collide, and several modules emit classes/styles that do not match framework CSS.

# Goal

Restore layout/module markup-to-CSS contract correctness with minimal valid-output drift and focused tests.

# In Scope

- Layout class composition for `Row`, `Column`, `Container`, `Section`, and `Grid`.
- CSS-backed token validation/normalization for gap/alignment/columns.
- `PageBuilder.withStickySidebar(null)` fail-fast behavior.
- `TabsModule` unique IDs / required module ID policy.
- `QuoteModule`, `TimelineModule`, `StatsModule`, `ComparisonModule`, and `SimpleListModule` correctness fixes.

# Out of Scope

- Theme redesign.
- Renaming public CSS classes unless aliases preserve compatibility.

# Implementation Steps

1. Layout helper.
   - Add token-aware class replacement helpers to avoid wiping custom classes.
   - Preserve base classes like `.row`, `.col`, `.container`, `.section`.
   - Validate or normalize tokens against CSS-backed options.

2. CSS contract tests.
   - Add tests that rendered classes exist in `framework.css` for Row/Grid gap/alignment where feasible.
   - Keep compatibility aliases in CSS if changing emitted class names would break existing users.

3. Sticky sidebar.
   - `withStickySidebar(null)` should throw `IllegalArgumentException`.
   - Add test proving content is not silently redirected/lost.

4. Modules.
   - `TabsModule`: generate instance-unique IDs or require `withModuleId` with clear error. Prefer generated stable per-instance IDs if valid output tests allow.
   - `QuoteModule`, `TimelineModule`, `StatsModule`: align emitted classes with CSS or add CSS aliases.
   - `ComparisonModule`: enforce row arity or pad deterministically; fail-fast is preferred if docs say rows must match columns.
   - `SimpleListModule`: null-safe id matching and rebuild/invalidation after mutators.

5. Docs.
   - Update `docs/core/02-layout-page-row-column-grid.md`.
   - Update module catalog for affected modules and exact token contracts.

# Validation

- Targeted tests:
  - `RowTest`, `ColumnTest`, `GridTest`, `ContainerTest`, `SectionTest`, `PageTest`.
  - `TabsModuleTest`, `QuoteModuleTest`, `TimelineModuleTest`, `StatsModuleTest`, `ComparisonModuleTest`, `SimpleListModuleTest`.
- `./mvnw -pl simplypages test`
- Before/after HTML diff notes for common layout and module examples.

# Exit Criteria

- Layout modifiers compose without dropping base/custom classes.
- Emitted module/layout classes match CSS or documented aliases.
- Multiple tab modules do not collide.
