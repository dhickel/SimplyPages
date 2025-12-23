# Part 13: Templates and Dynamic Updates

As your application grows, you'll encounter scenarios where simple component composition isn't efficient enough, particularly for:
1.  **High-performance rendering** of complex pages.
2.  **Dynamic updates** where parts of a module need to change without rebuilding the whole structure.
3.  **Out-of-Band (OOB) updates** with HTMX where you need to update multiple page sections at once.

JHF introduces **Templates**, **SlotKeys**, and **RenderContext** to solve these problems by separating **static structure** from **dynamic data**.

## Core Concepts

### 1. Static vs. Dynamic
In standard JHF components, structure and data are often mixed:
```java
// Structure and data mixed
Paragraph p = new Paragraph().withInnerText("Hello " + username);
```
Every time you render this, JHF builds the component object and generates the HTML string.

With **Templates**, we separate them:
- **Template**: The static HTML structure (compiled once).
- **Slot**: A placeholder for dynamic content.
- **RenderContext**: The data to fill the slots (provided at render time).

### 2. SlotKey
A `SlotKey<T>` is a typed identifier for a dynamic value.

```java
public class Dashboard {
    // Define keys for dynamic parts
    public static final SlotKey<String> USERNAME = SlotKey.of("username");
    public static final SlotKey<Component> RECENT_ITEMS = SlotKey.of("recent_items");
}
```

### 3. Template
A `Template` takes a component tree, "compiles" it into a static string with placeholders, and allows fast rendering.

```java
// 1. Build structure using Slots
Module userModule = ContentModule.create()
    .withTitle("User Profile")
    .withChild(new Paragraph().withInnerText("Welcome, "))
    .withChild(Slot.of(Dashboard.USERNAME)); // Dynamic part

// 2. Compile to Template (do this once, e.g., static field)
public static final Template USER_TEMPLATE = Template.of(userModule);
```

### 4. RenderContext
To render a template, you provide a context containing the values.

```java
// 3. Render with data
String html = USER_TEMPLATE.render(
    RenderContext.builder()
        .with(Dashboard.USERNAME, "Alice")
        .build()
);
```

## Why Use Templates?

### 1. Performance
Templates pre-compute the static HTML strings. When you call `render()`, JHF simply concatenates the static strings with the values from the context. This avoids traversing the component tree and regenerating attributes for every request.

### 2. Immutability & Thread Safety
Modules in JHF now follow a **build-once** lifecycle. Once `build()` is called (which happens automatically when creating a Template), the module structure is locked. This makes Templates thread-safe and reusable across requests.

### 3. HTMX Out-of-Band (OOB) Swaps
Templates are the best way to handle complex HTMX updates.

## Dynamic Updates with HTMX and Templates

A common pattern with HTMX is to update a specific module on the page without reloading the whole page. Because Modules are immutable after building, you **cannot** simply change a field on an existing Module instance and re-render it.

Instead, you use a **Template** and render it with new data.

### Example: Live Updating Widget

**Scenario**: A "Server Status" widget that updates every 5 seconds.

#### 1. Define the SlotKeys
```java
public class ServerStatusPage {
    public static final SlotKey<String> CPU_LOAD = SlotKey.of("cpu");
    public static final SlotKey<String> MEMORY_USAGE = SlotKey.of("memory");
}
```

#### 2. Create the Template
Note the `hx-swap-oob="true"` attribute. This tells HTMX to find the element with the matching ID on the page and swap it, even if the response contains other content.

```java
public class ServerStatusPage {
    // ... keys ...

    public static final Template STATUS_WIDGET = Template.of(
        ContentModule.create()
            .withModuleId("server-status-widget") // Fixed ID
            .withTitle("Server Status")
            .withChild(new Div()
                .withChild(new Span().withInnerText("CPU: "))
                .withChild(Slot.of(CPU_LOAD)) // Slot
                .withChild(new Br())
                .withChild(new Span().withInnerText("Mem: "))
                .withChild(Slot.of(MEMORY_USAGE))) // Slot
            .withAttribute("hx-swap-oob", "true") // Enable OOB swap
    );
}
```

