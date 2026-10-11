# Rendering, lifecycle, and reuse

SimplyPages separates the shape of a UI from values supplied for a particular request.
Java components describe structure; `RenderContext` carries request data; rendering produces
HTML that can be returned as a full document or a fragment.

## Components and modules

A component is any object implementing `render(RenderContext)`. `HtmlTag` handles the common
case: an element, attributes, text, and ordered children. Components such as selects and
cards can generate children through `getChildrenStream()`.

A module composes several components into a feature. Its `buildContent()` runs through the
lazy `build()` guard and should establish structure. Successful builds are idempotent.
After a failed build, the framework restores its child and attribute collections for a
retry; a subclass still owns rollback of any external side effects or its own mutable fields.

Modules do not size themselves through width helpers. Rows and columns determine placement,
so a module can be reused in different layouts. Edit methods that change built content need
an explicit rebuild; ordinary configuration setters are intended for use before building.

## What compilation preserves

A template records static tag output and leaves slots dynamic. It respects generated child
streams and builds modules before traversing them. Custom `render(RenderContext)` overrides
remain delegates because their behavior may depend on the request or may wrap the nominal
tag. Flattening their fields would lose that behavior.

This distinction matters for sharing. A compiled template can retain mutable custom
components. A fresh context isolates slot data, but cannot make a stateful delegate
thread-safe. Configure ordinary components before sharing, avoid render-time mutation in
custom components, and cache rendered strings when you need guaranteed immutable output.

## Caching is an explicit choice

With the default policy, slot values render live. `COMPILE_ON_FIRST_HIT` stores the first
rendered output in that context. This can save work inside a confined render flow but also
freezes dependencies that contributed to that output. It is unsuitable for a shared context
carrying different users' request data.

A template is a render plan; a rendered string is an output cache. Applications own cache
keys and invalidation, including user, tenant, authorization, and content revision boundaries.

## Server rendering and HTMX

HTMX requests HTML from the server and swaps it into a target. It fits the same component
model because a fragment is already renderable output. The server still owns validation and
state changes; the client controls where the resulting markup appears. Choosing `outerHTML`
or `innerHTML` is part of the response contract, including whether the target wrapper is
present in the response.

[Rendering reference](../reference/rendering.md) · [All explanation](README.md)
