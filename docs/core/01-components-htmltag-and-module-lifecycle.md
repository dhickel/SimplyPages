[Previous](../getting-started/03-editing-system-first-implementation.md) | [Index](../INDEX.md)

# Components, HtmlTag, and Module Lifecycle

## Thread Safety First

`HtmlTag`, `Module`, and `RenderContext` are mutable and not thread-safe while being configured.

- Build/mutate component trees and contexts in a single request flow.
- Do not share mutable instances across concurrent requests.
- Reuse only stable structures (typically via `Template`) and pass per-request values through `RenderContext`.

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
- HTMX aliases via `hxGet`, `hxPost`, `hxPut`, `hxPatch`, `hxDelete`, `hxTarget`, `hxSwap`, `hxTrigger`, `hxInclude`, `hxPushUrl`.

Example:

```java
HtmlTag panel = new HtmlTag("div")
    .hxGet("/orders/42/details")
    .hxTarget("#content-area")
    .hxSwap("innerHTML");
```

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
