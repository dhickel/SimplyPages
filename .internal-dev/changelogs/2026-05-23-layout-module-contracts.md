# 2026-05-23 Layout and Module Contracts

## Summary

Restored layout and module markup-to-CSS contract behavior with focused validation.

## Changes

- Added `CssClassNames` for token-aware class preservation and replacement.
- Updated `Row`, `Grid`, `Container`, and `Section` class mutators to preserve base/custom classes.
- Validated Row/Grid layout tokens against CSS-backed options.
- Added `PageBuilder.withStickySidebar(null)` fail-fast behavior.
- Added `Module.invalidateContent()` for supported post-render mutators.
- Made `TabsModule` fallback tab/panel IDs unique per module instance.
- Aligned `QuoteModule`, `TimelineModule`, and `StatsModule` emitted classes with framework CSS.
- Made `ComparisonModule` fail fast when row value counts do not match configured columns.
- Made `SimpleListModule` null-id safe and rebuilt after item add/remove mutations.
- Updated layout/module docs and package guidance.

## Validation

- `./mvnw -pl simplypages -Dtest=RowTest,ColumnTest,GridTest,ContainerTest,SectionTest,PageTest,AdditionalModulesTest,MoreModulesTest,SimpleListModuleTest,ModuleTest test`
- `./mvnw -pl simplypages test`
- `./mvnw -pl demo test`
