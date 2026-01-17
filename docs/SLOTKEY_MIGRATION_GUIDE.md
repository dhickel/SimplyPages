# Migration Guide: Old to New SlotKey System

## Overview

The framework now uses immutable modules with `renderDynamic(SlotKeyMap)` for thread-safe rendering. Build your structure once and pass data at render time instead of mutating modules.

## Module Changes

### Before (Mutable)

```java
ContentModule module = ContentModule.create()
    .withTitle("My Title")
    .withContent("My Content");

String html = module.render();

// Later... (unsafe on shared instances)
module.setTitle("New Title");
String html2 = module.render();
```

### After (Immutable)

```java
// Build once (shared template)
ContentModule module = ContentModule.create()
    .withDefaultTitle("Default Title")
    .build();  // Immutable after this

// Render with data (thread-safe)
SlotKeyMap data1 = SlotKeyMap.create()
    .putString("title", "My Title")
    .putString("content", "My Content");
String html1 = module.renderDynamic(data1);

// Render again with different data (safe)
SlotKeyMap data2 = SlotKeyMap.create()
    .putString("title", "New Title");
String html2 = module.renderDynamic(data2);
```

## Custom Modules

If you have custom modules, follow this pattern:

1. Remove mutable fields (title, content, etc.)
2. Add `SlotKey` fields (final)
3. Remove setters that mutate state after build
4. Add build-time default setters (e.g., `withDefaultTitle`)
5. Register slots in `buildContent()`
6. Build component structure with slot references or render-time composition

Example:

```java
public class MyModule extends Module {
    private final SlotKey<String> titleSlot = SlotKey.of("title");

    public MyModule withDefaultTitle(String title) {
        setBuildTimeDefault("title", title);
        return this;
    }

    @Override
    protected void buildContent() {
        registerSlot("title", String.class, titleSlot, "Default");
        super.withChild(Header.H2().withInnerText(titleSlot));
    }
}
```

## Key Takeaways

- Modules are build-once and immutable after `build()`.
- Use `SlotKeyMap` + `renderDynamic` for per-request data.
- Avoid mutating module fields after build.
