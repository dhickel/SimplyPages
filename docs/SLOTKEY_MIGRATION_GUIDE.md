# Migration Guide: Slot Runtime Refactor

## Overview

Template rendering now uses a mutable `RenderContext` as the canonical runtime store.
`RenderContext` can hold live slot values and compiled slot HTML entries, controlled by a render policy.

## What Changed

1. `RenderContext` is now mutable and request-scoped.
2. Slot entries are represented as explicit live/compiled variants internally.
3. Render policy is configured on `RenderContext`:
   - `RenderContext.RenderPolicy.NEVER_COMPILE`
   - `RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT`
4. `Template.render(context)` can compile explicit live slot values on first render when policy is enabled.
5. Slot defaults (`SlotKey.of(name, default)` and provider defaults) are rendered live and are not persisted as compiled entries.

## API Migration

### Before

```java
RenderContext context = RenderContext.builder()
    .with(TITLE, "My Title")
    .build();

String html = TEMPLATE.render(context);
```

### After (same baseline behavior)

```java
RenderContext context = RenderContext.builder()
    .with(TITLE, "My Title")
    .build();

String html = TEMPLATE.render(context); // default policy: NEVER_COMPILE
```

### Enable compile-on-first-hit

```java
RenderContext context = RenderContext.builder()
    .withPolicy(RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT)
    .with(TITLE, "My Title")
    .with(CONTENT, contentComponent)
    .build();

String first = TEMPLATE.render(context);  // renders live, then compiles explicit entries
String second = TEMPLATE.render(context); // uses compiled entries when present
```

### Invalidate compiled slots by writing live values

```java
context.put(TITLE, "Updated Title"); // replaces prior compiled/live entry for TITLE
String html = TEMPLATE.render(context);
```

### Manual compiled entry injection

```java
context.putCompiled(CONTENT, "<div>Pre-rendered trusted html</div>");
```

## `SlotKeyMap` Status

`SlotKeyMap` remains available as a legacy utility and bridge.
It is not the canonical runtime rendering model.

Bridge helpers:

```java
SlotKeyMap map = SlotKeyMap.fromRenderContext(context);

RenderContext rebuilt = map.toRenderContext(Map.of(
    "title", TITLE,
    "content", CONTENT
));
```

## Checklist for Existing Code

1. Continue using `Template.render(RenderContext)` as the main render entrypoint.
2. If you want caching behavior, set policy to `COMPILE_ON_FIRST_HIT`.
3. Use `put(...)` to update values and invalidate old compiled entries.
4. Keep defaults for fallback behavior, but do not rely on defaults being compiled/persisted.

