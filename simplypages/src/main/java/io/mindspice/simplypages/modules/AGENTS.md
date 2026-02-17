# Modules Package Agent Guide

## Purpose
Owns high-level composed modules built from components/layout primitives.

## Owns
- Content/data/form/forum/media modules
- Dynamic modules and editable wrappers (`EditableModule`)
- Opinionated feature modules (hero, stats, timeline, tabs, accordion, quote, callout, comparison)

## Invariants
- Modules compose structure in `buildContent()`.
- Module lifecycle uses `build()` idempotently.
- Module direct width APIs are blocked; layout should size modules.
- Public fluent APIs remain chainable and readable.

## Do
- Keep each module focused on one UI concern.
- Compose existing components instead of custom HTML assembly where possible.
- Expose sensible defaults and explicit configuration methods.

## Do Not
- Re-introduce mutable render-time structure hacks.
- Duplicate core escaping behavior inside module code unless necessary.

## Common Pitfalls
- Mutating children post-build without rebuild semantics.
- Assuming width methods on modules are supported.
- Creating tight coupling to demo controllers/routes.

## Required Tests
- Module tests in `simplypages/src/test/java/.../modules`
- Lifecycle regressions (`ModuleLifecycleTest`)
- Editing wrapper regressions when changing `EditableModule`

## Dependencies
- Depend on `core`, `components`, `layout`, and optional `editing` helpers.
- Avoid depending on demo package code.

## Maintenance Requirement
Keep this file updated whenever module lifecycle, API surface, or module catalog changes.

See root `AGENTS.md` for global standards.
