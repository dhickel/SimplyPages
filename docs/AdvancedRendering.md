# Advanced Rendering Patterns

This document formalizes the architectural patterns for handling advanced rendering scenarios in the Java HTML Framework (JHF). The recent introduction of the high-performance Template/SlotKey system for dynamic data has created the need for clear guidance on how it coexists with the original, more flexible rendering model.

This document defines two primary, officially supported rendering patterns and introduces a new helper component (TemplateComponent) to ensure seamless integration between them. The goal is to provide developers with a clear "right tool for the right job," enabling them to build everything from simple static pages to complex, structurally dynamic, and user-customizable applications without sacrificing performance or clarity.

## The Challenge: "Dynamic Data" vs. "Dynamic Structure"

Our framework must efficiently handle two distinct types of "dynamic" content:

1. **Dynamic Data**: Where the page layout is fixed, but the data within it changes on each request (e.g., a user's name in a header, a list of items). The Template/SlotKey system was designed to solve this with high performance.
2. **Dynamic Structure**: Where the page's composition of rows, columns, and modules is conditional and determined at runtime (e.g., a user-customizable dashboard, or a page that shows a different layout for "Admin" vs. "Standard" users).

A single rendering model cannot solve both of these challenges optimally. This document codifies a dual-pattern approach.

## The Official Rendering Patterns

### Pattern A: Request-Scoped Composition (For Dynamic Structure)

This is the original, intuitive pattern for building pages. It remains the recommended solution for any page where the layout is conditional.

* **Use Case**:
    * User-customizable dashboards or portals (e.g., a "grow journal").
    * Pages where the layout of rows, columns, or modules changes based on user role or other business logic.
    * Simple pages where the performance overhead of pre-compiled templates is unnecessary.
* **Implementation**:
    * On each request, the component tree is built from scratch in memory.
    * Conditional logic (if/else) is used directly in the builder/page class to construct the appropriate layout.

```java
// Example: Building a different layout based on user role
public String buildDashboardPage(User user) {
    Page page = Page.create();
    if (user.isPro()) {
        // Build a complex, multi-column layout for Pro users
        page.addRow(row -> row
            .withChild(Column.create().withWidth(8).withChild(new AdvancedChartModule()))
            .withChild(Column.create().withWidth(4).withChild(new QuickActionsModule()))
        );
    } else {
        // Build a simple, single-column layout for Standard users
        page.addRow(row -> row
            .withChild(Column.create().withWidth(12).withChild(new BasicStatsModule()))
        );
    }
    return page.render();
}
```

* **Trade-offs**: Provides maximum flexibility and clarity at the cost of performance, as the component tree is rebuilt on every request.

### Pattern B: Pre-compiled Templates (For Dynamic Data)

This is the high-performance pattern introduced in the recent refactor.

* **Use Case**:
    * Pages with a fixed structure that are rendered frequently with different data.
    * Individual components/modules that are the target of frequent HTMX updates (polling, dynamic search results, etc.).
    * Implementing HTMX Out-of-Band (OOB) swaps where one request updates multiple page sections.
* **Implementation**:
    * A Module or Component's structure is defined once.
    * Dynamic areas are marked with `Slot.of(SLOT_KEY)`.
    * A Template is created from the component (`Template.of(myModule)`), which can be a static final constant.
    * At render time, a lightweight `RenderContext` provides the data: `TEMPLATE.render(context)`.
* **Trade-offs**: Provides maximum performance and type safety at the cost of structural flexibility.

## Advanced Use Case: The Composite Pattern

For the most complex scenarios, like a customizable "grow journal" where individual modules also need to be updated dynamically via HTMX, we will combine both patterns.

* **Implementation**:
    1. **Page Layout**: Use Pattern A (Request-Scoped Composition) to build the overall page structure. Logic will fetch the user's layout (e.g., ["Note-1", "Env-2"]) and iterate through it.
    2. **Module Rendering**: For each module in the layout, render its pre-defined Pattern B (`Template`). Each module must have its own Template (e.g., `NoteModule.TEMPLATE`, `EnvModule.TEMPLATE`). The rendered HTML for each module must include a unique DOM ID for HTMX targeting.
* **Result**: This composite approach provides structural flexibility at the page level while retaining high-performance, template-driven rendering for individual module updates via HTMX.

## Feature: In-Place & Wiki-Style Editing

The framework fully supports in-place editing by using HTMX to swap between different template "views" of a single module.

* **Concept**: A module (e.g., `NoteModule`) will have multiple associated templates: `DISPLAY_TEMPLATE`, `EDIT_TEMPLATE`, and potentially `PENDING_REVIEW_TEMPLATE`.
* **The Flow**:
    1. **Initial View**: The page loads, rendering `DISPLAY_TEMPLATE`, which includes an "Edit" button.
    2. **Click Edit**: The button issues an `hx-get` to an endpoint that renders and returns the `EDIT_TEMPLATE` (containing a form). `hx-swap="outerHTML"` replaces the display view with the edit form.
    3. **Click Save/Propose**: The form `hx-post`s to an update endpoint. The controller saves the data and responds with the appropriate next view:
        * For a simple edit, it returns the `DISPLAY_TEMPLATE` with the new content.
        * For a wiki edit, it returns the `PENDING_REVIEW_TEMPLATE`.
    4. The response HTML replaces the form, completing the cycle without a page load.

## Helper Component: TemplateComponent

To make the "Composite Pattern" more ergonomic, a new helper component is provided: `io.mindspice.jhf.core.TemplateComponent`.

* **The Problem**: Integrating a rendered Template into a Page composition was clunky because a Template is not a Component.
* **The Solution**: A simple adapter class that wraps a Template and RenderContext, making them usable as a standard Component.

```java
// Usage Example:
TemplateComponent.of(NoteModule.TEMPLATE, noteContext);
```

* **Benefit**: This allows for much cleaner composition: `page.addComponents(TemplateComponent.of(NoteModule.TEMPLATE, noteContext));`

## Architectural Decision: Rejection of Stateful Session UI

The idea of storing page layouts or component trees in the HTTP Session ("SessionPage") was considered and explicitly rejected. This approach leads to severe memory, scalability, and data-synchronization problems that are contrary to modern, stateless web architecture. The framework will not support or recommend this pattern.
