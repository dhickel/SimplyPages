# Navigation Components Agent Guide

## Purpose
Owns navigation primitives and reusable nav structures.

## Owns
- `Link`, `NavBar`, `SideNav`, `Breadcrumb`

## Invariants
- Navigation components remain semantic and link-safe.
- Active/selected state APIs produce stable class and attribute output.
- Side navigation structure remains builder-friendly.

## Do
- Keep navigation APIs concise and predictable.
- Preserve compatibility with builders relying on nav components.
- Add tests for active state rendering and link attributes.

## Do Not
- Embed app route policies in component layer.
- Break class hooks used by framework CSS/builders.

## Common Pitfalls
- Inconsistent active class naming.
- Regressing nested nav/section structures.
- Generating malformed URLs/attributes.

## Required Tests
- `LinkTest`, `NavBarTest`, `SideNavTest`, `BreadcrumbTest`
- Regression tests for new nav states/options

## Dependencies
- Depends on `core` and base components.
- Keep no dependency on demo logic.

## Maintenance Requirement
Keep this file updated whenever navigation contracts or class/state behavior changes.

See root `AGENTS.md` for global standards.

Update this file in the same change whenever package-level behavior or conventions drift.
