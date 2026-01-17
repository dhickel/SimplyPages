# State Management Best Practices

Managing state correctly is critical for building scalable and thread-safe applications with SimplyPages.

## The Golden Rule: Stateless Modules, Request-Scoped Pages

**Modules must be designed as stateless configuration containers.**
**Pages and Components must be request-scoped.**

### Why?

SimplyPages runs in a multi-threaded environment (e.g., Spring Boot web server). If you share a module instance across requests (e.g., by storing it in a static field or a singleton service), one user's data will bleed into another user's view, or worse, cause concurrent modification exceptions.

### Correct Pattern

Create new instances of pages and modules for every request.

```java
@Controller
public class MyController {

    @GetMapping("/dashboard")
    public String dashboard() {
        // ALWAYS create a new Page instance
        DashboardPage page = new DashboardPage();
        return page.render();
    }
}
```

## Immutable Configuration

Modules typically use a "Build-Once" pattern.
- Configuration methods (`withTitle`, `withContent`) return `this` for chaining.
- `buildContent()` is called exactly once, internally, when the module is first rendered.
- Subsequent calls to render return the cached structure.

This means you cannot modify a module's structure after it has been rendered. If you need to change it (e.g., for an HTMX update), you must create a new instance.

### Example: HTMX Update

```java
@PostMapping("/update-chart")
public String updateChart() {
    // Create a FRESH instance with new data
    ChartModule chart = ChartModule.create()
        .withData(newData);

    // Return the new HTML
    return chart.render();
}
```

## Context Propagation

Dynamic data that isn't part of the static structure should be passed via `RenderContext`.

```java
RenderContext context = new RenderContext();
context.put("user", currentUser);

page.render(context);
```

Components implement `render(RenderContext)` to propagate this context down the tree.

## Do Not Cache Components

Fully constructed `Component` instances (like `Div`, `Span`, `Button` constructed inside a Module) are stateful. They contain their list of children, attributes, etc.

**Never cache Component instances.** Cache the underlying data models instead, and rebuild the component tree for each response.

## Thread Safety Analysis

### Shared Storage
If you need to share state between users (e.g., a chat room or live dashboard), use thread-safe data structures in your services, not in your view components.

- **Good:** `ConcurrentHashMap` in a `@Service`.
- **Good:** Database with `@Transactional`.
- **Bad:** `static List<String> messages` in a `Page` class.

### Controller Fields
**Never store module instances in Controller fields.**

```java
@Controller
public class BadController {
    // ❌ DANGER: This is shared by ALL users!
    private ContentModule sharedModule = ContentModule.create();

    @GetMapping("/")
    public String index() {
        return sharedModule.render(); // Users will see each other's edits!
    }
}
```

## Summary

1.  **New Request = New Page Instance.**
2.  **New Request = New Module Instances.**
3.  **Use `RenderContext` for dynamic request data.**
4.  **Store shared state in Services/DB, not in Components.**
