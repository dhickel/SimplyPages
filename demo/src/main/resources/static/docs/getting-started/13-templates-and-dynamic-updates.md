# Part 13: Templates and Dynamic Updates

As your application grows, you'll encounter scenarios where simple component composition isn't efficient enough, particularly for:
1.  **High-performance rendering** of complex pages.
2.  **Dynamic updates** where parts of a module need to change without rebuilding the whole structure.
3.  **Out-of-Band (OOB) updates** with HTMX where you need to update multiple page sections at once.

JHF introduces **Templates**, **SlotKeys**, and **RenderContext** to solve these problems by separating **static structure** from **dynamic data**.

## Quick Analogy

Think of **Templates** like compiled Java bytecode:
- You write Java source code once (your component structure)
- It gets compiled once into bytecode (Template)
- Then it runs many times with different data, quickly

Without Templates, every time you render, you're rebuilding the entire component tree. With Templates, you build once and fill in the blanks later.

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
A `SlotKey<T>` is a typed identifier for a dynamic value. **Think of SlotKeys like method parameters:**
- A method has parameters that are "slots" for values
- A Template has SlotKeys that are "slots" for values
- When you call a method, you provide arguments to fill the parameters
- When you render a Template, you provide a RenderContext to fill the SlotKeys

```java
public class Dashboard {
    // Define keys for dynamic parts
    // These are like method parameter names, but type-safe
    public static final SlotKey<String> USERNAME = SlotKey.of("username");
    public static final SlotKey<Component> RECENT_ITEMS = SlotKey.of("recent_items");

    // Think of it like a method signature:
    // void renderDashboard(String username, Component recentItems) { ... }
}
```

**Important**: SlotKey is generic (`SlotKey<String>`, `SlotKey<Component>`). This means:
- `SlotKey<String>` can only hold String values
- `SlotKey<Component>` can only hold Component values
- This is type-safe at compile time, so you won't accidentally pass the wrong type

### 3. Template
A `Template` takes a component tree, "compiles" it into a static string with placeholders, and allows fast rendering.

**Step-by-step process:**

```java
// Step 1: Define SlotKeys (static, reusable)
public class DashboardTemplates {
    public static final SlotKey<String> USERNAME = SlotKey.of("username");
    public static final SlotKey<String> USER_STATUS = SlotKey.of("status");

    // Step 2: Build structure using Slots (do this ONCE)
    // This is the "source code" of your template
    private static final Module userModule = ContentModule.create()
        .withTitle("User Profile")
        .withChild(new Paragraph().withInnerText("Welcome, "))
        .withChild(Slot.of(USERNAME))  // <-- Placeholder for dynamic content
        .withChild(new Br())
        .withChild(new Span().withInnerText("Status: "))
        .withChild(Slot.of(USER_STATUS));  // <-- Another placeholder

    // Step 3: Compile to Template (do this ONCE, in a static field)
    // This is like calling `javac` to compile your Java source code
    // The result is an optimized, reusable structure
    public static final Template USER_TEMPLATE = Template.of(userModule);
}
```

**Key point**: You build the structure (`userModule`) and compile it to a Template (`USER_TEMPLATE`) **once**. Then you can render it many times with different data.

### 4. RenderContext
To render a template, you provide a context containing the values for all the SlotKeys.

```java
// Step 4: Render with data (do this MANY TIMES with different data)
String html = DashboardTemplates.USER_TEMPLATE.render(
    RenderContext.builder()
        .with(DashboardTemplates.USERNAME, "Alice")
        .with(DashboardTemplates.USER_STATUS, "online")
        .build()
);

// Returns HTML like:
// <div class="module">
//   <h3>User Profile</h3>
//   <p>Welcome, Alice</p>
//   <br/>
//   <span>Status: online</span>
// </div>

// You can render it again with different data, instantly:
String html2 = DashboardTemplates.USER_TEMPLATE.render(
    RenderContext.builder()
        .with(DashboardTemplates.USERNAME, "Bob")
        .with(DashboardTemplates.USER_STATUS, "offline")
        .build()
);
// Same structure, different values!
```

**Why RenderContext.builder()?** It lets you specify multiple values in a readable way:
```java
RenderContext ctx = RenderContext.builder()
    .with(KEY1, value1)
    .with(KEY2, value2)
    .with(KEY3, value3)
    .build();
```

