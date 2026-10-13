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
- Component implementations must provide `render(RenderContext)`.
- `compile()` is a convenience alias for `Template.of(component)`.
- `render(Map<SlotKey<?>, Object>)` is available for simple map-driven rendering.

## HtmlTag

`HtmlTag` is the base implementation for most concrete components.

Capabilities:

- Attributes via `withAttribute`.
- Children via `withChild`.
- Escaped text via `withInnerText`.
- Explicit unescaped HTML via `withUnsafeHtml`.
- HTMX aliases via `hxGet`, `hxPost`, `hxPut`, `hxPatch`, `hxDelete`, `hxTarget`, `hxSwap`, `hxTrigger`, `hxInclude`, `hxPushUrl`, `hxScrollTargetTop`.

Example:

```java
HtmlTag panel = new HtmlTag("div")
    .hxGet("/orders/42/details")
    .hxTarget("#content-area")
    .hxSwap("innerHTML")
    .hxScrollTargetTop();
```

## Module

`Module` is for reusable composed sections.

Rules:

1. Build structure in `buildContent()`.
2. `build()` runs once (idempotent lifecycle).
3. Do not depend on `buildContent()` re-running per request.
4. Dynamic values belong in slots resolved at render time.

### Failed builds and explicit rebuilds

An initial failed build restores entry-state ordered children, attributes and cached HTML id,
then remains retryable. A subclass can explicitly replace structure with `rebuildContent()`.
If that rebuild throws `RuntimeException` or `Error`, the same failure is rethrown after restoring
those collections, cached id and prior build status. A previously built module therefore stays
built: ordinary rendering uses its restored structure without automatically retrying the failure.
A later explicit successful rebuild replaces the structure normally.

This is a **shallow entry-state snapshot**, including caller changes made before rebuilding,
not a historical checkpoint or general transaction. It does not restore module id, title,
text/slots, trusted HTML, subclass/domain fields, descendant mutations or external effects.
Subclasses own those boundaries. Configure before building; ordinary setters do not rebuild.

## Readonly rendering and live delegates

`Paragraph`, `Divider`, `EditableRow` and `EditablePage` derive temporary rendering state without
changing their stored attributes, children or configuration. Paragraph and Divider still emit
inherited children through their virtual child streams. The editable wrappers emit only their
supplied/generated composition; generic inherited children stay stored but remain un-emitted.

These renderers remain live template delegates. Configuration changes after compilation affect
their next output, unlike ordinary static tag segments captured at compile time. Finish mutable
configuration before sharing, use an independent context per request, and ensure custom delegates
and slot-provided components are independently safe to share. This is not a universal concurrency
guarantee. See the [Divider reserved styling contract](../reference/components-and-modules-catalog.md#divider).

## Fluent return types

Fluent methods retain their declared return types, not a universal self type. For example,
`Column.withAttribute(String, String)` returns `HtmlTag`, including for legacy subclasses;
`Column.withWidth(int)` returns `Column`. Class setters also differ: Column layout/class setters
replace classes, while `addClass` appends. Modules append classes and reject their direct width,
minimum-width and maximum-width helpers; layout containers control sizing.

## Why Build-Once Matters

Build-once keeps module structure stable and predictable.
It also makes templates and render caching practical.

## Correct Pattern

- Build static structure once.
- Inject changing values via `RenderContext`.

## Incorrect Pattern

- Mutating module internals per request and expecting `buildContent()` to rebuild automatically.
