[Previous](02-layout-page-row-column-grid.md) | [Index](../INDEX.md)

# Template, RenderContext, and SlotKey Reference

## SlotKey

Typed key for context values.

```java
public static final SlotKey<String> TITLE = SlotKey.of("title");
public static final SlotKey<String> SUBTITLE = SlotKey.of("subtitle", "Default subtitle");
```

Notes:

- The canonical constructor and all `SlotKey.of` factories throw `NullPointerException` for a null name at construction, rather than during later hash or map use.
- Every non-null name is preserved literally, including empty and blank strings. Names are not trimmed, case-folded, or Unicode-normalized.
- Default providers may be null and are not evaluated during construction.
- Equality and hash code use only the name, not the generic type or default provider.
- Keep keys centralized with a consistent type for each name to avoid accidental mismatches.

## Slot

`Slot.of(key)` inserts a dynamic placeholder.

- If slot value is a `Component`, component rendering is used.
- Otherwise value is escaped and rendered as text.
- Resolution happens in both direct `Component.render(context)` and `Template.render(context)` flows.

## RenderContext

Mutable value container for slot resolution.

```java
RenderContext ctx = RenderContext.builder()
    .with(TITLE, "Quarterly Report")
    .withPolicy(RenderContext.RenderPolicy.NEVER_COMPILE)
    .build();
```

Single-slot convenience for common cases:

```java
RenderContext ctx = RenderContext.of(TITLE, "Quarterly Report");
```

Methods you will use most:

- `put(key, value)`
- `remove(key)`
- `clear()`
- `withPolicy(...)`

Reuse guidance:

- Default: create per request/render flow.
- Reuse is valid only with confined access (no concurrent mutation).

## Template

Compiled rendering structure.

```java
public static final Template CARD_TEMPLATE = Template.of(
    new Div().withClass("card")
        .withChild(Header.H3("").withInnerText(TITLE))
        .withChild(new Paragraph().withChild(Slot.of(SUBTITLE)))
);
```

Equivalent shortcut:

```java
Template CARD_TEMPLATE = someComponentTree.compile();
```

Render:

```java
String html = CARD_TEMPLATE.render(ctx);
```

Map-based convenience:

```java
String html = CARD_TEMPLATE.render(Map.of(TITLE, "Quarterly Report"));
String html2 = someComponentTree.render(Map.of(TITLE, "Quarterly Report"));
```

Reuse guidance:

- `Template` is the preferred wrapper for reusing stable component/module structures.
- Configure/mutate structure first, then treat it as immutable and feed per-request values through `RenderContext`.

## Compile Policies

- `NEVER_COMPILE`: always resolve live entries.
- `COMPILE_ON_FIRST_HIT`: live slot values can be memoized into compiled entries for reused contexts.

Defaults provided in `SlotKey` are rendered live and are not persisted as compiled entries.

## Direct and compiled rendering parity

Both paths render explicit compiled entries as trusted HTML. `NEVER_COMPILE` prevents new
cache entries; it does not ignore an existing compiled entry. `get(key)` returns live typed
values or defaults, while `getCompiled(key)` retrieves cached HTML.

Compilation preserves generated child streams and delegates custom `render(RenderContext)`
overrides. Standard `Column` structure is compiled as ordinary tag structure, with its base
class already established during configuration; no warm-up render is required. A custom
`Column` subclass render override still remains a live delegate.

Templates can therefore retain mutable components, including framework components with
custom renderers; share only when those delegates are safe to share. Contexts and slot-provided
components remain request-owned. See the [rendering reference](../reference/rendering.md).