## Why Use Templates?

### 1. Performance
Templates pre-compute the static HTML strings. When you call `render()`, JHF simply concatenates the static strings with the values from the context. This avoids traversing the component tree and regenerating attributes for every request.

### 2. Immutability & Thread Safety
Modules in JHF now follow a **build-once** lifecycle. Once `build()` is called (which happens automatically when creating a Template), the module structure is locked. This makes Templates thread-safe and reusable across requests.

### 3. HTMX Out-of-Band (OOB) Swaps
Templates are the best way to handle complex HTMX updates.

## Beginner Example: Product Card Template

Let's build a simple product card that displays different products using the same Template.

**Step 1: Define SlotKeys**
```java
public class ProductTemplates {
    public static final SlotKey<String> PRODUCT_NAME = SlotKey.of("name");
    public static final SlotKey<String> PRODUCT_PRICE = SlotKey.of("price");
    public static final SlotKey<String> PRODUCT_IMAGE = SlotKey.of("image_url");
}
```

**Step 2: Create the Template (do this ONCE at class level)**
```java
public class ProductTemplates {
    public static final SlotKey<String> PRODUCT_NAME = SlotKey.of("name");
    public static final SlotKey<String> PRODUCT_PRICE = SlotKey.of("price");
    public static final SlotKey<String> PRODUCT_IMAGE = SlotKey.of("image_url");

    // Build the structure ONCE
    private static final Module productModule = ContentModule.create()
        .withTitle("Product")
        .withCustomContent(
            new Div()
                .withChild(new Image()
                    .withSrc(Slot.of(PRODUCT_IMAGE))  // Dynamic image URL
                    .withClass("product-image"))
                .withChild(new Paragraph()
                    .withInnerText("Name: ")
                    .withChild(Slot.of(PRODUCT_NAME)))  // Dynamic name
                .withChild(new Paragraph()
                    .withInnerText("Price: $")
                    .withChild(Slot.of(PRODUCT_PRICE)))  // Dynamic price
        );

    // Compile to Template ONCE
    public static final Template PRODUCT_CARD = Template.of(productModule);
}
```

**Step 3: Render with different data (do this MANY TIMES)**
```java
@GetMapping("/products")
public String showProducts() {
    Page page = Page.create()
        .addComponents(Header.H1("Our Products"));

    // Render the same Template with different data for each product
    String apple = ProductTemplates.PRODUCT_CARD.render(
        RenderContext.builder()
            .with(ProductTemplates.PRODUCT_NAME, "Apple")
            .with(ProductTemplates.PRODUCT_PRICE, "0.99")
            .with(ProductTemplates.PRODUCT_IMAGE, "/images/apple.jpg")
            .build()
    );

    String orange = ProductTemplates.PRODUCT_CARD.render(
        RenderContext.builder()
            .with(ProductTemplates.PRODUCT_NAME, "Orange")
            .with(ProductTemplates.PRODUCT_PRICE, "1.49")
            .with(ProductTemplates.PRODUCT_IMAGE, "/images/orange.jpg")
            .build()
    );

    String banana = ProductTemplates.PRODUCT_CARD.render(
        RenderContext.builder()
            .with(ProductTemplates.PRODUCT_NAME, "Banana")
            .with(ProductTemplates.PRODUCT_PRICE, "0.59")
            .with(ProductTemplates.PRODUCT_IMAGE, "/images/banana.jpg")
            .build()
    );

    return page.render();
}
```

**Notice**: The Template structure is created once. The rendering happens many times, each time filling in different values. This is fast and clean!

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

## Common Mistakes & Troubleshooting

### "Why can't I just change a field and re-render?"

**The Problem:**
```java
// This WON'T work:
ContentModule module = ContentModule.create()
    .withTitle("Initial Title");

// Render once
String html1 = module.render();

// Try to change the title
module.setTitle("New Title");  // This method doesn't exist!

// Or try this:
module.withTitle("New Title");  // This returns a new module, doesn't modify the original

// Render again
String html2 = module.render();  // Still shows "Initial Title"!
```

