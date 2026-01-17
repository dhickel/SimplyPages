# Example: Live Dashboard with Auto-Updating Stats

This example shows how to build a live dashboard that automatically updates statistics using Templates and HTMX polling. This is a common pattern for admin dashboards, monitoring tools, and real-time data displays.

## What We're Building

A dashboard with three stat widgets:
- **Active Users**: Shows current user count
- **Revenue**: Shows today's revenue
- **Active Sessions**: Shows current session count

Each widget updates automatically every 3 seconds using HTMX polling, and all three widgets update from a single server request using Out-of-Band (OOB) swaps.

## The Complete Example

### Step 1: Define SlotKeys and Template

First, create a reusable template for a stat widget:

```java
package com.example.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.core.*;
import io.mindspice.jhf.layout.Grid;
import io.mindspice.jhf.layout.Page;
import io.mindspice.jhf.modules.ContentModule;

public class DashboardPage {

    // Define SlotKeys for dynamic values
    public static final SlotKey<String> STAT_TITLE = SlotKey.of("stat_title");
    public static final SlotKey<String> STAT_VALUE = SlotKey.of("stat_value");
    public static final SlotKey<String> STAT_CHANGE = SlotKey.of("stat_change");
    public static final SlotKey<String> MODULE_ID = SlotKey.of("module_id");

    // Pre-compile the stat widget template
    public static final Template STAT_WIDGET_TEMPLATE = Template.of(
        ContentModule.create()
            .withModuleId(Slot.of(MODULE_ID))  // Dynamic module ID for HTMX targeting
            .withCustomContent(
                new Div()
                    .withClass("stat-widget")
                    .withChild(
                        new Header.H3()
                            .withClass("stat-title")
                            .withChild(Slot.of(STAT_TITLE))
                    )
                    .withChild(
                        new Div()
                            .withClass("stat-value")
                            .withChild(Slot.of(STAT_VALUE))
                    )
                    .withChild(
                        new Div()
                            .withClass("stat-change")
                            .withChild(Slot.of(STAT_CHANGE))
                    )
            )
    );
}
```

**Key Points:**
- We define SlotKeys for all dynamic data (title, value, change indicator, module ID)
- The template is a `static final` field - it compiles once and reuses for all renders
- We use `Slot.of(MODULE_ID)` even for the module ID, making it configurable per widget
- Think of SlotKeys as method parameters and RenderContext as the arguments you pass

### Step 2: Create the Page Layout

Build the page with three stat widgets in a grid:

```java
public class DashboardPage {
    // ... (SlotKeys and Template from Step 1) ...

    public String render() {
        return Page.builder()
            .addComponents(Header.H1("Live Dashboard"))
            .addComponents(new Markdown("Stats update automatically every 3 seconds."))

            // Create a 3-column grid with stat widgets
            .addComponents(
                Grid.create()
                    .withColumns(3)
                    .withChild(renderStatWidget(
                        "users-widget",
                        "Active Users",
                        "1,234",
                        "+12% from yesterday"
                    ))
                    .withChild(renderStatWidget(
                        "revenue-widget",
                        "Today's Revenue",
                        "$5,678",
                        "+8% from yesterday"
                    ))
                    .withChild(renderStatWidget(
                        "sessions-widget",
                        "Active Sessions",
                        "89",
                        "+5% from yesterday"
                    ))
            )

            // Add HTMX polling trigger
            .addComponents(
                new Div()
                    .withAttribute("hx-get", "/api/dashboard/stats")
                    .withAttribute("hx-trigger", "every 3s")  // Poll every 3 seconds
                    .withAttribute("hx-swap", "none")         // OOB swaps only - don't replace this div
            )

            .build()
            .render();
    }

    // Helper method to render a stat widget
    private Component renderStatWidget(String id, String title, String value, String change) {
        return new Component() {
            @Override
            public String render(RenderContext context) {
                return STAT_WIDGET_TEMPLATE.render(
                    RenderContext.builder()
                        .with(MODULE_ID, id)
                        .with(STAT_TITLE, title)
                        .with(STAT_VALUE, value)
                        .with(STAT_CHANGE, change)
                        .build()
                );
            }
        };
    }
}
```

**Key Points:**
- We use `Grid.create().withColumns(3)` for a 3-column layout
- Initial render shows static values (1,234 users, $5,678 revenue, etc.)
- HTMX polling trigger is a hidden div that calls `/api/dashboard/stats` every 3 seconds
- `hx-swap="none"` means we don't replace the polling div - updates happen via OOB swaps

### Step 3: Create the Controller Endpoint

The endpoint fetches fresh data and returns updated widgets:

