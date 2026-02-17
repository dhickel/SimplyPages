# Display Components Agent Guide

## Purpose
Owns display-oriented UI components and visual containers.

## Owns
- `Alert`, `Badge`, `Card`, `CardGrid`, `DataTable`, `InfoBox`, `Label`, `Modal`
- `OrderedList`, `UnorderedList`, `ProgressBar`, `Spinner`, `Table`, `Tag`

## Invariants
- Display components remain composable and class-driven.
- `DataTable` and `Table` rendering contracts remain stable.
- Modal behavior and IDs remain predictable/safe.

## Do
- Keep display APIs concise with sensible presets.
- Preserve semantic HTML output and class naming stability.
- Validate user-controlled identifiers where used in JS hooks.

## Do Not
- Couple display components to application routing/state.
- Assume trusted content unless explicitly designated as trusted.

## Common Pitfalls
- Breaking CSS contract class names used by framework styles.
- Changing modal container behavior without integration tests.
- Regressing table/list rendering order.

## Required Tests
- `MarkdownSecurityTest`, `MarkdownTest`
- `Modal`-related and display component tests in `.../components`
- New behavior needs focused render assertions

## Dependencies
- Depends on `core` and base components.
- Keep independent from demo-only concerns.

## Maintenance Requirement
Keep this file updated whenever display component contracts, classes, or modal behavior change.

See root `AGENTS.md` for global standards.
