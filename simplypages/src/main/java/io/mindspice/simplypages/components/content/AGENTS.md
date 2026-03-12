# Content Helper Package Agent Guide

## Purpose
Owns opinionated static markdown content helper contracts and rendering pipelines.

## Owns
- Directory-driven static content generation (`StaticContentSiteBuilder`, `StaticContentSite`)
- Frontmatter parsing and route-index contracts
- Default list/detail rendering contracts for markdown sections

## Invariants
- Generation is stateless per call and does not keep mutable shared runtime state.
- User applications own cache/refresh lifecycle and endpoint wiring.
- Frontmatter parsing is best-effort; invalid values emit warnings instead of failing generation.

## Do
- Keep section/list/detail contracts composable and framework-safe by default.
- Preserve markdown HTML escaping defaults unless explicitly configured unsafe.
- Add targeted tests for parsing, pagination, routing, and rendering structure.

## Do Not
- Introduce app-specific persistence or long-lived cache state in helper classes.
- Couple helper output to demo-only routes/controllers.

## Required Tests
- `simplypages/src/test/java/.../components/content`
- Structural assertions for route/page output and sticky TOC behavior

## Maintenance Requirement
Keep this file updated whenever content helper contracts or boundaries change.

See root `AGENTS.md` for global standards.
