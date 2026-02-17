# Core Package Agent Guide

## Purpose
Owns rendering primitives and contracts used by the whole framework.

## Owns
- `Component`, `HtmlTag`, `Module`
- `RenderContext`, `SlotKey`, `Slot`, `SlotKeyMap`
- `Template`, `TemplateComponent`
- `Attribute`, `Style`

## Invariants
- Rendering must be safe by default for untrusted text/attributes.
- `render(RenderContext)` behavior must remain compatible with `render()` defaults.
- `Module.build()` is idempotent and drives build-once lifecycle semantics.
- Slot/template rendering must preserve type-safe key usage.

## Do
- Add core API only when needed by multiple packages.
- Keep low-level logic deterministic and side-effect minimal.
- Document behavior changes in Javadocs and tests.

## Do Not
- Put domain-specific UI logic here.
- Bypass escape/encoding paths for untrusted data.
- Break existing render path contracts without migration notes.

## Common Pitfalls
- Mixing per-request mutable state into built module structure.
- Changing render order semantics unintentionally.
- Breaking default `RenderContext.empty()` behavior.

## Required Tests
- `HtmlTagTest`, `ModuleTest`, `RenderContextTest`, `TemplateTest`, `SlotKeyTest`
- Escaping/attribute safety regressions
- Slot/default value semantics

## Dependencies
- Keep `core` dependency-light and foundational.
- Other packages may depend on `core`; avoid reverse dependencies.

## Maintenance Requirement
Keep this file updated whenever core contracts or invariants change.

See root `AGENTS.md` for global standards.
