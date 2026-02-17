[Previous](../security/02-authwrapper-authorizationchecker-integration.md) | [Index](../INDEX.md)

# Performance, Threading, and Cache Lifecycles

## Core Threading Rule

Framework components and modules are mutable objects.
Treat them as request-scoped unless you explicitly freeze output into cached HTML or stable templates.

## Safe Reuse Matrix

- Safe to reuse globally:
  - `Template` instances
  - immutable config values
- Reuse with care:
  - `RenderContext` only when lifecycle is explicit and synchronized by your app design
- Do not reuse across concurrent requests:
  - mutable `Module` or `HtmlTag` instances being edited/mutated

## Preferred Performance Pattern

1. Keep structure static in `Template`.
2. Map request data into context.
3. Render fragments, not whole pages, for HTMX updates.

## Cache Lifecycles

Use clear invalidation triggers:

- content revision change
- tenant config change
- deployment/version rollover

Pseudo strategy:

```text
startup: compile templates and warm static html where useful
request: fetch data -> context -> render
mutation: invalidate keys affected by changed domain objects
```

## Avoidable Bottlenecks

1. Rebuilding identical templates in request handlers.
2. Re-rendering full shells when only one module changed.
3. Sharing mutable module instances across request threads.
