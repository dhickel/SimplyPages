# Components Package Agent Guide

## Purpose
Owns shared low-level UI components and base component conventions.

## Owns
- Generic components in this package (`Div`, `Header`, `Paragraph`, `Markdown`, etc.)
- Cross-cutting component behavior not specific to subpackages

## Invariants
- Components follow `HtmlTag` extension patterns consistently.
- `create()` factory methods and fluent return types stay consistent.
- Attribute and text rendering remains safely escaped by default.

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
