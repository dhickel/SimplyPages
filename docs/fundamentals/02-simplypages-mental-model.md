[Previous](01-web-and-htmx-primer.md) | [Index](../INDEX.md)

# SimplyPages Mental Model

SimplyPages is a Java-first SSR framework.

The model is straightforward:

- Use components for primitives.
- Use modules for reusable composed sections.
- Use pages and layout containers for structure.
- Use templates + slots for request-time dynamic content.

## Core Types

- `Component`: renderable contract.
- `HtmlTag`: base tag implementation for most components.
- `Module`: build-once composed unit (`buildContent()` lifecycle).
- `RenderContext`: per-request slot data container.
- `SlotKey<T>`: typed dynamic value key.
- `Slot<T>`: dynamic placeholder bound to a `SlotKey`.
- `Template`: compiled render structure.

## Two Rendering Modes

1. Static composition:
- Build component tree once.
- Render once or on demand without slot data.

2. Dynamic slot-driven rendering:
- Define static `Template` and `SlotKey`s.
- Inject request-time data through `RenderContext`.

## Non-Negotiable Lifecycle Rule

`Module.build()` is idempotent and build-once.
Do not treat `buildContent()` as per-request mutation logic.

If data changes per request, put that data behind `SlotKey` and render through `Template`.

## Suggested Team Conventions

1. Keep `SlotKey` declarations as `public static final` constants.
2. Keep `Template` instances as `public static final` where possible.
3. Keep controllers thin: fetch domain data, map to context, render template.
4. Keep business logic out of component classes.
