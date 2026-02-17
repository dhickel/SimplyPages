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

## Do
- Keep layout APIs fluent and simple.
- Validate width/shape constraints at API boundaries.
- Preserve backward-compatible CSS class semantics where possible.

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

## Dependencies
- Depend on `core` and generic components only.
- Do not introduce dependencies on `modules` or demo-specific code.

## Maintenance Requirement
Keep this file updated whenever layout behavior, constraints, or breakpoints change.

See root `AGENTS.md` for global standards.
