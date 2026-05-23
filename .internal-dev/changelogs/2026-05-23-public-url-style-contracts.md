# 2026-05-23 Public URL and Style Contracts

## Summary

Hardened public URL and inline-style contracts across link-like components, builders, and modules.

## Changes

- Added `SafeUrl` as the shared core URL validation helper.
- Updated `Link`, `NavBar`, `SideNav`, `Breadcrumb`, `Dropdown`, `AccountWidget`, `TopNavBuilder`,
  `HeroModule`, and related builders to validate href-like values consistently.
- Replaced raw background/style string assembly in `HeroModule`, `BannerBuilder`, and
  `AccountBarBuilder` with hardened style helpers.
- Added constrained CSS image URL validation for generated `background-image: url(...)` values.
- Escaped `Form.withHxPostCsrf(...)` header names and token values as JSON before attribute rendering.
- Updated security/reference docs and package-level `AGENTS.md` guidance for URL/style contracts.

## Validation

- `./mvnw -pl simplypages -Dtest=SafeUrlTest,LinkTest,NavBarTest,SideNavTest,BreadcrumbTest,DropdownTest,AccountWidgetTest,HeroModuleTest,FormTest,BannerBuilderTest,AccountBarBuilderTest,TopNavBuilderTest test`
- `./mvnw -pl simplypages test`
- `./mvnw -pl demo test`