```java
package com.example.controllers;

import org.springframework.web.bind.annotation.*;
import com.example.pages.DashboardPage;
import io.mindspice.jhf.core.RenderContext;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping("/stats")
    @ResponseBody
    public String getStats() {
        // Fetch real data from your database/service
        int activeUsers = getUserCount();        // e.g., 1,245
        String revenue = getRevenue();           // e.g., "$5,890"
        int activeSessions = getSessionCount();  // e.g., 92

        // Calculate change percentages
        String userChange = calculateUserChange();     // e.g., "+13% from yesterday"
        String revenueChange = calculateRevenueChange(); // e.g., "+10% from yesterday"
        String sessionChange = calculateSessionChange(); // e.g., "+6% from yesterday"

        // Render all three widgets with fresh data
        String usersWidget = renderStatWithOOB(
            "users-widget",
            "Active Users",
            String.valueOf(activeUsers),
            userChange
        );

        String revenueWidget = renderStatWithOOB(
            "revenue-widget",
            "Today's Revenue",
            revenue,
            revenueChange
        );

        String sessionsWidget = renderStatWithOOB(
            "sessions-widget",
            "Active Sessions",
            String.valueOf(activeSessions),
            sessionChange
        );

        // Return all three widgets - HTMX will swap each into place
        return usersWidget + revenueWidget + sessionsWidget;
    }

    // Helper method to render a stat widget with OOB attribute
    private String renderStatWithOOB(String id, String title, String value, String change) {
        String html = DashboardPage.STAT_WIDGET_TEMPLATE.render(
            RenderContext.builder()
                .with(DashboardPage.MODULE_ID, id)
                .with(DashboardPage.STAT_TITLE, title)
                .with(DashboardPage.STAT_VALUE, value)
                .with(DashboardPage.STAT_CHANGE, change)
                .build()
        );

        // Add hx-swap-oob="true" to enable Out-of-Band swap
        return html.replace(
            "id=\"" + id + "\"",
            "id=\"" + id + "\" hx-swap-oob=\"true\""
        );
    }

    // Placeholder methods - replace with real data fetching
    private int getUserCount() {
        return (int) (Math.random() * 100) + 1200;  // Random for demo
    }

    private String getRevenue() {
        return "$" + (int) (Math.random() * 1000 + 5000);  // Random for demo
    }

    private int getSessionCount() {
        return (int) (Math.random() * 20) + 80;  // Random for demo
    }

    private String calculateUserChange() {
        return "+" + (int) (Math.random() * 20) + "% from yesterday";
    }

    private String calculateRevenueChange() {
        return "+" + (int) (Math.random() * 15) + "% from yesterday";
    }

    private String calculateSessionChange() {
        return "+" + (int) (Math.random() * 10) + "% from yesterday";
    }
}
```

**Key Points:**
- The endpoint returns **three complete HTML fragments** (one for each widget)
- Each fragment has `hx-swap-oob="true"` attribute added
- HTMX sees the OOB attribute and swaps each widget by matching the `id` attribute
- One request updates all three widgets - efficient!

### Step 4: Add Optional CSS for Styling

```css
.stat-widget {
    padding: 1.5rem;
    background: #f8f9fa;
    border-radius: 8px;
    border: 1px solid #dee2e6;
}

.stat-title {
    color: #6c757d;
    font-size: 0.875rem;
    font-weight: 600;
    text-transform: uppercase;
    margin-bottom: 0.5rem;
}

.stat-value {
    font-size: 2rem;
    font-weight: bold;
    color: #212529;
    margin-bottom: 0.5rem;
}

.stat-change {
    font-size: 0.875rem;
    color: #28a745;  /* Green for positive change */
}
```

## How It Works: The Flow

1. **Initial Page Load**:
   - User visits `/dashboard`
   - Page renders with initial stat values (1,234 users, etc.)
   - HTMX polling div starts its 3-second timer

2. **First Poll (3 seconds later)**:
   - HTMX sends GET request to `/api/dashboard/stats`
   - Controller fetches fresh data (1,245 users, etc.)
   - Controller renders three widgets and adds `hx-swap-oob="true"` to each
   - Server returns: `<div id="users-widget" hx-swap-oob="true">...</div><div id="revenue-widget" hx-swap-oob="true">...</div><div id="sessions-widget" hx-swap-oob="true">...</div>`

3. **HTMX Processes Response**:
   - HTMX sees `hx-swap-oob="true"` on each widget
   - HTMX finds existing elements with matching IDs (`users-widget`, `revenue-widget`, `sessions-widget`)
   - HTMX swaps each widget with the new HTML
   - User sees all three stats update simultaneously

4. **Continuous Polling**:
   - Process repeats every 3 seconds
   - Stats stay fresh without any page reloads

## Key Concepts Explained

### Why Templates?

Without templates, you'd rebuild the entire widget structure on every request:

```java
// ❌ BAD: Rebuilding structure every time (slow, memory-intensive)
public String renderStatWidget(String id, String title, String value, String change) {
    return ContentModule.create()
        .withModuleId(id)
        .withCustomContent(
            new Div()
                .withClass("stat-widget")
                .withChild(new Header.H3().withClass("stat-title").withInnerText(title))
                .withChild(new Div().withClass("stat-value").withInnerText(value))
                .withChild(new Div().withClass("stat-change").withInnerText(change))
        )
        .render();  // Builds entire tree from scratch!
}
```

With templates, the structure is pre-compiled:

```java
// ✅ GOOD: Template structure built once, data injected at render time
return STAT_WIDGET_TEMPLATE.render(
    RenderContext.builder()
        .with(STAT_TITLE, title)
        .with(STAT_VALUE, value)
        .with(STAT_CHANGE, change)
        .build()
);
```

