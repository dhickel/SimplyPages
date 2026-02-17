[Previous](03-template-rendercontext-slotkey-reference.md) | [Index](../INDEX.md)

# Rendering Pipeline: High and Low Level

This page explains how SimplyPages turns Java objects into HTML.

## High-Level Pipeline

1. Controller receives request.
2. Service loads domain data.
3. Controller maps data to `RenderContext`.
4. `Template` or `Component` renders HTML.
5. Response returns full page or fragment.

```mermaid
sequenceDiagram
    participant B as Browser
    participant C as Controller
    participant S as Service
    participant T as Template/Component

    B->>C: HTTP request
    C->>S: Load domain data
    S-->>C: Domain object(s)
    C->>T: render(context)
    T-->>C: HTML
    C-->>B: HTTP response (HTML)
```

## Low-Level: Template Compilation

`Template.of(root)` compiles a component tree into segments:

- Static string segments
- Dynamic slot segments (`SlotKey` lookups)
- Opaque component segments (fallback component rendering)

Modules are built before compilation (`module.build()`).

## Low-Level: Slot Resolution

During `template.render(context)`:

1. For each segment, renderer appends output to a single `StringBuilder`.
2. For a slot segment:
- if entry is compiled, append compiled HTML
- if entry is live, render value now
- if key missing, evaluate slot default provider
3. Text values are escaped; component values render as nested components.

## Compile-On-First-Hit Behavior

With `RenderPolicy.COMPILE_ON_FIRST_HIT`, explicit live slot entries can be persisted as compiled entries in that context object.

```mermaid
sequenceDiagram
    participant App
    participant T as Template
    participant RC as RenderContext

    App->>T: render(RC)
    T->>RC: getEntry(slotKey)
    RC-->>T: LiveEntry(value)
    T->>T: render value -> html
    T->>RC: putCompiled(slotKey, html)
    T-->>App: html

    App->>T: render(RC) again
    T->>RC: getEntry(slotKey)
    RC-->>T: CompiledEntry(html)
    T-->>App: html (no re-render)
```

## Escaping Model

- `withInnerText(...)` escapes text.
- `Slot` text values are escaped.
- `withUnsafeHtml(...)` bypasses escaping and must only receive trusted content.

## HTMX Edit OOB Flow

```mermaid
sequenceDiagram
    participant U as User
    participant B as Browser/HTMX
    participant E as Edit Endpoint

    U->>B: Click Save
    B->>E: POST /modules/{id}/save
    E-->>B: OOB HTML (close modal + update module)
    B->>B: Apply OOB swaps
```

## Practical Debug Checklist

1. Wrong value: verify `SlotKey` names and context keys match.
2. Stale value: check context reuse and compile policy.
3. Escaping surprises: confirm `withInnerText` vs `withUnsafeHtml` usage.
4. Missing module content: verify lifecycle assumptions around `buildContent()`.
