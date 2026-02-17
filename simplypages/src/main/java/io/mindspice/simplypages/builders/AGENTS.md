# Builders Package Agent Guide

## Purpose
Owns higher-level shell and navigation builder utilities.

## Owns
- `ShellBuilder`, `BannerBuilder`
- `AccountBarBuilder`, `TopNavBuilder`, `SideNavBuilder`

## Invariants
- Builders remain fluent and simple to compose.
- `build()` output shape stays stable for consumers.
- Default values keep generated output functional.

## Do
- Keep generated markup predictable and integration-friendly.
- Validate required fields where absence would break output.
- Preserve backward-compatible defaults when adding options.

## Do Not
- Push unrelated business logic into builders.
- Depend on demo routes unless explicitly demo-only and documented.

## Common Pitfalls
- Breaking CSS class hooks used by framework styles.
- Changing default IDs/targets unexpectedly.
- Regressing HTMX bootstrap behavior in shell output.

## Required Tests
- `ShellBuilderTest`, `BannerBuilderTest`
- `TopNavBuilderTest`, `SideNavBuilderTest`, `AccountBarBuilderTest`

## Dependencies
- Depends on `core`, `components`, and `layout` only as needed.
- Keep builders reusable outside demo app.

## Maintenance Requirement
Keep this file updated whenever builder defaults, markup contracts, or integration hooks change.

See root `AGENTS.md` for global standards.