**Performance Impact**: Templates are ~10-50x faster for frequently-rendered components.

### Why Out-of-Band Swaps?

Without OOB swaps, you'd need three separate HTMX requests:

```java
// ❌ INEFFICIENT: Three requests every 3 seconds
<div id="users-widget" hx-get="/api/stats/users" hx-trigger="every 3s"></div>
<div id="revenue-widget" hx-get="/api/stats/revenue" hx-trigger="every 3s"></div>
<div id="sessions-widget" hx-get="/api/stats/sessions" hx-trigger="every 3s"></div>
```

With OOB swaps, one request updates everything:

```java
// ✅ EFFICIENT: Single request updates all three widgets
<div hx-get="/api/dashboard/stats" hx-trigger="every 3s" hx-swap="none"></div>
```

**Benefits**:
- Single database query for all stats
- Single HTTP request
- Atomic updates (all stats refresh together)

## Common Mistakes & Solutions

### Mistake 1: Forgetting hx-swap-oob

**Problem**: Widgets don't update even though the endpoint returns HTML.

**Solution**: Ensure you add `hx-swap-oob="true"` to each widget:

```java
return html.replace(
    "id=\"" + id + "\"",
    "id=\"" + id + "\" hx-swap-oob=\"true\""
);
```

### Mistake 2: ID Mismatch

**Problem**: OOB swap doesn't work because IDs don't match.

**Solution**: Ensure the ID in the initial render matches the ID in the OOB response:

```java
// Initial render
.withModuleId("users-widget")

// OOB response
.with(MODULE_ID, "users-widget")  // Must match exactly!
```

### Mistake 3: Polling Div Gets Replaced

**Problem**: Polling stops after first request because the trigger div disappears.

**Solution**: Use `hx-swap="none"` on the polling div:

```java
new Div()
    .withAttribute("hx-get", "/api/dashboard/stats")
    .withAttribute("hx-trigger", "every 3s")
    .withAttribute("hx-swap", "none")  // Don't replace me!
```

### Mistake 4: Re-Creating Template Every Render

**Problem**: Creating templates in instance methods defeats the performance benefits.

**Solution**: Templates must be `static final` constants:

```java
// ❌ BAD: Creates new template every time
public String render() {
    Template myTemplate = Template.of(...);  // DON'T DO THIS
    return myTemplate.render(context);
}

// ✅ GOOD: Template created once at class load time
public static final Template MY_TEMPLATE = Template.of(...);

public String render() {
    return MY_TEMPLATE.render(context);  // Reuses pre-compiled template
}
```

## Variations & Extensions

### Variation 1: Manual Refresh Button

Add a button to refresh stats on-demand:

```java
.addComponents(
    new Button("Refresh Now")
        .withClass("btn btn-primary")
        .withAttribute("hx-get", "/api/dashboard/stats")
        .withAttribute("hx-swap", "none")
)
```

### Variation 2: Different Poll Intervals

Use different intervals for different data freshness requirements:

```java
// Critical stats: poll every 1 second
.withAttribute("hx-trigger", "every 1s")

// Normal stats: poll every 5 seconds
.withAttribute("hx-trigger", "every 5s")

// Background stats: poll every 30 seconds
.withAttribute("hx-trigger", "every 30s")
```

### Variation 3: Conditional Polling

Only poll when user is on the page (battery-friendly):

```java
// Stop polling when user navigates away
.withAttribute("hx-trigger", "every 3s[document.visibilityState === 'visible']")
```

### Variation 4: Error Handling

Show error state if polling fails:

```java
.withAttribute("hx-on::response-error", "document.getElementById('error-banner').style.display = 'block'")

// Add error banner to page
.addComponents(
    new Alert("Unable to fetch latest stats. Retrying...")
        .withId("error-banner")
        .withClass("alert-danger")
        .withStyle("display: none")  // Hidden by default
)
```

## Real-World Usage

This pattern is perfect for:

- **Admin Dashboards**: User counts, revenue, system health
- **Monitoring Tools**: Server metrics, error rates, queue depths
- **Analytics Dashboards**: Page views, conversions, active users
- **IoT Dashboards**: Sensor readings, device status
- **Trading Dashboards**: Stock prices, portfolio value

## Next Steps

- Read [Pattern: Template-Based OOB Swaps](../getting-started/09-htmx-dynamic-features.md#pattern-template-based-oob-swaps) for more OOB patterns
- Read [Templates and Dynamic Updates](../getting-started/13-templates-and-dynamic-updates.md) for Template deep dive
- See [Wiki-Style Editing Example](wiki-style-editing.md) for form-based Templates
- Check [Quick Reference: Template System](../quick-reference/template-system.md) for SlotKey cheat sheet

## Complete Code Reference

You can find the complete working example in the demo application:
- Page: `src/main/java/io/mindspice/demo/pages/DynamicUpdatesPage.java`
- Controller: `src/main/java/io/mindspice/demo/DemoController.java`
- Live demo: Visit `/demo/dynamic-updates` when running the application