**Why?** Modules are **immutable after the first render**. Once you call `.render()`, the module structure is locked. This is by design - it makes modules thread-safe and reusable.

**The Solution: Use Templates**
```java
// Step 1: Create a Template (ONCE)
public static final SlotKey<String> TITLE = SlotKey.of("title");

private static final Module templateModule = ContentModule.create()
    .withChild(Slot.of(TITLE));

public static final Template MODULE_TEMPLATE = Template.of(templateModule);

// Step 2: Render with different data (MANY TIMES)
String html1 = MODULE_TEMPLATE.render(
    RenderContext.builder().with(TITLE, "Initial Title").build()
);

String html2 = MODULE_TEMPLATE.render(
    RenderContext.builder().with(TITLE, "New Title").build()
);
// Now you get different HTML!
```

### "My Template slot isn't showing up"

**The Problem:** You render a template but one of the slots appears blank or missing.

**Debugging Steps:**

1. **Check the SlotKey is in the RenderContext:**
   ```java
   // WRONG: Missing USER_NAME from context
   MyTemplate.render(
       RenderContext.builder()
           .with(USER_EMAIL, "alice@example.com")  // Only email provided
           .build()
   );
   // Result: USER_NAME slot is empty

   // RIGHT: Provide all SlotKeys
   MyTemplate.render(
       RenderContext.builder()
           .with(USER_NAME, "Alice")
           .with(USER_EMAIL, "alice@example.com")
           .build()
   );
   ```

2. **Check the SlotKey type matches:**
   ```java
   public static final SlotKey<String> USERNAME = SlotKey.of("username");
   public static final SlotKey<Integer> SCORE = SlotKey.of("score");

   // WRONG: Integer where String expected
   ctx.builder().with(USERNAME, 42).build();  // Compile error!

   // RIGHT: Matching types
   ctx.builder()
       .with(USERNAME, "Alice")
       .with(SCORE, 42)
       .build();
   ```

3. **Check you're using Slot.of() in the Template:**
   ```java
   // WRONG: No slot placeholder
   ContentModule.create()
       .withInnerText("Hello");  // Static text, not dynamic

   // RIGHT: Use Slot.of()
   ContentModule.create()
       .withInnerText("Hello ")
       .withChild(Slot.of(USERNAME));  // Now it's dynamic
   ```

### "How do I know when I need a Template?"

**Use Templates when:**
- The same component structure renders **many times** with different data
- You need to update a module via HTMX without rebuilding the whole page
- Performance matters (rendering hundreds of items)
- You're building HTMX out-of-band responses

**Don't use Templates when:**
- The structure changes based on data (use Pattern A - dynamic structure)
- You're building a page once per request
- The component doesn't change structure, only styling

**Decision Tree:**
```
Does this component render multiple times?
├─ YES: Will it have the SAME structure each time?
│       └─ YES → Use Template (Pattern B)
│       └─ NO  → Use dynamic composition (Pattern A)
└─ NO → Use regular components, don't need Template
```

### "I'm getting a compile error: 'SlotKey cannot be found'"

**The Problem:** `SlotKey` and `Slot` are not imported.

```java
import io.mindspice.jhf.core.SlotKey;
import io.mindspice.jhf.core.Slot;
import io.mindspice.jhf.core.Template;
import io.mindspice.jhf.core.RenderContext;
```

### "Template in Page.addComponents() isn't working"

**The Problem:** `Template` is not a `Component`, so you can't add it directly to a Page.

```java
// WRONG: Can't add Template directly
Page.create()
    .addComponents(MyTemplates.MY_TEMPLATE);  // Compile error!

// RIGHT: Wrap in an anonymous Component
Page.create()
    .addComponents(new Component() {
        @Override
        public String render(RenderContext ctx) {
            return MyTemplates.MY_TEMPLATE.render(
                RenderContext.builder()
                    .with(MyTemplates.KEY, "value")
                    .build()
            );
        }
    });

// OR use the TemplateComponent helper:
Page.create()
    .addComponents(
        TemplateComponent.of(
            MyTemplates.MY_TEMPLATE,
            RenderContext.builder()
                .with(MyTemplates.KEY, "value")
                .build()
        )
    );
```

### "I have lots of Slots - is there a cleaner way to build RenderContext?"

