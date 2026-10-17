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
Failed initial composition restores ordered shallow children, attributes and cached HTML id
and remains retryable. An explicit rebuild takes that snapshot before clearing children and also
restores prior build status on `RuntimeException` or `Error`, rethrowing the same failure. A
previously built module remains built and ordinary rendering does not retry the failed rebuild.
A later explicit successful rebuild replaces its structure.

The snapshot captures entry state, including intervening caller changes, not a historical deep
checkpoint. Module id, title, text/slots, trusted HTML, subclass/domain fields, descendant mutations
and external effects are outside this rollback boundary.

Modules do not size themselves through width helpers. Rows and columns determine placement,
so a module can be reused in different layouts. Edit methods that change built content need
an explicit rebuild; ordinary configuration setters are intended for use before building.

## What compilation preserves

A template records static tag output and leaves slots dynamic. It respects generated child
streams and builds modules before traversing them. Custom `render(RenderContext)` overrides
remain delegates because their behavior may depend on the request or may wrap the nominal
tag. Flattening their fields would lose that behavior.

Paragraph, Divider, EditableRow and EditablePage retain their render overrides as live delegates,
but derive temporary output without modifying stored attributes, children or configuration.
Later configuration therefore remains visible through their compiled templates. Paragraph/Divider
preserve inherited virtual child streams; editable wrappers retain generic inherited children
without emitting them. AccountWidget's generated stream and standard Page structure keep their
existing static-compilation behavior. Divider's exact reserved output namespace is documented in
the [catalog](../reference/components-and-modules-catalog.md#divider); cleanup is not a general
CSS normalization policy.

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
