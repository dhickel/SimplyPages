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

- Built StaticContentSite instances snapshot section configuration.
- Slug and heading-ID collision handling must include already suffixed source values.
- Root-mounted sections must emit local /slug routes, never protocol-relative //slug links.
- Published final detail routes matching another section's index path emit nonfatal ContentWarning diagnostics, one per owner pair, in stable configuration/entry order. Include both section keys, detail source file, index source directory, detail priority and index pagination masking.
- Preserve detail-before-index request resolution, including page queries, and retain all generated maps/components and direct section lookups. Applications inspect bundle warnings; diagnostics do not fail generation or reassign routes.
- Drafts and pre-uniquified slugs must not create collision diagnostics. Duplicate normalized bases still reject at configuration; do not add generic URI normalization or public route-owner APIs.

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
- Public root/nested detail-index collisions, both registration orders, query/trailing-slash resolution versus direct component identity, repeatable warnings, adjusted slugs, drafts and noncollisions

## Maintenance Requirement
Keep this file updated whenever content helper contracts or boundaries change.

See root `AGENTS.md` for global standards.
