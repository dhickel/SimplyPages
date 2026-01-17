# Rendering System Refactoring

This document outlines the changes to the rendering system introduced to support stateless component reuse, dynamic slots, and template pre-compilation.

## Core Concepts

### 1. Separation of Static Structure and Dynamic Data
Previously, components like `Module` often mixed static configuration (title, layout) with per-request dynamic data (user specific content). The new system separates these:
- **Static Structure:** Defined at construction time. Immutable after building.
- **Dynamic Data:** Passed at render time via `RenderContext`.

### 2. SlotKey and RenderContext
- **`SlotKey<T>`**: A typed key representing a dynamic insertion point. It can have a default value or provider.
- **`RenderContext`**: An immutable map-like container holding values for `SlotKey`s for a specific request.

### 3. Slots
- **`Slot` Component**: A component that renders a value from the `RenderContext` based on a `SlotKey`.
- **Dynamic Inner Text**: `HtmlTag` now supports `withInnerText(SlotKey<String>)` for dynamic text content without creating a full child component.

### 4. Template Compilation
- **`Template`**: A class that "compiles" a component tree into a linear sequence of static string segments and dynamic slot lookups.
- **Optimization**: This avoids traversing the component tree on every request. Static parts are pre-rendered into strings, and only dynamic slots are evaluated at request time.

## Usage Patterns

### Defining Slots
```java
public class MyPage {
    // Define typed keys
    public static final SlotKey<String> USER_NAME = SlotKey.of("userName", "Guest");
    public static final SlotKey<Component> MAIN_CONTENT = SlotKey.of("mainContent");
}
```

### Building a Template
Build the page structure once (e.g., at application startup) using `Slot`s for dynamic parts.

```java
// Static structure
Page page = Page.builder()
    .addComponents(Header.H1("Welcome, ").withInnerText(MyPage.USER_NAME))
    .addRow(row -> row.withChild(
        Slot.of(MyPage.MAIN_CONTENT)
    ))
    .build();

// Compile to template
Template pageTemplate = Template.of(page);
```

### Rendering with Context
At request time, build a context with the specific data and render the template.

```java
@GetMapping("/dashboard")
public String dashboard(@RequestParam String user) {
    RenderContext context = RenderContext.builder()
        .with(MyPage.USER_NAME, user)
        .with(MyPage.MAIN_CONTENT, new Card().withBody("Your Dashboard"))
        .build();

    return pageTemplate.render(context);
}
```

## Changes to Module System

### Module Lifecycle
- **Old Behavior:** `Module.render()` would clear children and call `buildContent()` every time. This made modules mutable and not thread-safe for reuse if they depended on internal state that changed.
- **New Behavior:** `Module` now follows a "build-once" pattern.
    - `build()`: Populates the children list. Idempotent (only runs once).
    - `render(context)`: Calls `build()` if needed, then renders children using the context.
    - **Implication:** You can now reuse a `Module` instance across multiple requests if it only contains static structure and `Slot`s.

### Migration Guide
Existing code works without changes because `render()` (no-args) calls `render(RenderContext.empty())`.

To modernize a Module:
1. Move per-request data from instance fields to `SlotKey`s.
2. Use `Slot.of(KEY)` in `buildContent()` instead of using the instance field directly.
3. Pass the values via `RenderContext` when rendering.

**Example Refactor:**

*Old:*
```java
public class WelcomeModule extends Module {
    private String username;
    public WelcomeModule withUsername(String name) { this.username = name; return this; }
    protected void buildContent() {
        withChild(new Paragraph("Hello " + username));
    }
}
// Usage: new WelcomeModule().withUsername("Alice").render();
```

*New:*
```java
public class WelcomeModule extends Module {
    public static final SlotKey<String> USERNAME = SlotKey.of("username");

    protected void buildContent() {
        // Use slot for dynamic content
        withChild(new Paragraph("Hello "));
        withChild(Slot.of(USERNAME));
    }
}
// Usage:
// static WelcomeModule MODULE = new WelcomeModule(); // Reuse this
// MODULE.render(RenderContext.builder().with(WelcomeModule.USERNAME, "Alice").build());
```

## Performance
Using `Template` significantly reduces overhead for complex pages by pre-calculating the static HTML strings. Only the dynamic slots incur rendering cost at request time.