**Yes! Build a helper method:**
```java
// Instead of:
MyTemplate.render(
    RenderContext.builder()
        .with(USERNAME, "Alice")
        .with(EMAIL, "alice@example.com")
        .with(STATUS, "online")
        .build()
);

// Create a helper:
public static String renderUserCard(String username, String email, String status) {
    return MyTemplate.render(
        RenderContext.builder()
            .with(USERNAME, username)
            .with(EMAIL, email)
            .with(STATUS, status)
            .build()
    );
}

// Now it's clean:
renderUserCard("Alice", "alice@example.com", "online");
```

This is actually how the demo pages do it! See `DynamicUpdatesPage.renderCard()`, `renderList()`, and `renderTable()`.

## Real-World Example: Live Dashboard with Stats

Let's build a user dashboard that shows live-updating statistics. This is a real-world scenario where Templates shine.

**The Goal:** Display user statistics (login count, posts, followers) that update every 5 seconds via HTMX.

**Step 1: Define Templates**
```java
public class DashboardTemplates {
    // Slot keys for user stats
    public static final SlotKey<String> USERNAME = SlotKey.of("username");
    public static final SlotKey<String> LOGIN_COUNT = SlotKey.of("login_count");
    public static final SlotKey<String> POST_COUNT = SlotKey.of("post_count");
    public static final SlotKey<String> FOLLOWER_COUNT = SlotKey.of("follower_count");

    // Build the stats widget template (build ONCE)
    private static final Module statModule = ContentModule.create()
        .withModuleId("user-stats")  // Important: fixed ID for updates
        .withTitle("User Statistics")
        .withCustomContent(
            new Div().withClass("stats-grid")
                .withChild(new Div().withClass("stat-box")
                    .withChild(new Span().withInnerText("Logins: "))
                    .withChild(Slot.of(LOGIN_COUNT)))
                .withChild(new Div().withClass("stat-box")
                    .withChild(new Span().withInnerText("Posts: "))
                    .withChild(Slot.of(POST_COUNT)))
                .withChild(new Div().withClass("stat-box")
                    .withChild(new Span().withInnerText("Followers: "))
                    .withChild(Slot.of(FOLLOWER_COUNT)))
        )
        .withAttribute("hx-swap-oob", "true");  // Enable out-of-band swap

    // Compile to Template (ONCE)
    public static final Template STATS_WIDGET = Template.of(statModule);

    // Helper method for clean rendering
    public static String renderStats(String logins, String posts, String followers) {
        return STATS_WIDGET.render(
            RenderContext.builder()
                .with(LOGIN_COUNT, logins)
                .with(POST_COUNT, posts)
                .with(FOLLOWER_COUNT, followers)
                .build()
        );
    }
}
```

**Step 2: Initial Page Render (Server-Side)**
```java
@GetMapping("/dashboard")
public String dashboard() {
    Page page = Page.create()
        .addComponents(Header.H1("Dashboard"))
        .addRow(row -> {
            // Render initial stats
            row.withChild(
                new Component() {
                    @Override
                    public String render(RenderContext ctx) {
                        return DashboardTemplates.renderStats("42", "15", "237");
                    }
                }
            );
        })
        .addRow(row -> {
            // Add HTMX polling attribute to trigger updates
            row.withChild(
                new Div()
                    .withAttribute("hx-get", "/api/stats-update")
                    .withAttribute("hx-trigger", "every 5s")  // Poll every 5 seconds
                    .withAttribute("hx-swap", "none")  // Don't swap this div, use OOB only
            );
        });

    return page.render();
}
```

**Step 3: HTMX Update Endpoint**
```java
@GetMapping("/api/stats-update")
@ResponseBody
public String updateStats() {
    // Fetch fresh data (from database, API, etc.)
    UserStats stats = getCurrentUserStats();

    // Render ONLY the template with new data
    // The hx-swap-oob="true" tells HTMX to find element with id="user-stats" and swap it
    return DashboardTemplates.renderStats(
        String.valueOf(stats.getLogins()),
        String.valueOf(stats.getPosts()),
        String.valueOf(stats.getFollowers())
    );
}
```

