[Previous](../getting-started/03-editing-system-first-implementation.md) | [Index](../INDEX.md)

# Components, HtmlTag, and Module Lifecycle

## Component

`Component` is the rendering contract.

- `render(RenderContext)` is the context-aware path.
- `render()` defaults to `RenderContext.empty()`.

## HtmlTag

`HtmlTag` is the base implementation for most concrete components.

Capabilities:

- Attributes via `withAttribute`.
- Children via `withChild`.
- Escaped text via `withInnerText`.
- Explicit unescaped HTML via `withUnsafeHtml`.

## Module

`Module` is for reusable composed sections.

Rules:

1. Build structure in `buildContent()`.
2. `build()` runs once (idempotent lifecycle).
3. Do not depend on `buildContent()` re-running per request.
4. Dynamic values belong in slots resolved at render time.

## Why Build-Once Matters

Build-once keeps module structure stable and predictable.
It also makes templates and render caching practical.

## Correct Pattern

- Build static structure once.
- Inject changing values via `RenderContext`.

## Incorrect Pattern

- Mutating module internals per request and expecting `buildContent()` to rebuild automatically.