#### 3. Initial Render (The Page)
When the user visits the page, render the template with initial data.

```java
@GetMapping("/dashboard")
public String dashboard() {
    Page page = Page.create()
        .addComponents(Header.h1("Dashboard"))
        .addRow(row -> row.withChild(
            // Render the template as a component
            new Component() {
                @Override
                public String render(RenderContext ctx) {
                    return ServerStatusPage.STATUS_WIDGET.render(
                        RenderContext.builder()
                            .with(ServerStatusPage.CPU_LOAD, "0%")
                            .with(ServerStatusPage.MEMORY_USAGE, "0GB")
                            .build()
                    );
                }
            }
        ));

    return page.render();
}
```

#### 4. The Update Endpoint
When HTMX polls for updates, simply render the **Template** with new data.

```java
@GetMapping("/api/status-update")
@ResponseBody
public String updateStatus() {
    // Fetch live data...
    String cpu = getCpuLoad();
    String mem = getMemoryUsage();

    // Render ONLY the widget template
    return ServerStatusPage.STATUS_WIDGET.render(
        RenderContext.builder()
            .with(ServerStatusPage.CPU_LOAD, cpu)
            .with(ServerStatusPage.MEMORY_USAGE, mem)
            .build()
    );
}
```

The response will look like:
```html
<div id="server-status-widget" class="module ..." hx-swap-oob="true">
   ... updated content ...
</div>
```
HTMX sees `hx-swap-oob="true"` and `id="server-status-widget"`, finds the existing widget on the page, and swaps it.

## Comparison: Pages vs. Modules vs. Templates

There can be confusion about where each concept fits. Here is the breakdown:

| Feature | **Page** | **Module** | **Template** |
| :--- | :--- | :--- | :--- |
| **Role** | Top-level container for the entire viewport. | Functional section of a page (Widget, Form, Table). | Compilation wrapper for Modules/Components. |
| **Lifecycle** | Created per-request. Disposable. | **Build-once**. Structure is immutable. | **Compiled once**. Global/Static reuse. |
| **Dynamic Data** | Added during construction (`addComponents`). | Defined via `SlotKey` placeholders. | Injected via `RenderContext` at render time. |
| **Sizing** | Manages Grid Layout (`Row`/`Column`). | **Do not size directly.** Use container `Column`. | Inherits sizing from the wrapped Module. |
| **HTMX Use** | Target for full body swaps. | Target for partial updates. | Generator for OOB responses. |

### Overlaps and Clashes

1.  **Sizing Clash**:
    *   **Issue**: Users often try to set `.withWidth()` on a `Module`.
    *   **Resolution**: Modules throw `UnsupportedOperationException` for width methods. Always wrap Modules in `Column` or `Div` to control width. Templates preserve this behavior.

2.  **State Clash**:
    *   **Issue**: Trying to change a field on a `Module` (e.g., `module.setTitle("New")`) and re-rendering it won't update the HTML if `build()` has already run.
    *   **Resolution**: Use `Template` and `SlotKey` for any value that needs to change after the initial build.

3.  **Template vs. Component**:
    *   **Issue**: A `Template` is not a `Component` (it doesn't implement the interface directly because `render` requires `RenderContext`).
    *   **Resolution**: To use a Template inside a standard Page build, wrap it in an anonymous Component or use a helper method that calls `template.render(context)`.

## Summary

*   **Modules** are for defining **structure**. They are built once and immutable.
*   **Slots** define where **data** goes.
*   **Templates** combine structure and slots for **performance** and **reuse**.
*   **RenderContext** provides the **data** at runtime.

Use this system for any component that needs to update dynamically via HTMX or renders frequently with different data.
