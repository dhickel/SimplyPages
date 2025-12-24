# Advanced Rendering Patterns

> **Note**: This document has been reorganized into the `advanced/` directory as part of the documentation structure improvements. It covers advanced architectural patterns for developers building complex applications with JHF.

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
    * At render time, a lightweight RenderContext provides the data: `TEMPLATE.render(context)`.
* **Trade-offs**: Provides maximum performance and type safety at the cost of structural flexibility.

## Advanced Use Case: The Composite Pattern

For the most complex scenarios, like a customizable "grow journal" where individual modules also need to be updated dynamically via HTMX, we will combine both patterns.

* **Implementation**:
    1. **Page Layout**: Use Pattern A (Request-Scoped Composition) to build the overall page structure. Logic will fetch the user's layout (e.g., `["Note-1", "Env-2"]`) and iterate through it.
    2. **Module Rendering**: For each module in the layout, render its pre-defined Pattern B (`Template`). Each module must have its own Template (e.g., `NoteModule.TEMPLATE`, `EnvModule.TEMPLATE`). The rendered HTML for each module must include a unique DOM ID for HTMX targeting.
* **Result**: This composite approach provides structural flexibility at the page level while retaining high-performance, template-driven rendering for individual module updates via HTMX.

## Feature: In-Place & Wiki-Style Editing

The framework fully supports in-place editing by using HTMX to swap between different template "views" of a single module.

* **Concept**: A module (e.g., `NoteModule`) will have multiple associated templates: `DISPLAY_TEMPLATE`, `EDIT_TEMPLATE`, and potentially `PENDING_REVIEW_TEMPLATE`.
* **The Flow**:
    1. **Initial View**: The page loads, rendering `DISPLAY_TEMPLATE`, which includes an "Edit" button.
    2. **Click Edit**: The button issues an `hx-get` to an endpoint that renders and returns the `EDIT_TEMPLATE` (containing a form). `hx-swap="outerHTML"` replaces the display view with the edit form.
    3. **Click Save/Propose**: The form `hx-posts` to an update endpoint. The controller saves the data and responds with the appropriate next view:
        * For a simple edit, it returns the `DISPLAY_TEMPLATE` with the new content.
        * For a wiki edit, it returns the `PENDING_REVIEW_TEMPLATE`.
    4. The response HTML replaces the form, completing the cycle without a page load.

## New Framework Component: TemplateComponent

To make the "Composite Pattern" more ergonomic, a new helper component is available: `TemplateComponent`.

* **The Problem**: Integrating a rendered Template into a Page composition was clunky because a Template did not implement Component.
* **The Solution**: A simple adapter class that wraps a Template and RenderContext, making them usable as a standard Component.
* **Implementation**: `io.mindspice.jhf.core.TemplateComponent`

```java
// Usage:
page.addComponents(TemplateComponent.of(NoteModule.TEMPLATE, noteContext));
```

## Pattern C: HTMX Content Navigation (Documentation Viewer Pattern)

This pattern demonstrates how to build navigation systems where clicking links updates only a specific content area without full page reloads, combining server-side rendering with HTMX for seamless user experience.

* **Use Case**:
    * Documentation viewers with sidebar navigation
    * Tabbed content interfaces
    * Multi-section pages where navigation updates only the main content area
    * Any interface where the shell/chrome remains constant but content changes

* **Implementation**:
    1. **Page Structure**: Create a page with a fixed container that has a unique ID for the content area:
    ```java
    public String render() {
        return Page.builder()
                .withStickySidebar(sidebar, 9, 3)
                .addComponents(
                    new Div()
                        .withAttribute("id", "docs-content")  // Target for HTMX updates
                        .withChild(Header.H1(title))
                        .withChild(ContentModule.create()
                            .withContent(markdownContent))
                )
                .build()
                .render();
    }
    ```

    2. **Sidebar Links with HTMX**: Add HTMX attributes to navigation links to target the content area:
    ```java
    new Link("/docs/" + filePath, title)
        .withClass("text-decoration-none text-dark")
        .withHxGet("/docs/" + filePath)       // Load content via HTMX
        .withHxTarget("#docs-content")         // Target the content div
        .withHxSwap("innerHTML scroll:top")    // Replace inner HTML and scroll to top
        .withHxPushUrl(true)                   // Update browser URL
    ```

    3. **Controller Endpoint**: Detect HTMX requests and return partial content:
    ```java
    @GetMapping(value = {"/docs/**", "/docs"})
    @ResponseBody
    public String docs(
            HttpServletResponse response,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        // Load and process content...
        DocsPage docsPage = new DocsPage(title, markdown, sidebar);

        // Set Vary header for proper caching
        response.setHeader("Vary", "HX-Request");

        // Return only content for HTMX requests, full page for direct navigation
        if (hxRequest != null) {
            return docsPage.renderContent();  // Partial HTML
        }

        return renderWithShellIfNeeded(null, docsPage, response);  // Full page
    }
    ```

    4. **Separate Content Rendering Method**: Create a method to render just the content portion:
    ```java
    public String renderContent() {
        return new Div()
            .withAttribute("id", "docs-content")
            .withChild(Header.H1(title))
            .withChild(ContentModule.create()
                .withContent(markdownContent))
            .render();
    }
    ```

* **Key Benefits**:
    * **No Page Reloads**: Navigation feels instant - only content area updates
    * **Browser History**: `hx-push-url="true"` maintains proper browser back/forward navigation
    * **SEO Friendly**: Direct navigation (no HX-Request header) returns full page with all content
    * **Progressive Enhancement**: Falls back to standard links if JavaScript disabled

* **Implementation Example**:
    The framework's documentation viewer demonstrates this pattern in action:
    * Visit `/docs/getting-started/01-introduction` directly → full page loads
    * Click sidebar links → only content area updates via HTMX
    * Browser back/forward buttons work correctly
    * URL updates to reflect current page

* **Trade-offs**:
    * Requires controller logic to detect HTMX requests and return appropriate response
    * Must maintain both full-page and partial rendering methods
    * Provides excellent user experience while maintaining server-side rendering benefits

## Architectural Decision: Rejection of Stateful Session UI

The idea of storing page layouts or component trees in the HTTP Session ("SessionPage") was considered and explicitly rejected. This approach leads to severe memory, scalability, and data-synchronization problems that are contrary to modern, stateless web architecture. The framework will not support or recommend this pattern.