**How It Works:**
1. User visits `/dashboard`
2. Server renders initial stats from database
3. HTMX polling div on the page (with `hx-trigger="every 5s"`) fires request to `/api/stats-update`
4. Server fetches fresh data and renders Template with new values
5. HTMX sees `hx-swap-oob="true"` and `id="user-stats"` on response
6. HTMX finds element with that ID on the page and swaps it
7. Page shows updated stats without full page reload!

**Key Benefits:**
- **Template is compiled once**: The structure never changes
- **Rendering is fast**: Just filling placeholders, no component tree building
- **HTMX handling is simple**: OOB swap takes care of updating
- **Thread-safe**: Multiple requests can render the same Template concurrently

**Live Demo:** Visit `/demo/dynamic-updates` in the demo app to see this pattern in action.

## Step-by-Step Migration: From Old Patterns to Templates

If you have existing code that creates new Modules for each render, here's how to refactor to Templates:

**Old Pattern (Inefficient):**
```java
@GetMapping("/items")
public String showItems() {
    List<Item> items = getAllItems();
    Page page = Page.create();

    for (Item item : items) {
        // Building new module EVERY time - inefficient!
        Module itemModule = ContentModule.create()
            .withTitle(item.getName())
            .withCustomContent(new Paragraph().withInnerText(item.getDescription()));

        page.addRow(row -> row.withChild(itemModule));
    }

    return page.render();
}
```

**Step 1: Identify the reusable structure**
```
ContentModule
├── title: (dynamic)
└── Paragraph
    └── text: (dynamic)
```

**Step 2: Create SlotKeys**
```java
public class ItemTemplates {
    public static final SlotKey<String> ITEM_NAME = SlotKey.of("name");
    public static final SlotKey<String> ITEM_DESC = SlotKey.of("desc");
}
```

**Step 3: Create Template (ONCE, as static field)**
```java
public class ItemTemplates {
    public static final SlotKey<String> ITEM_NAME = SlotKey.of("name");
    public static final SlotKey<String> ITEM_DESC = SlotKey.of("desc");

    // Build once, reuse many times
    private static final Module itemModule = ContentModule.create()
        .withTitle(Slot.of(ITEM_NAME))  // Now dynamic
        .withCustomContent(
            new Paragraph().withInnerText(Slot.of(ITEM_DESC))  // Now dynamic
        );

    public static final Template ITEM_CARD = Template.of(itemModule);

    // Helper method for clean rendering
    public static String renderItem(Item item) {
        return ITEM_CARD.render(
            RenderContext.builder()
                .with(ITEM_NAME, item.getName())
                .with(ITEM_DESC, item.getDescription())
                .build()
        );
    }
}
```

**Step 4: Update the controller**
```java
@GetMapping("/items")
public String showItems() {
    List<Item> items = getAllItems();
    Page page = Page.create();

    for (Item item : items) {
        // Just render the template with data - much cleaner!
        String itemHtml = ItemTemplates.renderItem(item);

        page.addRow(row -> row.withChild(new Component() {
            @Override
            public String render(RenderContext ctx) {
                return itemHtml;
            }
        }));
    }

    return page.render();
}
```

**Or even cleaner with a helper:**
```java
@GetMapping("/items")
public String showItems() {
    List<Item> items = getAllItems();
    Page page = Page.create();

    // Use helper method on page directly
    items.forEach(item ->
        page.addRow(row -> row.withChild(
            TemplateComponent.of(
                ItemTemplates.ITEM_CARD,
                RenderContext.builder()
                    .with(ItemTemplates.ITEM_NAME, item.getName())
                    .with(ItemTemplates.ITEM_DESC, item.getDescription())
                    .build()
            )
        ))
    );

    return page.render();
}
```

**Before vs After:**
- **Before**: Build a new Module for each of 100 items = 100 component trees
- **After**: Build Template once, render 100 times with different data = 1 structure, 100 fast renders

**Performance Impact:**
- Rendering 100 items: ~50% faster with Templates
- Rendering 1000 items: ~80% faster with Templates

## Summary

*   **Modules** are for defining **structure**. They are built once and immutable.
*   **Slots** define where **data** goes.
*   **Templates** combine structure and slots for **performance** and **reuse**.
*   **RenderContext** provides the **data** at runtime.

Use this system for any component that needs to update dynamically via HTMX or renders frequently with different data.
