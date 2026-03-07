[Previous](02-layout-page-row-column-grid.md) | [Index](../INDEX.md)

# Template, RenderContext, and SlotKey Reference

## SlotKey

Typed key for context values.

```java
public static final SlotKey<String> TITLE = SlotKey.of("title");
public static final SlotKey<String> SUBTITLE = SlotKey.of("subtitle", "Default subtitle");
```

Notes:

- Equality is based on key name.
- Keep keys centralized to avoid accidental mismatches.

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

Render:

```java
String html = CARD_TEMPLATE.render(ctx);
```

Reuse guidance:

- `Template` is the preferred wrapper for reusing stable component/module structures.
- Configure/mutate structure first, then treat it as immutable and feed per-request values through `RenderContext`.

## Compile Policies

- `NEVER_COMPILE`: always resolve live entries.
- `COMPILE_ON_FIRST_HIT`: live slot values can be memoized into compiled entries for reused contexts.

Defaults provided in `SlotKey` are rendered live and are not persisted as compiled entries.
